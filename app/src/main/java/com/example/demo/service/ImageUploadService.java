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

import com.example.demo.repository.ProductRepository;

import net.coobird.thumbnailator.Thumbnails;

@Service
public class ImageUploadService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    private String generateRandomString() {
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private int getNextImageIndex(String selectedFolder, String productSlug) throws IOException {
        if (productSlug == null || productSlug.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã Slug sản phẩm không được để trống!");
        }

        boolean isProductExist = productRepository.existsBySlug(productSlug);
        if (!isProductExist) {
            throw new IllegalArgumentException("Lỗi: Mã Slug sản phẩm '" + productSlug + "' không tồn tại trong hệ thống!");
        }

        String prefix = productSlug + "/master";
        if (selectedFolder != null && !selectedFolder.trim().isEmpty()) {
            prefix = selectedFolder.trim() + "/" + prefix;
        }

        List<String> existingFiles = supabaseStorageService.listObjects(SupabaseStorageService.StorageRoot.IMAGES, prefix, false);
        return (int) existingFiles.stream()
                .filter(name -> name.startsWith("prod_"))
                .count() + 1;
    }

    public List<String> uploadAndProcessProductImagesJpg(MultipartFile[] files, String selectedFolder, String productSlug) throws IOException {
        List<String> generatedFileNames = new ArrayList<>();
        if (files == null || files.length == 0) return generatedFileNames;

        int currentImageIndex = getNextImageIndex(selectedFolder, productSlug);
        String[] subFolders = {"compact", "grande", "master"};

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String randomStr = generateRandomString();
            String savedMasterName = "";

            for (String sub : subFolders) {
                String targetFileName = String.format("prod_%d_%s_%s.jpg", currentImageIndex, randomStr, sub);
                int size = 2048;
                if (sub.equals("compact")) size = 160;
                else if (sub.equals("grande")) size = 600;

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    Thumbnails.of(file.getInputStream())
                            .size(size, size)
                            .outputFormat("jpg")
                            .outputQuality(0.75)
                            .toOutputStream(baos);

                    String objectPath = buildImageObjectPath(selectedFolder, productSlug, sub, targetFileName);
                    supabaseStorageService.uploadBytes(SupabaseStorageService.StorageRoot.IMAGES, objectPath, baos.toByteArray(), "image/jpeg");
                }

                if (sub.equals("master")) {
                    savedMasterName = targetFileName;
                }
            }

            generatedFileNames.add(savedMasterName);
            currentImageIndex++;
        }

        return generatedFileNames;
    }

    public List<String> uploadAndProcessProductImagesWebp(MultipartFile[] files, String selectedFolder, String productSlug) throws IOException {
        List<String> generatedFileNames = new ArrayList<>();
        if (files == null || files.length == 0) return generatedFileNames;

        int currentImageIndex = getNextImageIndex(selectedFolder, productSlug);
        String[] subFolders = {"compact", "grande", "master"};

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String randomStr = generateRandomString();
            String savedMasterName = "";
            File tempOriginalFile = File.createTempFile("raw_upload_", "_" + file.getOriginalFilename());
            file.transferTo(tempOriginalFile);

            try {
                for (String sub : subFolders) {
                    String targetFileName = String.format("prod_%d_%s_%s.webp", currentImageIndex, randomStr, sub);
                    int size = 2048;
                    if (sub.equals("compact")) size = 160;
                    else if (sub.equals("grande")) size = 600;

                    File tempWebpFile = File.createTempFile("webp_out_", ".webp");
                    String command = String.format("cwebp -q 85 -resize %d 0 \"%s\" -o \"%s\"", size, tempOriginalFile.getAbsolutePath(), tempWebpFile.getAbsolutePath());
                    Process process = Runtime.getRuntime().exec(command);
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        throw new IOException("LỖI HỆ THỐNG: Ứng dụng cwebp trả về lỗi hệ điều hành (Mã lỗi: " + exitCode + ").");
                    }

                    byte[] imageBytes = Files.readAllBytes(tempWebpFile.toPath());
                    String objectPath = buildImageObjectPath(selectedFolder, productSlug, sub, targetFileName);
                    supabaseStorageService.uploadBytes(SupabaseStorageService.StorageRoot.IMAGES, objectPath, imageBytes, "image/webp");

                    if (sub.equals("master")) {
                        savedMasterName = targetFileName;
                    }

                    if (tempWebpFile.exists()) {
                        tempWebpFile.delete();
                    }
                }

                generatedFileNames.add(savedMasterName);
                currentImageIndex++;
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

    private String buildImageObjectPath(String selectedFolder, String productSlug, String subFolder, String fileName) {
        String prefix = (selectedFolder == null || selectedFolder.trim().isEmpty())
                ? String.format("%s/%s", productSlug, subFolder)
                : String.format("%s/%s/%s", selectedFolder.trim(), productSlug, subFolder);
        return prefix.replaceAll("//+", "/") + "/" + fileName;
    }
}
