package com.example.demo.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AdminService;
import com.example.demo.service.ProductImageService;
import com.example.demo.service.SupabaseStorageService.StorageRoot;

@RestController
@RequestMapping("/api/admin/folders")
public class AdminFolderController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private ProductImageService productImageService;

    @GetMapping
    public ResponseEntity<?> getFolderTree() {
        List<String> folders = adminService.listFolders(StorageRoot.ARTICLES, "");
        return ResponseEntity.ok(folders);
    }

    @PostMapping
    public ResponseEntity<?> createFolder(@RequestParam String path) {
        try {
            adminService.createFolder(StorageRoot.ARTICLES, path);
            return ResponseEntity.ok("Đã tạo thư mục: " + path);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Không thể tạo thư mục: " + e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteFolder(@RequestParam String path) {
        try {
            adminService.deleteFolder(StorageRoot.ARTICLES, path);
            return ResponseEntity.ok("Đã xóa thư mục thành công");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi khi xóa thư mục: " + e.getMessage());
        }
    }

    @GetMapping("/images/tree")
    public ResponseEntity<?> getImageFolderTree() {
        List<String> folders = adminService.listFolders(StorageRoot.IMAGES, "");
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/images/files")
    public ResponseEntity<?> getFilesByFolder(@RequestParam String path) {
        // The explorer now returns each image as {name, path, publicUrl} so the admin UI can
        // render the actual Supabase public URL directly instead of relying on the app's /images proxy.
        List<String> files = adminService.listFiles(StorageRoot.IMAGES, path);
        List<java.util.Map<String, String>> payload = files.stream().map(fileName -> {
            String normalizedName = fileName == null ? "" : fileName.replace('\\', '/').trim();
            if (normalizedName.isBlank()) {
                return java.util.Map.of("name", "", "path", "", "publicUrl", "");
            }

            String normalizedPrefix = (path == null ? "" : path).replace('\\', '/').trim().replaceAll("^/|/$", "");
            String objectPath = normalizedName.startsWith("images/")
                    ? normalizedName
                    : (normalizedPrefix.isBlank() || normalizedName.startsWith(normalizedPrefix + "/")
                            ? "images/" + normalizedName
                            : "images/" + normalizedPrefix + "/" + normalizedName);

            String publicUrl = productImageService.resolvePublicUrl(objectPath);
            String displayName = normalizedName.substring(normalizedName.lastIndexOf('/') + 1);
            return java.util.Map.of(
                    "name", displayName,
                    "path", normalizedName,
                    "publicUrl", publicUrl == null ? "" : publicUrl);
        }).toList();

        return ResponseEntity.ok(payload);
    }
}