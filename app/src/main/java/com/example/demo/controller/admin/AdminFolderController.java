package com.example.demo.controller.admin;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AdminService;
import com.example.demo.service.ProductImageService;
import com.example.demo.service.SupabaseStorageService.StorageRoot;

// Reads shared between ADMIN and EMPLOYEE. Folder deletion is ADMIN-only —
// it's destructive and irreversible, a different risk tier from routine
// content edits elsewhere in the admin panel.
@RestController
@RequestMapping("/api/admin/folders")
public class AdminFolderController {

    private final AdminService adminService;
    private final ProductImageService productImageService;

    public AdminFolderController(AdminService adminService, ProductImageService productImageService) {
        this.adminService = adminService;
        this.productImageService = productImageService;
    }

    // ---------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------

    @GetMapping
    public ResponseEntity<?> getFolderTree() {
        List<String> folders = adminService.listFolders(StorageRoot.ARTICLES, "");
        return ResponseEntity.ok(folders);
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
        List<Map<String, String>> payload = files.stream().map(fileName -> {
            String normalizedName = fileName == null ? "" : fileName.replace('\\', '/').trim();
            if (normalizedName.isBlank()) {
                return Map.of("name", "", "path", "", "publicUrl", "");
            }

            String normalizedPrefix = (path == null ? "" : path).replace('\\', '/').trim().replaceAll("^/|/$", "");
            String objectPath = normalizedName.startsWith("images/")
                    ? normalizedName
                    : (normalizedPrefix.isBlank() || normalizedName.startsWith(normalizedPrefix + "/")
                            ? "images/" + normalizedName
                            : "images/" + normalizedPrefix + "/" + normalizedName);

            String publicUrl = productImageService.resolvePublicUrl(objectPath);
            String displayName = normalizedName.substring(normalizedName.lastIndexOf('/') + 1);
            return Map.of(
                    "name", displayName,
                    "path", normalizedName,
                    "publicUrl", publicUrl == null ? "" : publicUrl);
        }).toList();

        return ResponseEntity.ok(payload);
    }

    // ---------------------------------------------------------------
    // Writes
    // ---------------------------------------------------------------

    @PostMapping
    public ResponseEntity<?> createFolder(@RequestParam String path) {
        try {
            adminService.createFolder(StorageRoot.ARTICLES, path);
            return ResponseEntity.ok("Đã tạo thư mục: " + path);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Không thể tạo thư mục: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
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
}
