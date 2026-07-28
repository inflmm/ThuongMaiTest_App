package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Spring Data JPA sẽ tự động tạo câu lệnh SQL cho phương thức này:
    // SELECT * FROM product WHERE deleted = false
    List<Product> findByDeletedFalse();

    // Tương tự, tìm theo ID nhưng chỉ khi chưa bị xóa:
    // SELECT * FROM product WHERE id = ? AND deleted = false
    Optional<Product> findBySlugAndDeletedFalse(String slug);

	Optional<Product> findBySlug(String slug);

	List<Product> findByIdInAndDeletedFalse(List<Long> ids);

	boolean existsBySlug(String productSlug);
}