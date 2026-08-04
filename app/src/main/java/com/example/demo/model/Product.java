package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity // Đánh dấu đây là một Entity (ánh xạ tới bảng trong DB)
public class Product extends BaseEntity {

    @Id // Đánh dấu là khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Cấu hình ID tự tăng
    private Long id;

    @Column(unique = true) // Đảm bảo slug không trùng lặp
    private String slug;

    private String name;
    private Double price;
    private String sku;
    private Integer stockQuantity = 0;
    private Boolean available = true;
    private Boolean visible = true;
    private String shortDescription;
    // Ảnh chính
    private String imageUrl;
    private String image_folder_path;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(length = 1000)
    private String categoryPathIds;

    @Column(length = 1000)
    private String categoryPathSlugs;

    @Lob // Để lưu nội dung dài
    @JsonIgnore	// Dùng JsonIgnore để bỏ qua khi tạo file Json, để lấy thuộc tính này cần lấy thông qua dto hoặc hàm có mapping khác
    @Column(columnDefinition = "text")
    private String longDescription;


    // Thông số kỹ thuật động (EAV)
    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSpecification> specifications = new ArrayList<>();

    // Link Many-to-Many với Collection
    @JsonIgnore  // JsonIgnore ở đây không chỉ để bỏ qua không cần thiết mà cũng tránh gây vòng lặp dẫn đến stack overflow
    @ManyToMany(mappedBy = "products")
    private List<Collection> collections = new ArrayList<>();

    // Danh sách ảnh sản phẩm theo entity chuẩn.
    // Dùng cho admin/product flow mới, trong khi image_folder_path vẫn giữ vai trò
    // cho việc tìm kiếm và duyệt ảnh theo folder hiện có.
    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    private Boolean deleted = false;

    // Constructors
    public Product() {}

    // Constructor này dùng để tạo nhanh Product kèm theo các thông tin cơ bản
    public Product(String name, Double price, String imageUrl, String longDescription) {
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.longDescription = longDescription;
        this.deleted = false;
    }

    // Getters and Setters
    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

    public String getSlug() { return slug; }
	public void setSlug(String slug) { this.slug = slug; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getCategoryPathIds() {
        return categoryPathIds;
    }

    public void setCategoryPathIds(String categoryPathIds) {
        this.categoryPathIds = categoryPathIds;
    }

    public String getCategoryPathSlugs() {
        return categoryPathSlugs;
    }

    public void setCategoryPathSlugs(String categoryPathSlugs) {
        this.categoryPathSlugs = categoryPathSlugs;
    }

	public String getImage_folder_path() {
		return image_folder_path;
	}

	public void setImage_folder_path(String image_folder_path) {
		this.image_folder_path = image_folder_path;
	}

	public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }

	public String getLongDescription() {
		return longDescription;
	}

	public void setLongDescription(String longDescription) {
		this.longDescription = longDescription;
	}

	public List<ProductSpecification> getSpecifications() {
		return specifications;
	}

	public void setSpecifications(List<ProductSpecification> specifications) {
		this.specifications = specifications;
	}

	public List<Collection> getCollections() {
		return collections;
	}

	public void setCollections(List<Collection> collections) {
		this.collections = collections;
	}

	public List<ProductImage> getImages() {
		return images;
	}

	public void setImages(List<ProductImage> images) {
		this.images = images;
	}

}