package com.example.demo.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.service.ImageUploadService;

@RestController
@RequestMapping("/api/admin/images")
public class AdminImageController {

    @Autowired
    private ImageUploadService imageUploadService; // Service xử lý ảnh sản phẩm (đã làm ở bước trước)


    /**
     * LUỒNG 1: Upload ảnh sản phẩm - Có băm size, đổi tên theo quy tắc, chia 3 thư mục con
     */
    @PostMapping("/product-upload")
    public ResponseEntity<?> uploadProductImage(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "folder", required = false) String selectedFolder,
            @RequestParam("productSlug") String productSlug,
            @RequestParam(value = "format", defaultValue = "jpg") String format) { // Bổ sung param format
        try {
            List<String> baseNames;
            
            // Kiểm tra loại định dạng người dùng chọn từ giao diện Frontend
            if ("webp".equalsIgnoreCase(format)) {
                baseNames = imageUploadService.uploadAndProcessProductImagesWebp(files, selectedFolder, productSlug);
            } else {
                // Mặc định hoặc khi chọn JPG
                baseNames = imageUploadService.uploadAndProcessProductImagesJpg(files, selectedFolder, productSlug);
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Upload và xử lý ảnh sản phẩm (" + format.toUpperCase() + ") thành công!",
                "data", baseNames
            ));
        } catch (IllegalArgumentException e) {
            // Bắt lỗi kiểm tra ràng buộc (như trống slug sản phẩm)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi cấu trúc hoặc nén ảnh: " + e.getMessage()));
        }
    }

    /**
     * LUỒNG 2: Upload ảnh thông thường - Giữ nguyên kích thước, giữ nguyên tên, không tạo sub-folder
     */
    @PostMapping("/raw-upload")
    public ResponseEntity<?> uploadRawImage(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("folder") String selectedFolder) {
        try {
            List<String> fileNames = imageUploadService.uploadRawImages(files, selectedFolder);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Upload ảnh gốc thành công",
                "data", fileNames
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi ghi file hệ thống: " + e.getMessage()));
        }
    }
}