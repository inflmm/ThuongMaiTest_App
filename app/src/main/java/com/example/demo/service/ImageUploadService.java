package com.example.demo.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

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
            String randomStr = generateRandomString();
            String baseFileName = baseName.contains(".") ? baseName.substring(0, baseName.lastIndexOf('.')) : baseName;
            String extension = baseName.contains(".") ? baseName.substring(baseName.lastIndexOf('.') + 1) : "jpg";
            String namingBase = resolveNamingBase(baseName, settings);
            String savedName = "";
            List<VariantSpec> variants = resolveVariants(settings);

            for (VariantSpec variantSpec : variants) {
                String targetFileName = buildTargetFileName(namingBase, settings, variantSpec.variant(), extension);
                int size = variantSpec.size();
                if (settings.resizeMode().equals("specific") && "custom".equals(settings.variant())) {
                    size = Math.max(settings.customWidth() == null ? 400 : settings.customWidth(), settings.customHeight() == null ? 400 : settings.customHeight());
                }

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    Thumbnails.of(file.getInputStream())
                            .size(size, size)
                            .outputFormat("jpg")
                            .outputQuality(Math.max(0.5, Math.min(1.0, settings.quality() / 100.0)))
                            .toOutputStream(baos);

                    String objectPath = buildImageObjectPath(selectedFolder, targetFileName);
                    supabaseStorageService.uploadBytes(SupabaseStorageService.StorageRoot.IMAGES, objectPath, baos.toByteArray(), "image/jpeg");
                    saveProductImageRecord(objectPath, generatedFileNames.size() + 1, variantSpec.variant());
                    savedName = targetFileName;
                }
            }

            generatedFileNames.add(savedName);
        }

        return generatedFileNames;
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
