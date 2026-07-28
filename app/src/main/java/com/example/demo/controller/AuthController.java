package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.UserRegistrationDTO;
import com.example.demo.model.User;
import com.example.demo.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegistrationDTO dto) {
        try {
            User savedUser = authService.registerNewUser(dto);
            return ResponseEntity.ok("Đăng ký thành công với ID: " + savedUser.getUserId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi đăng ký: " + e.getMessage());
        }
    }

}