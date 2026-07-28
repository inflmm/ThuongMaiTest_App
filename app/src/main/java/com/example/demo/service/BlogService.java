package com.example.demo.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.example.demo.model.Blog;
import com.example.demo.repository.BlogRepository;
import com.example.demo.repository.BlogSpecification;

import jakarta.transaction.Transactional;

@Service
public class BlogService {
    @Autowired
    private BlogRepository blogRepository;

    /**
     * DÀNH CHO NGƯỜI DÙNG: Chỉ lấy các bài viết đã xuất bản và chưa xóa
     */
    public Optional<List<Blog>> getAllPublicBlogs() {
        List<Blog> blogs = blogRepository.findByDeletedFalseAndIsPublishedTrue();
        return blogs.isEmpty() ? Optional.empty() : Optional.of(blogs);
    }

    /**
     * DÀNH CHO ADMIN: Lấy tất cả bài viết chưa xóa (để quản lý ẩn/hiện)
     */
    public List<Blog> getAllBlogsForAdmin() {
        return blogRepository.findByDeletedFalse();
    }

    public Optional<Blog> getBlogByIdForAdmin(Long id) {
        return blogRepository.findByIdAndDeletedFalse(id)
                .map(this::loadContentFromFile); // Hàm bổ trợ đọc file bạn đã viết
    }

    /**
     * CHI TIẾT BÀI VIẾT (Người dùng)
     */
    public Optional<Blog> getPublicBlogBySlug(String slug) {
        return blogRepository.findBySlugAndDeletedFalseAndIsPublishedTrue(slug)
                .map(this::loadContentFromFile); // Hàm bổ trợ đọc file bạn đã viết
    }

    // Hàm bổ trợ đọc file content (giữ nguyên logic cũ của bạn)
    private Blog loadContentFromFile(Blog blog) {
    	String subFolder = (blog.getContentPath() == null) ? "" : blog.getContentPath();
        String folderPath = "C:/ecommerce-uploads/articles/" + subFolder;
        String fileName = blog.getSlug() + ".html";
        String filePath = folderPath + fileName;
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                blog.setContent(Files.readString(path, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            blog.setContent("<p>Lỗi đọc nội dung bài viết.</p>");
        }
        return blog;
    }

    public Blog createNewBlog(Blog blog, String rawContent) throws IOException {
        // 1. Kiểm tra thư mục (contentPath) đã tồn tại chưa
        String subFolder = (blog.getContentPath() == null) ? "" : blog.getContentPath();
        Path folderPath = Paths.get("C:/ecommerce-uploads/articles/" + subFolder);

        if (!Files.exists(folderPath)) {
            throw new IOException("Thư mục lưu trữ không tồn tại. Vui lòng tạo thư mục trước.");
        }
        if (blog.getSummary() == null || blog.getSummary().isBlank()) {
            blog.setSummary("Mô tả bài viết");
        }

        // 2. Tạo tên file dựa trên slug: slug.html
        String fileName = blog.getSlug() + ".html";
        Path filePath = folderPath.resolve(fileName);

        // 3. Ghi nội dung vào file vật lý
        Files.writeString(filePath, rawContent, StandardCharsets.UTF_8);

        // 4. Lưu thông tin vào DB (lúc này contentPath chỉ lưu phần thư mục)
        blog.setDeleted(false);
        blog.setPublished(false); // Mặc định ẩn
        return blogRepository.save(blog);
    }
    /**
     * CẬP NHẬT BLOG: Cho phép đổi slug và rename file tương ứng
     */
    @Transactional
    public Blog updateBlog(Blog updatedData, String newRawContent) throws IOException {
        // 1. Kiểm tra ID và lấy bài viết cũ từ DB
        Long id = updatedData.getId();
        Blog existingBlog = blogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết có ID: " + id));

        String oldSlug = existingBlog.getSlug();
        String newSlug = updatedData.getSlug();

        // Logic xử lý thư mục và file
        String subFolder = (updatedData.getContentPath() != null) ? updatedData.getContentPath() : existingBlog.getContentPath();
        if (subFolder != null && !subFolder.endsWith("/")) {
			subFolder += "/";
		}

        Path baseDir = Paths.get("C:/ecommerce-uploads/articles/", subFolder);

        // 2. Logic đổi tên file nếu đổi Slug
        if (!oldSlug.equals(newSlug)) {
            Path oldFile = baseDir.resolve(oldSlug + ".html");
            Path newFile = baseDir.resolve(newSlug + ".html");
            if (Files.exists(oldFile)) {
                Files.move(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        // 3. Ghi nội dung mới
        Path targetFile = baseDir.resolve(newSlug + ".html");
        Files.writeString(targetFile, newRawContent, StandardCharsets.UTF_8);

        // 4. Cập nhật dữ liệu từ updatedData vào existingBlog
        existingBlog.setTitle(updatedData.getTitle());
        existingBlog.setSlug(newSlug);
        existingBlog.setSummary(updatedData.getSummary());
        existingBlog.setThumbnail(updatedData.getThumbnail());
        existingBlog.setContentPath(subFolder);

        if (updatedData.getPublishTime() != null) {
            existingBlog.setPublishTime(updatedData.getPublishTime());
        }

        return blogRepository.save(existingBlog);
    }
    /**
     * Hàm lưu bài viết (dùng cho trang admin sau này)
     */
    public Blog saveBlog(Blog blog) {
        return blogRepository.save(blog);
    }

    /**
     * Xóa mềm bài viết
     */
    public void deleteBlog(Long id) {
        blogRepository.findById(id).ifPresent(blog -> {
            blog.setDeleted(true);
            blogRepository.save(blog);
        });
    }
    /**
     * Tìm kiếm bài viết
     */
    public Page<Blog> searchBlogs(Long id, String title, LocalDate from, LocalDate to, String contentPath, Pageable pageable) {
        Specification<Blog> spec = BlogSpecification.filterBlogs(id, title, from, to, contentPath);
        return blogRepository.findAll(spec, pageable);
    }
}