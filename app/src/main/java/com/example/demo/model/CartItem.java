package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "productId")
    private Product product; // Hibernate sẽ tự hiểu là dùng product_id để link
    private Integer quantity;
    private String userId;

    private Boolean deleted = false;

    // Constructors
    public CartItem() {}



    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }

}