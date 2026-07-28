package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders") // Dùng tên orders vì Order là từ khóa SQL
public class Order extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId; // Người đặt hàng
    private Double totalAmount; // Tổng giá trị đơn hàng

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING; // Trạng thái đơn hàng

    private LocalDateTime orderDate = LocalDateTime.now();

    // Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Double getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(Double totalAmount) {
		this.totalAmount = totalAmount;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus success) {
		this.status = success;
	}

	public LocalDateTime getOrderDate() {
		return orderDate;
	}

	public void setOrderDate(LocalDateTime orderDate) {
		this.orderDate = orderDate;
	}

}

