package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Blog;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long>, JpaSpecificationExecutor<Blog> {
    // Lấy danh sách bài viết chưa bị xóa
    List<Blog> findByDeletedFalse();

    // Tìm bài viết theo slug và chưa bị xóa
    Optional<Blog> findBySlugAndDeletedFalse(String slug);

    // Lấy blog cho người dùng: Phải chưa xóa VÀ đã xuất bản
    List<Blog> findByDeletedFalseAndIsPublishedTrue();

    // Lấy blog cho chi tiết (người dùng): Phải đúng slug, chưa xóa VÀ đã xuất bản
    Optional<Blog> findBySlugAndDeletedFalseAndIsPublishedTrue(String slug);

	Page<Blog> findByContentPathAndDeletedFalse(String contentPath, Pageable pageable);

	Page<Blog> findByDeletedFalse(Pageable pageable);

	boolean existsBySlugAndDeletedFalse(String slug); // Dùng để check trùng slug

	Optional<Blog> findByIdAndDeletedFalse(Long id);


}