package com.example.demo.controller.admin;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.service.ImageUploadService;

// Content management — shared between ADMIN and EMPLOYEE.
@RestController
@RequestMapping("/api/admin/images")
public class AdminImageController {

    private final ImageUploadService imageUploadService;

    public AdminImageController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    // ---------------------------------------------------------------
    // Writes (this controller has no reads — listing/browsing uploaded
    // images lives in AdminFolderController)
    // ---------------------------------------------------------------

    /**
     * LUỒNG 1: Upload ảnh sản phẩm - Có băm size, đổi tên theo quy tắc, chia 3 thư mục con
     */
    @PostMapping("/product-upload")
    public ResponseEntity<?> uploadProductImage(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "folder", required = false) String selectedFolder,
            @RequestParam(value = "format", defaultValue = "webp") String format,
            @RequestParam(value = "quality", defaultValue = "85") Integer quality,
            @RequestParam(value = "resizeMode", defaultValue = "all4") String resizeMode,
            @RequestParam(value = "variant", required = false) String variant,
            @RequestParam(value = "customWidth", required = false) Integer customWidth,
            @RequestParam(value = "customHeight", required = false) Integer customHeight,
            @RequestParam(value = "namingMode", required = false) String namingMode,
            @RequestParam(value = "prefix", required = false) String prefix,
            @RequestParam(value = "suffixStyle", required = false) String suffixStyle) {
        try {
            List<String> baseNames = imageUploadService.uploadAndProcessImages(files, selectedFolder, format, quality, resizeMode, variant, customWidth, customHeight, namingMode, prefix, suffixStyle);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Upload và xử lý ảnh thành công!",
                    "data", baseNames));
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
            @RequestParam(value = "folder", required = false) String selectedFolder) {
        try {
            List<String> fileNames = imageUploadService.uploadRawImages(files, selectedFolder);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Upload ảnh gốc thành công",
                    "data", fileNames));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi ghi file hệ thống: " + e.getMessage()));
        }
    }
}
