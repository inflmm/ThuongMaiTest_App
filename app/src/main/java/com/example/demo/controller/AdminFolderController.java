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
import com.example.demo.service.SupabaseStorageService.StorageRoot;

@RestController
@RequestMapping("/api/admin/folders")
public class AdminFolderController {

    @Autowired
    private AdminService adminService;

    @GetMapping
    public ResponseEntity<?> getFolderTree() throws IOException {
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
    public ResponseEntity<?> getImageFolderTree() throws IOException {
        List<String> folders = adminService.listFolders(StorageRoot.IMAGES, "");
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/images/files")
    public ResponseEntity<?> getFilesByFolder(@RequestParam String path) throws IOException {
        List<String> files = adminService.listFiles(StorageRoot.IMAGES, path);
        return ResponseEntity.ok(files);
    }
}