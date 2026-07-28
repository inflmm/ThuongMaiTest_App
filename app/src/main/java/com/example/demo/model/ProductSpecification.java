package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ProductSpecification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String specKey;   // VD: CPU
    private String specValue; // VD: i5-14400F

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSpecKey() {
		return specKey;
	}

	public void setSpecKey(String specKey) {
		this.specKey = specKey;
	}

	public String getSpecValue() {
		return specValue;
	}

	public void setSpecValue(String specValue) {
		this.specValue = specValue;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}


}