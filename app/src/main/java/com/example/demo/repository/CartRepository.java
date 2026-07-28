package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.CartItem;
import com.example.demo.model.Product;

@Repository
public interface CartRepository extends JpaRepository<CartItem, Long> {

    // Tìm giỏ hàng của một người dùng (userId) chưa bị xóa
    List<CartItem> findByUserIdAndDeletedFalse(String userId);

    // Tìm một mục giỏ hàng cụ thể (dùng để update quantity)
    // Ví dụ: Tìm CartItem của userId này cho productId này
    Optional<CartItem> findByUserIdAndProductSlugAndDeletedFalse(String userId, String productSlug);

    // Sử dụng SUM để cộng dồn tất cả quantity của các sản phẩm trong giỏ
    // COALESCE(..., 0) giúp trả về 0 nếu giỏ hàng trống (thay vì null)
    @Query("SELECT COALESCE(SUM(c.quantity), 0) FROM CartItem c " +
            "WHERE c.userId = :userId AND c.deleted = false")
     Integer countTotalItemsByUserId(@Param("userId") String userId);

	Optional<CartItem> findByUserIdAndProductIdAndDeletedFalse(String userId, Long productId);

	Optional<CartItem> findByIdAndUserId(Long cartItemId, String userId);

	Optional<CartItem> findByUserIdAndProductAndDeletedFalse(String userId, Product product);
}