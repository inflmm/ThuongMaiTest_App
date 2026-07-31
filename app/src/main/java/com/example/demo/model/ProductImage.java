package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_image")
public class ProductImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = true)
    private Product product;

    @Column(name = "object_path", nullable = false, length = 1000)
    private String objectPath;

    @Column(name = "public_url", length = 2000)
    private String publicUrl;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "variant", length = 50)
    private String variant;  // Supported values: large, medium, compact, thumbnail.
    // This field indicates which image size/variant this record represents for the product gallery.

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    public ProductImage() {
    }

    public ProductImage(Product product, String objectPath, String publicUrl, Integer displayOrder, String variant) {
        this.product = product;
        this.objectPath = objectPath;
        this.publicUrl = publicUrl;
        this.displayOrder = displayOrder;
        this.variant = variant;
        this.deleted = false;
    }

    public ProductImage(String objectPath, String publicUrl, Integer displayOrder, String variant) {
        this(null, objectPath, publicUrl, displayOrder, variant);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getObjectPath() {
        return objectPath;
    }

    public void setObjectPath(String objectPath) {
        this.objectPath = objectPath;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
