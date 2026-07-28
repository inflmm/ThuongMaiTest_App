package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ProductDetailDto;
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
    public List<Product> getAllProducts() {
        // Gọi Service để lấy dữ liệu (chỉ lấy các sản phẩm chưa bị xóa)
        return productService.getAllActiveProducts();
    }

    @PostMapping("/list-by-ids")
    public ResponseEntity<?> getProductsByIds(@RequestBody List<Long> ids) {
        // Giả sử bạn dùng ProductRepository.findAllById(ids)
        List<Product> products = productService.getProductsByIds(ids);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<Product> getProductJsonBySlug(@PathVariable String slug) {
        return productService.getProductBySlug(slug)
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
}