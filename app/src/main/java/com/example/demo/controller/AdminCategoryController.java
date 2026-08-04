package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CategoryAdminDto;
import com.example.demo.model.Category;
import com.example.demo.service.CategoryService;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<Category> getAllCategories() {
        return categoryService.getRootCategories();
    }

    @GetMapping("/tree")
    public List<CategoryAdminDto> getCategoryTree() {
        return categoryService.getCategoryTreeDto();
    }

    @GetMapping("/children/{parentId}")
    public List<Category> getChildren(@PathVariable Long parentId) {
        return categoryService.getChildren(parentId);
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody CategoryPayload payload) {
        try {
            Category created = categoryService.createCategory(payload.name(), payload.parentId(), payload.displayOrder(), payload.iconUrl());
            return ResponseEntity.ok(categoryService.toAdminDto(created));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody CategoryPayload payload) {
        try {
            Category updated = categoryService.updateCategory(id, payload.name(), payload.parentId(), payload.displayOrder(), payload.iconUrl());
            return ResponseEntity.ok(categoryService.toAdminDto(updated));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(Map.of("deleted", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/recalculate-counts")
    public ResponseEntity<?> recalculateCategoryCounts() {
        categoryService.recalculateAllCounts();
        return ResponseEntity.ok(Map.of("message", "Đã tính lại số lượng danh mục"));
    }

    public record CategoryPayload(String name, Long parentId, Integer displayOrder, String iconUrl) {
    }
}
