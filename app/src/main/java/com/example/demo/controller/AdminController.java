package com.example.demo.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Blog;
import com.example.demo.repository.BlogRepository;
import com.example.demo.service.BlogService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

	@Autowired
    private BlogService blogService;

    @Autowired
    private BlogRepository blogRepository;

    // LẤY DANH SÁCH + PHÂN TRANG + LỌC THEO THƯ MỤC
    @GetMapping("/blogs")
    public ResponseEntity<?> getBlogs(
            @RequestParam(required = false) String folder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

    	// Tạo đối tượng Pageable từ Spring Data
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Blog> blogPage;

        if (folder != null && !folder.isEmpty()) {
            // Cần thêm method này vào BlogRepository
            blogPage = blogRepository.findByContentPathAndDeletedFalse(folder, pageable);
        } else {
            blogPage = blogRepository.findByDeletedFalse(pageable);
        }

        return ResponseEntity.ok(blogPage);
    }

    // TẠO MỚI
    @PostMapping("/blogs")
    public ResponseEntity<?> createBlog(@RequestBody Blog blog, @RequestParam String content) {
        try {
            Blog saved = blogService.createNewBlog(blog, content);
            return ResponseEntity.ok(saved);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Lỗi lưu file: " + e.getMessage());
        }
    }

    // CẬP NHẬT
    @PutMapping("/blogs")
    public ResponseEntity<?> updateBlog(@RequestBody Blog blog, @RequestParam String content) {
        try {
            // Blog object lúc này PHẢI có ID gửi kèm trong JSON body
            if (blog.getId() == null) {
                return ResponseEntity.badRequest().body("ID không được để trống khi cập nhật!");
            }
            Blog updated = blogService.updateBlog(blog, content);
            return ResponseEntity.ok(updated);
        } catch (IOException e) {
        	e.printStackTrace();
            return ResponseEntity.internalServerError().body("Lỗi cập nhật: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // XOÁ (Xoá mềm)
    @DeleteMapping("/blogs/{id}")
    public ResponseEntity<?> deleteBlog(@PathVariable Long id) {
        blogService.deleteBlog(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/blogs/{id}/publish")
    public ResponseEntity<?> togglePublishStatus(@PathVariable Long id, @RequestBody boolean status) {
        return blogRepository.findById(id).map(blog -> {
            blog.setPublished(status);
            if (status && blog.getPublishTime() == null) {
                blog.setPublishTime(LocalDateTime.now());
            }
            blogRepository.save(blog);
            return ResponseEntity.ok(status);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/blogs/{id}")
    public ResponseEntity<Blog> getBlogById(@PathVariable Long id) {
        return blogService.getBlogByIdForAdmin(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //Tìm kiếm blog
    @GetMapping("/blogs/search")
    public ResponseEntity<Page<Blog>> searchBlogs(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String contentPath,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Blog> results = blogService.searchBlogs(id, title, fromDate, toDate, contentPath, pageable);
        return ResponseEntity.ok(results);
    }
}