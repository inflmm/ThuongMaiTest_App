package com.example.demo.dto;

import java.util.List;
import java.util.Map;

public class ProductDetailDto {
    private Long id;
    private String slug;
    private String name;
    private Double price;
    private String imageUrl; // Thumbnail
    private String image_folder_path;
    private String longDescription; // Nội dung bài viết
    private Map<String, String> specifications; // Key-Value các thông số
    private List<String> masterFiles;

    // Constructor, Getters, Setters...
    public ProductDetailDto() {}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}


	public String getImage_folder_path() {
		return image_folder_path;
	}

	public void setImage_folder_path(String image_folder_path) {
		this.image_folder_path = image_folder_path;
	}

	public String getLongDescription() {
		return longDescription;
	}

	public void setLongDescription(String longDescription) {
		this.longDescription = longDescription;
	}

	public Map<String, String> getSpecifications() {
		return specifications;
	}

	public void setSpecifications(Map<String, String> specifications) {
		this.specifications = specifications;
	}

	public List<String> getMasterFiles() {
		return masterFiles;
	}

	public void setMasterFiles(List<String> masterFiles) {
		this.masterFiles = masterFiles;
	}


}