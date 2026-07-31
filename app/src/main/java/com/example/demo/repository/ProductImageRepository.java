package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Product;
import com.example.demo.model.ProductImage;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductAndDeletedFalseOrderByDisplayOrderAsc(Product product);

    Optional<ProductImage> findFirstByProductAndDeletedFalseOrderByDisplayOrderAsc(Product product);
}
