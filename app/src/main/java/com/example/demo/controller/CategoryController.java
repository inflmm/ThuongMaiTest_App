package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CategoryAdminDto;
import com.example.demo.dto.CategoryTreeNodeDto;
import com.example.demo.dto.ProductSummaryDto;
import com.example.demo.model.Category;
import com.example.demo.service.CategoryService;

@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public List<Category> getRootCategories() {
        return categoryService.getRootCategories();
    }

    @GetMapping("/categories/tree")
    public List<CategoryAdminDto> getCategoryTree() {
        return categoryService.getCategoryTreeDto();
    }

    @GetMapping("/categories/storefront-tree")
    public List<CategoryTreeNodeDto> getStorefrontCategoryTree() {
        return categoryService.getCategoryTreeForStorefront();
    }

    @GetMapping("/categories/{slug}")
    public CategoryDto getCategory(@PathVariable String slug) {
        return categoryService.getCategoryBySlug(slug)
                .map(this::toCategoryDto)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    @GetMapping("/categories/{slug}/products")
    public List<ProductSummaryDto> getProductsForCategory(@PathVariable String slug) {
        return categoryService.getProductsForCategorySlug(slug).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    private ProductSummaryDto toSummaryDto(com.example.demo.model.Product product) {
        ProductSummaryDto dto = new ProductSummaryDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setSlug(product.getSlug());
        dto.setPrice(product.getPrice());
        dto.setImageUrl(product.getImageUrl());
        dto.setShortDescription(product.getShortDescription());
        return dto;
    }

    private CategoryDto toCategoryDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setSlug(category.getSlug());
        dto.setLevel(category.getLevel());
        dto.setDisplayOrder(category.getDisplayOrder());
        dto.setProductCount(category.getProductCount() != null ? category.getProductCount() : 0);
        dto.setAdminProductCount(category.getAdminProductCount() != null ? category.getAdminProductCount() : 0);
        return dto;
    }

    private static class CategoryDto {
        private Long id;
        private String name;
        private String slug;
        private Integer level;
        private Integer displayOrder;
        private Integer productCount;
        private Integer adminProductCount;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public Integer getLevel() { return level; }
        public void setLevel(Integer level) { this.level = level; }
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
        public Integer getProductCount() { return productCount; }
        public void setProductCount(Integer productCount) { this.productCount = productCount; }
        public Integer getAdminProductCount() { return adminProductCount; }
        public void setAdminProductCount(Integer adminProductCount) { this.adminProductCount = adminProductCount; }
    }

}
