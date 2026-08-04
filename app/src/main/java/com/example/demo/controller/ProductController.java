package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ProductAdminDto;
import com.example.demo.dto.ProductDetailDto;
import com.example.demo.dto.ProductSummaryDto;
import com.example.demo.model.Product;
import com.example.demo.service.ProductService;

@RestController
@RequestMapping("/api/products") // Đường dẫn gốc cho tất cả API liên quan đến sản phẩm
@CrossOrigin(origins = "*") // Cho phép Front-end truy cập từ mọi nguồn
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * API 1: Lấy tất cả sản phẩm đang hoạt động (Hiển thị trang chủ)
     * GET http://localhost:8080/api/products
     * @return Danh sách Product
     */
    @GetMapping
    public List<ProductSummaryDto> getAllProducts() {
        return productService.getAllActiveProducts().stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @PostMapping("/list-by-ids")
    public ResponseEntity<?> getProductsByIds(@RequestBody List<Long> ids) {
        List<ProductSummaryDto> products = productService.getProductsByIds(ids).stream()
                .map(this::toSummaryDto)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProductSummaryDto> getProductJsonBySlug(@PathVariable String slug) {
        // Input: slug của sản phẩm.
        // Output: entity Product đầy đủ thông tin cơ bản.
        // Expected return: 200 OK với Product nếu tồn tại, 404 nếu không tìm thấy.
        return productService.getProductBySlug(slug)
                .map(this::toSummaryDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/detail/{slug}")
    public ResponseEntity<ProductDetailDto> getProductDetail(@PathVariable String slug) {
        // Nếu có dữ liệu -> 200 OK + Body
        // Nếu không có (Optional rỗng) -> 404 Not Found
        return productService.getProductDetailBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/admin")
    public List<ProductAdminDto> getAdminProducts() {
        return productService.getAdminProducts();
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<ProductAdminDto> getAdminProduct(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/admin")
    public ResponseEntity<Product> saveAdminProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productService.createOrUpdateProduct(product));
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> deleteAdminProduct(@PathVariable Long id) {
        productService.softDeleteProduct(id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    private ProductSummaryDto toSummaryDto(Product product) {
        ProductSummaryDto dto = new ProductSummaryDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setSlug(product.getSlug());
        dto.setPrice(product.getPrice());
        dto.setImageUrl(product.getImageUrl());
        dto.setShortDescription(product.getShortDescription());
        return dto;
    }
}