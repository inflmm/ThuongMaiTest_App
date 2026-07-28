package com.example.demo.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Order;
import com.example.demo.service.CheckoutService;

@RestController
@RequestMapping("/api/checkout")
@CrossOrigin(origins = "*")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    /**
     * API Thanh toán (Chỉ cần POST để kích hoạt logic)
     * POST http://localhost:8080/api/checkout
     * Body: { "paymentMethod": "CASH", "shippingAddress": "..." } (Tạm thời bỏ qua)
     */
    @PostMapping
    public ResponseEntity<?> checkout(Authentication authentication) {
        // Kiểm tra bảo mật: Chỉ cho phép người dùng đã đăng nhập thanh toán
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập để thanh toán"));
        }

        try {
            String userId = authentication.getName(); // Lấy userId
            Order order = checkoutService.processCheckout(userId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}