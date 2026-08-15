package com.example.demo.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.ProductImage;
import com.example.demo.repository.ProductImageRepository;

import net.coobird.thumbnailator.Thumbnails;

@Service
public class ImageUploadService {

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductImageService productImageService;

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();
    private static final long MAX_TOTAL_UPLOAD_BYTES = 25L * 1024 * 1024;

    // Hard ceiling on the longest edge we ever decode the source at, regardless of
    // its longest requested variant. Protects against a small compressed file size
    // hiding a very large pixel count (e.g. a high-megapixel, high-compression photo).
    private static final int MAX_SOURCE_DIMENSION = 2560;

    /**
     * One output size to generate.
     * label   - the suffix used in the output filename ("large", "medium", "compact",
     *           "thumbnail", or null for a single custom/specific-size request)
     * minWidth - despite the old name "size", this was always the target's longest
     *           edge in pixels (Thumbnailator/cwebp both fit-within-box on the longest
     *           edge while preserving aspect ratio) — renamed here to say what it means.
     */
    private record VariantSpec(String label, int minWidth) {
    }

    public record UploadSettings(boolean resizeEnabled, String format, int quality, String resizeMode, String variant, Integer customWidth, Integer customHeight, String namingMode, String prefix, String suffixStyle) {
    }

    static UploadSettings resolveUploadSettings(String format, Integer quality, String resizeMode, String variant, Integer customWidth, Integer customHeight, String namingMode, String prefix, String suffixStyle) {
        String normalizedFormat = format == null || format.isBlank() ? "webp" : format.trim().toLowerCase();
        if (!"webp".equals(normalizedFormat) && !"jpg".equals(normalizedFormat)) {
            normalizedFormat = "webp";
        }

        String normalizedResizeMode = resizeMode == null || resizeMode.isBlank() ? "all4" : resizeMode.trim().toLowerCase();
        if (!"all4".equals(normalizedResizeMode) && !"specific".equals(normalizedResizeMode)) {
            normalizedResizeMode = "all4";
        }

        String normalizedVariant = variant == null || variant.isBlank() ? null : variant.trim().toLowerCase();
        if (normalizedVariant != null && !List.of("large", "medium", "compact", "thumbnail", "custom").contains(normalizedVariant)) {
            normalizedVariant = null;
        }

        int normalizedQuality = quality == null ? 80 : quality;
        if (normalizedQuality < 50) {
            normalizedQuality = 50;
        } else if (normalizedQuality > 100) {
            normalizedQuality = 100;
        }

        Integer normalizedWidth = customWidth == null || customWidth < 1 ? null : customWidth;
        Integer normalizedHeight = customHeight == null || customHeight < 1 ? null : customHeight;

        String normalizedNamingMode = namingMode == null || namingMode.isBlank() ? "original" : namingMode.trim().toLowerCase();
        if (!"original".equals(normalizedNamingMode) && !"random".equals(normalizedNamingMode) && !"manual".equals(normalizedNamingMode)) {
            normalizedNamingMode = "original";
        }

        String normalizedPrefix = prefix == null ? "" : prefix.trim();
        String normalizedSuffixStyle = suffixStyle == null || suffixStyle.isBlank() ? "size" : suffixStyle.trim().toLowerCase();
        if (!"size".equals(normalizedSuffixStyle) && !"none".equals(normalizedSuffixStyle)) {
            normalizedSuffixStyle = "size";
        }

        return new UploadSettings(true, normalizedFormat, normalizedQuality, normalizedResizeMode, normalizedVariant, normalizedWidth, normalizedHeight, normalizedNamingMode, normalizedPrefix, normalizedSuffixStyle);
    }

    private String generateRandomString() {
        return generateRandomString(24);
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    public List<String> uploadAndProcessImages(MultipartFile[] files, String selectedFolder, String format, Integer quality, String resizeMode, String variant, Integer customWidth, Integer customHeight, String namingMode, String prefix, String suffixStyle) throws IOException {
        UploadSettings settings = resolveUploadSettings(format, quality, resizeMode, variant, customWidth, customHeight, namingMode, prefix, suffixStyle);
        return uploadAndProcessImages(files, selectedFolder, settings);
    }

    // ======================================================================
    // Unified image pipeline
    //
    // Both JPEG and WebP output go through this single path. What differs
    // between the two formats is only the encode step (ImageIO writer for
    // JPEG vs. shelling out to the `cwebp` CLI for WebP); everything about
    // decoding the source, resolving which sizes to generate, deciding
    // whether to upscale, and naming the output file is shared.
    // ======================================================================

    /**
     * Processes every uploaded file into one or more resized copies and uploads
     * each to storage.
     *
     * Flow per file:
     *   1. Decode the source exactly once, bounded to MAX_SOURCE_DIMENSION via
     *      ImageIO subsampling — this caps peak decode memory regardless of the
     *      source's native resolution (a small compressed file size does not
     *      imply a small pixel count).
     *   2. Resolve the requested variants (either the fixed large/medium/compact/
     *      thumbnail set, or a single specific/custom size), ordered largest to
     *      smallest.
     *   3. For each variant, from largest to smallest:
     *        - If the current working image is already at or below the variant's
     *          target width, DO NOT upscale — re-use it as-is for this slot
     *          instead of enlarging it. A file is still produced for every
     *          requested variant (so callers always get a consistent set of
     *          URLs), it's just not artificially blown up past its real
     *          resolution.
     *        - Otherwise, resize down from the current working image (which for
     *          JPEG is the previous variant's already-smaller output, not the
     *          original) and make that the new working image. This means only
     *          the largest variant ever pays for a "real" resize pass in the
     *          common case.
     *        - Encode to the target format and upload.
     *   4. Release all intermediate buffers/temp files.
     *
     * Note on WebP specifically: cwebp doesn't take an in-memory BufferedImage
     * or chain cleanly from a previous *compressed* WebP output without
     * compounding quality loss across generations, so instead of chaining like
     * the JPEG path does, every WebP variant is resized by cwebp from the same
     * single bounded intermediate (written once, from the same decode step).
     * That intermediate is already capped at MAX_SOURCE_DIMENSION, so this
     * isn't paying full-resolution cost per variant — it's a deliberate,
     * format-appropriate difference, not an inconsistency.
     */
    public List<String> uploadAndProcessImages(MultipartFile[] files, String selectedFolder, UploadSettings settings) throws IOException {
        List<String> generatedFileNames = new ArrayList<>();
        if (files == null || files.length == 0) {
            return generatedFileNames;
        }
        validateBatchSize(files);

        boolean isWebp = "webp".equals(settings.format());

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String baseName = StringUtils.cleanPath(file.getOriginalFilename());
            if (baseName == null || baseName.isBlank()) {
                baseName = "image";
            }
            String namingBase = resolveNamingBase(baseName, settings);
            String savedName = processOneImage(file, selectedFolder, settings, namingBase, generatedFileNames.size(), isWebp);
            generatedFileNames.add(savedName);
        }

        return generatedFileNames;
    }

    private String processOneImage(MultipartFile file, String selectedFolder, UploadSettings settings,
            String namingBase, int currentCount, boolean isWebp) throws IOException {

        String extension = isWebp ? "webp" : "jpg";
        float jpegQuality = (float) Math.max(0.5, Math.min(1.0, settings.quality() / 100.0));

        List<VariantSpec> orderedVariants = resolveResolvedVariants(settings).stream()
                .sorted(Comparator.comparingInt(VariantSpec::minWidth).reversed())
                .toList();

        BufferedImage currentSource = readImageBounded(file, MAX_SOURCE_DIMENSION);
        File webpIntermediate = null;
        String savedName = "";

        try {
            if (isWebp) {
                webpIntermediate = File.createTempFile("webp_source_", ".png");
                ImageIO.write(currentSource, "png", webpIntermediate);
            }

            for (VariantSpec variant : orderedVariants) {
                int targetLongestEdge = variant.minWidth();
                int currentLongestEdge = Math.max(currentSource.getWidth(), currentSource.getHeight());
                boolean needsResize = targetLongestEdge < currentLongestEdge;

                String targetFileName = buildTargetFileName(namingBase, settings, variant.label(), extension);
                String objectPath = buildImageObjectPath(selectedFolder, targetFileName);

                byte[] outputBytes;
                BufferedImage nextSource = currentSource;

                if (isWebp) {
                    File tempWebpFile = File.createTempFile("webp_out_", ".webp");
                    try {
                        runCwebp(webpIntermediate, tempWebpFile, settings.quality(), targetLongestEdge, needsResize);
                        outputBytes = Files.readAllBytes(tempWebpFile.toPath());
                    } finally {
                        tempWebpFile.delete();
                    }
                } else {
                    if (needsResize) {
                        nextSource = Thumbnails.of(currentSource).size(targetLongestEdge, targetLongestEdge).asBufferedImage();
                    }
                    outputBytes = encodeJpeg(nextSource, jpegQuality);
                }

                String contentType = isWebp ? "image/webp" : "image/jpeg";
                supabaseStorageService.uploadBytes(SupabaseStorageService.StorageRoot.IMAGES, objectPath, outputBytes, contentType);
                saveProductImageRecord(objectPath, currentCount + 1, variant.label());
                savedName = targetFileName;

                if (!isWebp && needsResize && nextSource != currentSource) {
                    currentSource.flush();
                    currentSource = nextSource;
                }
            }
        } finally {
            if (currentSource != null) {
                currentSource.flush();
            }
            if (webpIntermediate != null) {
                webpIntermediate.delete();
            }
        }

        return savedName;
    }

    /**
     * Same variant resolution as resolveVariants(), but also applies the
     * "specific" + "custom" size override up front instead of re-checking it
     * inside the processing loop.
     */
    private List<VariantSpec> resolveResolvedVariants(UploadSettings settings) {
        if (settings.resizeMode().equals("specific") && "custom".equals(settings.variant())) {
            int minWidth = Math.max(
                    settings.customWidth() == null ? 400 : settings.customWidth(),
                    settings.customHeight() == null ? 400 : settings.customHeight());
            return List.of(new VariantSpec(null, minWidth));
        }
        return resolveVariants(settings);
    }

    /**
     * Reads an image bounded to maxDimension on its longest edge using
     * ImageIO's native subsampling, so a large source is never fully decoded
     * at its native resolution just to be immediately downscaled — this bounds
     * peak memory for the initial decode step regardless of the source's
     * pixel dimensions.
     */
    private BufferedImage readImageBounded(MultipartFile file, int maxDimension) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(file.getInputStream())) {
            if (iis == null) {
                throw new IOException("Unable to read image stream: " + file.getOriginalFilename());
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported or corrupt image format: " + file.getOriginalFilename());
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                int longestEdge = Math.max(width, height);

                ImageReadParam param = reader.getDefaultReadParam();
                if (longestEdge > maxDimension) {
                    int subsampling = (int) Math.ceil((double) longestEdge / maxDimension);
                    param.setSourceSubsampling(subsampling, subsampling, 0, 0);
                }

                BufferedImage image = reader.read(0, param);
                if (image == null) {
                    throw new IOException("Failed to decode image: " + file.getOriginalFilename());
                }
                return image;
            } finally {
                reader.dispose();
            }
        }
    }

    /** Encodes a BufferedImage as JPEG bytes at the given quality (0.0–1.0), without any resizing. */
    private byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available on this JVM");
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            javax.imageio.stream.MemoryCacheImageOutputStream ios = new javax.imageio.stream.MemoryCacheImageOutputStream(baos);
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }

            // JPEG doesn't support an alpha channel — flatten if the source has one.
            BufferedImage rgbImage = image;
            if (image.getColorModel().hasAlpha()) {
                rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                rgbImage.createGraphics().drawImage(image, 0, 0, java.awt.Color.WHITE, null);
            }

            writer.write(null, new IIOImage(rgbImage, null, null), param);
            ios.flush();
            return baos.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    /**
     * Runs cwebp via ProcessBuilder with array-form arguments — avoids Runtime.exec's
     * naive whitespace tokenizing of a single command string (which silently breaks on
     * paths containing spaces, and is a shell-injection-adjacent pattern to avoid even
     * when inputs are currently trusted). Also enforces a timeout and drains process
     * output, since an un-drained stdout/stderr pipe can deadlock the process once its
     * OS buffer fills.
     *
     * When resize is false, the -resize flag is omitted entirely so cwebp encodes the
     * source at its actual dimensions rather than upscaling it to targetLongestEdge.
     */
    private void runCwebp(File sourceFile, File outputFile, int quality, int targetLongestEdge, boolean resize) throws IOException {
        List<String> command = new ArrayList<>(List.of("cwebp", "-q", String.valueOf(quality)));
        if (resize) {
            command.addAll(List.of("-resize", String.valueOf(targetLongestEdge), "0"));
        }
        command.addAll(List.of(sourceFile.getAbsolutePath(), "-o", outputFile.getAbsolutePath()));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new IOException("Failed to start cwebp — is it installed and on PATH?", e);
        }

        boolean finished;
        try {
            finished = process.waitFor(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("cwebp process was interrupted", e);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("cwebp timed out after 30s (target=" + targetLongestEdge + ")");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("cwebp exited with code " + exitCode + " (target=" + targetLongestEdge + ")");
        }
    }

    public List<String> uploadRawImages(MultipartFile[] files, String selectedFolder) throws IOException {
        List<String> uploadedFileNames = new ArrayList<>();

        if (files == null || files.length == 0) {
            return uploadedFileNames;
        }

        validateBatchSize(files);

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String rawFileName = StringUtils.cleanPath(file.getOriginalFilename());
            if (rawFileName == null || rawFileName.isEmpty()) {
                continue;
            }

            String objectPath = selectedFolder != null && !selectedFolder.trim().isEmpty()
                    ? selectedFolder.trim() + "/" + rawFileName
                    : rawFileName;
            supabaseStorageService.uploadFile(SupabaseStorageService.StorageRoot.IMAGES, file, objectPath);
            uploadedFileNames.add(rawFileName);
        }

        return uploadedFileNames;
    }

    private void validateBatchSize(MultipartFile[] files) throws IOException {
        if (files == null) {
            return;
        }

        long totalBytes = 0L;
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                totalBytes += file.getSize();
            }
        }

        if (totalBytes > MAX_TOTAL_UPLOAD_BYTES) {
            throw new IOException(String.format("Tổng dung lượng tệp vượt quá %dMB. Vui lòng chọn ít file hơn hoặc giảm kích thước.", MAX_TOTAL_UPLOAD_BYTES / (1024 * 1024)));
        }
    }

    private List<VariantSpec> resolveVariants(UploadSettings settings) {
        if ("specific".equals(settings.resizeMode())) {
            String variant = settings.variant();
            if ("custom".equals(variant)) {
                return List.of(new VariantSpec(null, Math.max(settings.customWidth() == null ? 400 : settings.customWidth(), settings.customHeight() == null ? 400 : settings.customHeight())));
            }
            if (variant != null && !variant.isBlank()) {
                return List.of(new VariantSpec(variant, resolveMinWidthForVariant(variant)));
            }
        }
        return List.of(
                new VariantSpec("large", 2048),
                new VariantSpec("medium", 800),
                new VariantSpec("compact", 400),
                new VariantSpec("thumbnail", 160));
    }

    /**
     * Builds the "core" identifier for a file — either the sanitized original
     * filename, a generated random string, or (in manual mode) nothing, letting
     * the user-supplied prefix stand alone. This is deliberately just the
     * middle piece; buildTargetFileName() below combines it with the prefix
     * and suffix.
     */
    private String resolveNamingBase(String originalFileName, UploadSettings settings) {
        String baseName = originalFileName.contains(".") ? originalFileName.substring(0, originalFileName.lastIndexOf('.')) : originalFileName;
        return switch (settings.namingMode()) {
            case "random" -> generateRandomString();
            case "manual" -> "";
            default -> baseName; // "original"
        };
    }

    /**
     * Filename structure is exactly three parts, in order:
     *   [prefix_] + core + [_suffix] + .ext
     * - prefix: user-supplied (settings.prefix()), added only if non-blank
     * - core:   the original filename, a random string, or empty — see resolveNamingBase()
     * - suffix: the variant label ("large"/"medium"/"compact"/"thumbnail"), added
     *           only if suffixStyle isn't "none"
     * Any part that's blank is simply omitted rather than leaving a stray
     * separator behind.
     */
    private String buildTargetFileName(String namingBase, UploadSettings settings, String variantLabel, String extension) {
        String prefix = settings.prefix() == null ? "" : settings.prefix().trim();
        String suffix = resolveSuffix(settings, variantLabel);

        List<String> parts = new ArrayList<>();
        if (!prefix.isBlank()) {
            parts.add(prefix);
        }
        if (!namingBase.isBlank()) {
            parts.add(namingBase);
        }
        String base = String.join("_", parts);
        if (base.isBlank()) {
            // Every naming mode produced nothing usable (e.g. manual mode with no
            // prefix given) — fall back to a random string so we still get a valid,
            // unique filename instead of one that's just ".jpg".
            base = generateRandomString();
        }

        return String.format("%s%s.%s", base, suffix, extension);
    }

    private String resolveSuffix(UploadSettings settings, String variantLabel) {
        if (settings.suffixStyle() != null && "none".equals(settings.suffixStyle())) {
            return "";
        }
        String normalized = variantLabel == null || variantLabel.isBlank() ? null : variantLabel.trim().toLowerCase();
        return normalized == null ? "" : "_" + normalized;
    }

    private int resolveMinWidthForVariant(String variant) {
        switch (variant) {
            case "large":
                return 2048;
            case "medium":
                return 800;
            case "compact":
                return 400;
            case "thumbnail":
                return 160;
            default:
                return 600;
        }
    }

    private void saveProductImageRecord(String objectPath, int displayOrder, String variant) {
        try {
            String publicUrl = productImageService.resolvePublicUrl(objectPath);
            String normalizedVariant = variant == null || variant.isBlank() ? null : variant.trim().toLowerCase();
            ProductImage image = new ProductImage(objectPath, publicUrl, displayOrder, normalizedVariant);
            productImageRepository.save(image);
        } catch (Exception ex) {
            // Keep uploads working even if the ProductImage table is not available in the current database.
        }
    }

    private String buildImageObjectPath(String selectedFolder, String fileName) {
        String normalizedFolder = StringUtils.cleanPath(selectedFolder == null ? "" : selectedFolder.trim());
        normalizedFolder = normalizedFolder == null ? "" : normalizedFolder.replace('\\', '/').replaceAll("^/|/$", "");
        if (normalizedFolder.isBlank()) {
            return fileName;
        }
        return normalizedFolder + "/" + fileName;
    }
}