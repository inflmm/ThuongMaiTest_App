package com.example.demo.model;

public enum OrderStatus {
    PENDING, // Chờ xử lý
    SUCCESS, // Thành công (đã thanh toán)
    SHIPPED, // Đã giao hàng
    CANCELLED // Đã hủy
}