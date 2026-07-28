package com.example.demo.dto;

public class CartItemDto {
    private Long id; // ID của CartItem
    private Long productId;
    private String productSlug;
    private Integer quantity;

    // Thông tin chi tiết được lấy từ Product
    private String productName;
    private Double unitPrice;
    private String imageUrl;

    // Constructor, Getters và Setters
    public CartItemDto() {}

    public CartItemDto(Long id, String productSlug, Integer quantity, String productName, Double unitPrice, String imageUrl) {
        this.id = id;
        this.productSlug = productSlug;
        this.quantity = quantity;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.imageUrl = imageUrl;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public String getProductSlug() {
		return productSlug;
	}

	public void setProductSlug(String productSlug) {
		this.productSlug = productSlug;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public Double getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(Double unitPrice) {
		this.unitPrice = unitPrice;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

}