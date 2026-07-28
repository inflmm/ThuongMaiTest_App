package com.example.demo.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Blog;
import com.example.demo.service.BlogService;

@RestController
@RequestMapping("/api/blogs")
@CrossOrigin(origins = "*") // Cho phép gọi API từ các domain khác nếu cần
public class BlogController {

    @Autowired
    private BlogService blogService;

    @GetMapping("/{slug}")
    public ResponseEntity<?> getBlogBySlug(@PathVariable String slug) {
        Optional<Blog> blog = blogService.getPublicBlogBySlug(slug);

        if (blog == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(blog);
    }
}