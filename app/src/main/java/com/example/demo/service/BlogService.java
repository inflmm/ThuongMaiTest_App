package com.example.demo.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import com.example.demo.service.SupabaseStorageService.StorageRoot;

import jakarta.transaction.Transactional;

@Service
public class BlogService {
    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    public Optional<List<Blog>> getAllPublicBlogs() {
        List<Blog> blogs = blogRepository.findByDeletedFalseAndIsPublishedTrue();
        return blogs.isEmpty() ? Optional.empty() : Optional.of(blogs);
    }

    public List<Blog> getAllBlogsForAdmin() {
        return blogRepository.findByDeletedFalse();
    }

    public Optional<Blog> getBlogByIdForAdmin(Long id) {
        return blogRepository.findByIdAndDeletedFalse(id)
                .map(this::loadContentFromStorage);
    }

    public Optional<Blog> getPublicBlogBySlug(String slug) {
        return blogRepository.findBySlugAndDeletedFalseAndIsPublishedTrue(slug)
                .map(this::loadContentFromStorage);
    }

    private Blog loadContentFromStorage(Blog blog) {
        String subFolder = blog.getContentPath() == null ? "" : blog.getContentPath().trim();
        String articlePath = buildArticleObjectPath(subFolder, blog.getSlug());
        try {
            String htmlContent = supabaseStorageService.downloadFile(StorageRoot.ARTICLES, articlePath);
            blog.setContent(htmlContent);
        } catch (IOException e) {
            blog.setContent("<p>Lỗi đọc nội dung bài viết.</p>");
        }
        return blog;
    }

    public Blog createNewBlog(Blog blog, String rawContent) throws IOException {
        String subFolder = blog.getContentPath() == null ? "" : blog.getContentPath().trim();
        if (!subFolder.isEmpty()) {
            supabaseStorageService.createFolder(StorageRoot.ARTICLES, subFolder);
        }

        if (blog.getSummary() == null || blog.getSummary().isBlank()) {
            blog.setSummary("Mô tả bài viết");
        }

        String articlePath = buildArticleObjectPath(subFolder, blog.getSlug());
        supabaseStorageService.uploadBytes(StorageRoot.ARTICLES, articlePath, rawContent.getBytes(StandardCharsets.UTF_8), "text/html; charset=UTF-8");

        blog.setDeleted(false);
        blog.setPublished(false);
        return blogRepository.save(blog);
    }

    @Transactional
    public Blog updateBlog(Blog updatedData, String newRawContent) throws IOException {
        Long id = updatedData.getId();
        Blog existingBlog = blogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết có ID: " + id));

        String oldSubFolder = existingBlog.getContentPath() == null ? "" : existingBlog.getContentPath().trim();
        String oldArticlePath = buildArticleObjectPath(oldSubFolder, existingBlog.getSlug());

        String newSubFolder = updatedData.getContentPath() == null ? "" : updatedData.getContentPath().trim();
        if (!newSubFolder.isEmpty()) {
            supabaseStorageService.createFolder(StorageRoot.ARTICLES, newSubFolder);
        }

        String newArticlePath = buildArticleObjectPath(newSubFolder, updatedData.getSlug());
        supabaseStorageService.uploadBytes(StorageRoot.ARTICLES, newArticlePath, newRawContent.getBytes(StandardCharsets.UTF_8), "text/html; charset=UTF-8");

        if (!oldArticlePath.equals(newArticlePath)) {
            try {
                supabaseStorageService.deleteObject(StorageRoot.ARTICLES, oldArticlePath);
            } catch (IOException ignored) {
            }
        }

        existingBlog.setTitle(updatedData.getTitle());
        existingBlog.setSlug(updatedData.getSlug());
        existingBlog.setSummary(updatedData.getSummary());
        existingBlog.setThumbnail(updatedData.getThumbnail());
        existingBlog.setContentPath(newSubFolder);

        if (updatedData.getPublishTime() != null) {
            existingBlog.setPublishTime(updatedData.getPublishTime());
        }

        return blogRepository.save(existingBlog);
    }

    public Blog saveBlog(Blog blog) {
        return blogRepository.save(blog);
    }

    public void deleteBlog(Long id) {
        blogRepository.findById(id).ifPresent(blog -> {
            blog.setDeleted(true);
            blogRepository.save(blog);
        });
    }

    public Page<Blog> searchBlogs(Long id, String title, LocalDate from, LocalDate to, String contentPath, Pageable pageable) {
        Specification<Blog> spec = BlogSpecification.filterBlogs(id, title, from, to, contentPath);
        return blogRepository.findAll(spec, pageable);
    }

    private String buildArticleObjectPath(String contentPath, String slug) {
        if (contentPath == null || contentPath.isBlank()) {
            return slug + ".html";
        }
        String normalized = contentPath.replaceAll("^/|/$", "");
        return normalized + "/" + slug + ".html";
    }
}
