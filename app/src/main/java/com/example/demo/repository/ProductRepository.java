package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByDeletedFalse();

    Optional<Product> findBySlugAndDeletedFalse(String slug);

    Optional<Product> findBySlug(String slug);

    List<Product> findByIdInAndDeletedFalse(List<Long> ids);

    List<Product> findByDeletedFalseOrderByUpdatedTimeDesc();

    List<Product> findByDeletedFalseAndVisibleTrueOrderByUpdatedTimeDesc();

    boolean existsBySlug(String productSlug);

    boolean existsByCategoryIdAndDeletedFalse(Long categoryId);

    List<Product> findAllByDeletedFalseOrderByUpdatedTimeDesc();

    Optional<Product> findByIdAndDeletedFalse(Long id);
    // Bộ lọc sản phẩm theo giá
    List<Product> findByPriceLessThanEqualAndDeletedFalseAndVisibleTrue(Double maxPrice);
}