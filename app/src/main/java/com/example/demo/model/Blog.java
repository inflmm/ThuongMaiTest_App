package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 *
 */
@Entity
@Table(name = "blogs")
public class Blog extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(nullable = false)
    private String thumbnail = "/images/blog-thumb-1.jpg"; // Đường dẫn ảnh đại diện

    @Column(columnDefinition = "TEXT")
    private String summary; // Tóm tắt ngắn hiện ở trang danh sách

	 // Lưu tên file: "bai-viet-so-1.html"
	private String contentPath;
	// Trường này không lưu xuống DB, chỉ dùng để giữ nội dung khi đọc từ file lên để hiển thị ra View
    @Transient
    private String content;

    @Column(nullable = false)
    private boolean isPublished = false; // Mặc định là ẩn (false) khi tạo mới

    private LocalDateTime publishTime;

    private Boolean isFeatured = false; // Đánh dấu bài viết nổi bật

    private Boolean deleted = false;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

	public String getContentPath() {
		return contentPath;
	}
	public void setContentPath(String contentPath) {
		this.contentPath = contentPath;
	}

	public boolean isPublished() {
		return isPublished;
	}
	public void setPublished(boolean isPublished) {
		this.isPublished = isPublished;
	}

	public Boolean getIsFeatured() {
		return isFeatured;
	}
	public void setIsFeatured(Boolean isFeatured) {
		this.isFeatured = isFeatured;
	}
	public LocalDateTime getPublishTime() {
		return publishTime;
	}
	public void setPublishTime(LocalDateTime publishTime) {
		this.publishTime = publishTime;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}
}