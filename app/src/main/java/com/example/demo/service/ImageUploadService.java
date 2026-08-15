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

    private record VariantSpec(String variant, int size) {
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

        int normalizedQuality = quality == null ? 85 : quality;
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
        if ("webp".equalsIgnoreCase(settings.format())) {
            return uploadAndProcessImagesWebp(files, selectedFolder, settings);
        }
        return uploadAndProcessImagesJpg(files, selectedFolder, settings);
    }

    public List<String> uploadAndProcessImagesJpg(MultipartFile[] files, String selectedFolder, UploadSettings settings) throws IOException {
        List<String> generatedFileNames = new ArrayList<>();
        if (files == null || files.length == 0) {
            return generatedFileNames;
        }
        validateBatchSize(files);

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String baseName = StringUtils.cleanPath(file.getOriginalFilename());
            if (baseName == null || baseName.isBlank()) {
                baseName = "image";
            }
            String extension = "jpg";
            String namingBase = resolveNamingBase(baseName, settings);
            String savedName = processOneImageJpg(file, selectedFolder, settings, namingBase, extension, generatedFileNames.size());
            generatedFileNames.add(savedName);
        }

        return generatedFileNames;
    }

    /**
     * Decodes the source image exactly once (bounded to MAX_SOURCE_DIMENSION via
     * subsampling, so peak memory doesn't scale with the original's native
     * resolution), then generates each variant by downscaling from the previous,
     * smaller output rather than re-decoding the original for every size.
     */
    private String processOneImageJpg(MultipartFile file, String selectedFolder, UploadSettings settings,
            String namingBase, String extension, int currentCount) throws IOException {

        List<VariantSpec> variants = resolveResolvedVariants(settings);
        // Largest first, so each step downsamples from the previous (smaller-cost) step.
        List<VariantSpec> orderedVariants = variants.stream()
                .sorted(Comparator.comparingInt(VariantSpec::size).reversed())
                .toList();

        float jpegQuality = (float) Math.max(0.5, Math.min(1.0, settings.quality() / 100.0));
        String savedName = "";
        BufferedImage currentSource = null;

        try {
            currentSource = readImageBounded(file, MAX_SOURCE_DIMENSION);

            for (VariantSpec variantSpec : orderedVariants) {
                int size = variantSpec.size();

                BufferedImage resized = (size >= Math.max(currentSource.getWidth(), currentSource.getHeight()))
                        ? currentSource
                        : Thumbnails.of(currentSource).size(size, size).asBufferedImage();

                byte[] jpegBytes = encodeJpeg(resized, jpegQuality);

                String targetFileName = buildTargetFileName(namingBase, settings, variantSpec.variant(), extension);
                String objectPath = buildImageObjectPath(selectedFolder, targetFileName);
                supabaseStorageService.uploadBytes(SupabaseStorageService.StorageRoot.IMAGES, objectPath, jpegBytes, "image/jpeg");
                saveProductImageRecord(objectPath, currentCount + 1, variantSpec.variant());
                savedName = targetFileName;

                // Release the previous buffer before chaining to the next, smaller one —
                // don't wait for the whole loop to finish before anything is eligible for GC.
                if (resized != currentSource) {
                    currentSource.flush();
                    currentSource = resized;
                }
            }
        } finally {
            if (currentSource != null) {
                currentSource.flush();
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
            int size = Math.max(
                    settings.customWidth() == null ? 400 : settings.customWidth(),
                    settings.customHeight() == null ? 400 : settings.customHeight());
            return List.of(new VariantSpec(null, size));
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

    public List<String> uploadAndProcessImagesWebp(MultipartFile[] files, String selectedFolder, UploadSettings settings) throws IOException {
        List<String> generatedFileNames = new ArrayList<>();
        if (files == null || files.length == 0) {
            return generatedFileNames;
        }
        validateBatchSize(files);

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String baseName = StringUtils.cleanPath(file.getOriginalFilename());
            if (baseName == null || baseName.isBlank()) {
                baseName = "image";
            }
            String randomStr = generateRandomString();
            String baseFileName = baseName.contains(".") ? baseName.substring(0, baseName.lastIndexOf('.')) : baseName;
            String extension = "webp";
            String namingBase = resolveNamingBase(baseName, settings);
            String savedName = "";
            List<VariantSpec> variants = resolveVariants(settings);
            File tempOriginalFile = File.createTempFile("raw_upload_", "_" + file.getOriginalFilename());
            file.transferTo(tempOriginalFile);

            try {
                for (VariantSpec variantSpec : variants) {
                    String targetFileName = buildTargetFileName(namingBase, settings, variantSpec.variant(), extension);
                    int size = variantSpec.size();
                    if (settings.resizeMode().equals("specific") && "custom".equals(settings.variant())) {
                        size = Math.max(settings.customWidth() == null ? 400 : settings.customWidth(), settings.customHeight() == null ? 400 : settings.customHeight());
                    }

                    File tempWebpFile = File.createTempFile("webp_out_", ".webp");
                    String command = String.format("cwebp -q %d -resize %d 0 \"%s\" -o \"%s\"", settings.quality(), size, tempOriginalFile.getAbsolutePath(), tempWebpFile.getAbsolutePath());
                    Process process = Runtime.getRuntime().exec(command);
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        throw new IOException("LỖI HỆ THỐNG: Ứng dụng cwebp trả về lỗi hệ điều hành (Mã lỗi: " + exitCode + ").");
                    }

                    byte[] imageBytes = Files.readAllBytes(tempWebpFile.toPath());
                    String objectPath = buildImageObjectPath(selectedFolder, targetFileName);
                    supabaseStorageService.uploadBytes(SupabaseStorageService.StorageRoot.IMAGES, objectPath, imageBytes, "image/webp");
                    saveProductImageRecord(objectPath, generatedFileNames.size() + 1, variantSpec.variant());
                    savedName = targetFileName;

                    if (tempWebpFile.exists()) {
                        tempWebpFile.delete();
                    }
                }
                generatedFileNames.add(savedName);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Tiến trình CLI WebP bị ngắt quãng bất thường: " + e.getMessage());
            } finally {
                if (tempOriginalFile.exists()) {
                    tempOriginalFile.delete();
                }
            }
        }

        return generatedFileNames;
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
                return List.of(new VariantSpec(variant, resolveSizeForVariant(variant)));
            }
        }
        return List.of(
                new VariantSpec("large", 2048),
                new VariantSpec("medium", 800),
                new VariantSpec("compact", 400),
                new VariantSpec("thumbnail", 160));
    }

    private String resolveNamingBase(String originalFileName, UploadSettings settings) {
        String baseName = originalFileName.contains(".") ? originalFileName.substring(0, originalFileName.lastIndexOf('.')) : originalFileName;
        if ("original".equals(settings.namingMode())) {
            return baseName;
        }
        if ("manual".equals(settings.namingMode())) {
            String normalizedPrefix = settings.prefix() == null ? "" : settings.prefix().trim();
            return normalizedPrefix.isBlank() ? baseName : normalizedPrefix;
        }
        return generateRandomString();
    }

    private String buildTargetFileName(String namingBase, UploadSettings settings, String variant, String extension) {
        String suffix = resolveSuffix(settings, variant);
        String middle = generateRandomString(32);

        if (settings.namingMode() != null && "original".equals(settings.namingMode())) {
            return String.format("%s%s.%s", namingBase, suffix, extension);
        }
        if (settings.namingMode() != null && "manual".equals(settings.namingMode())) {
            return String.format("%s_%s%s.%s", namingBase, middle, suffix, extension);
        }
        return String.format("%s_%s%s.%s", namingBase, middle, suffix, extension);
    }

    private String resolveSuffix(UploadSettings settings, String variant) {
        String normalizedVariant = variant == null || variant.isBlank() ? null : variant.trim().toLowerCase();
        String suffix = "";
        if (settings.suffixStyle() != null && "none".equals(settings.suffixStyle())) {
            return suffix;
        }
        if (normalizedVariant != null && !normalizedVariant.isBlank()) {
            suffix = "_" + normalizedVariant;
        }
        return suffix;
    }

    private int resolveSizeForVariant(String variant) {
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
