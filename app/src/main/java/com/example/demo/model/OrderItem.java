package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId; // Liên kết với Order
    @ManyToOne
    @JoinColumn(name = "productId") // Khớp chính xác với tên cột bạn muốn trong DB
    private Product product;
    private String productSlug;
    private String productName; // Lưu tên sản phẩm tại thời điểm đặt hàng (tránh giá bị thay đổi)
    private Integer quantity;
    private Double unitPrice; // Lưu giá sản phẩm tại thời điểm đặt hàng

    // Getters and Setters
    public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getOrderId() {
		return orderId;
	}
	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	public Product getProduct() {
		return product;
	}
	public void setProduct(Product product) {
		this.product = product;
	}
	public String getProductSlug() {
		return productSlug;
	}
	public void setProductSlug(String productSlug) {
		this.productSlug = productSlug;
	}
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public Integer getQuantity() {
		return quantity;
	}
	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
	public Double getUnitPrice() {
		return unitPrice;
	}
	public void setUnitPrice(Double unitPrice) {
		this.unitPrice = unitPrice;
	}
}