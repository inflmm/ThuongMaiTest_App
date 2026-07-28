package com.example.demo.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/folders")
public class AdminFolderController {

	private final String ROOT_PATH = "C:/ecommerce-uploads/articles/";

	private final String IMAGE_ROOT = "C:/ecommerce-uploads/images/";

    @GetMapping
    public ResponseEntity<?> getFolderTree() throws IOException {
        Path root = Paths.get(ROOT_PATH);
        if (!Files.exists(root)) {
			Files.createDirectories(root);
		}

        // Lấy tất cả thư mục con, trả về list string đường dẫn tương đối
        List<String> folders = Files.walk(root)
                .filter(Files::isDirectory)
                .map(path -> root.relativize(path).toString().replace("\\", "/"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        return ResponseEntity.ok(folders);
    }

    @PostMapping
    public ResponseEntity<?> createFolder(@RequestParam String path) {
        try {
            Path newDirPath = Paths.get(ROOT_PATH).resolve(path);
            Files.createDirectories(newDirPath);
            return ResponseEntity.ok("Đã tạo thư mục: " + path);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Không thể tạo thư mục");
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteFolder(@RequestParam String path) {
        try {
            Path dirPath = Paths.get(ROOT_PATH).resolve(path);

            if (!Files.exists(dirPath)) {
                return ResponseEntity.status(404).body("Thư mục không tồn tại");
            }

            // Kiểm tra xem thư mục có trống không
            try (var stream = Files.list(dirPath)) {
                if (stream.findFirst().isPresent()) {
                    return ResponseEntity.badRequest().body("Thư mục còn dữ liệu! Vui lòng xóa hết nội dung bên trong trước.");
                }
            }

            Files.delete(dirPath);
            return ResponseEntity.ok("Đã xóa thư mục thành công");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi khi xóa thư mục: " + e.getMessage());
        }
    }

    @GetMapping("/images/tree")
    public ResponseEntity<?> getImageFolderTree() throws IOException {
        Path root = Paths.get(IMAGE_ROOT);
        if (!Files.exists(root)) {
			Files.createDirectories(root);
		}

        List<String> folders = Files.walk(root)
                .filter(Files::isDirectory)
                .map(path -> root.relativize(path).toString().replace("\\", "/"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/images/files")
    public ResponseEntity<?> getFilesByFolder(@RequestParam String path) throws IOException {
        Path dirPath = Paths.get(IMAGE_ROOT).resolve(path);
        if (!Files.exists(dirPath)) {
			return ResponseEntity.notFound().build();
		}

        try (var stream = Files.list(dirPath)) {
            List<String> files = stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(file -> file.getFileName().toString())
                    // Chỉ lấy các định dạng ảnh phổ biến
                    .filter(name -> name.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|webp|svg)$"))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(files);
        }
    }
}