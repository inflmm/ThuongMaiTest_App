package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CartItemDto;
import com.example.demo.service.CartService;

@RestController
@RequestMapping("/api/cart") // Đường dẫn gốc cho giỏ hàng
@CrossOrigin(origins = "*")
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * API 1: Lấy danh sách giỏ hàng (DTO)
     */
    @GetMapping("/my-cart")
    public ResponseEntity<List<CartItemDto>> getCartContents(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Lấy định danh người dùng từ Spring Security
        String userId = authentication.getName();
        System.out.println("Đang lấy giỏ hàng cho ID: " + userId);
        return ResponseEntity.ok(cartService.getCartItemsDto(userId));
    }

    /**
     * API 2: Xóa mục khỏi giỏ hàng
     */
    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<?> removeItemFromCart(@PathVariable Long cartItemId, Authentication authentication) {
    	if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
        	String userId = authentication.getName();
            boolean success = cartService.removeFromCart(cartItemId, userId);

            if (success) {
                return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm khỏi giỏ hàng."));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Không tìm thấy mục giỏ hàng hoặc bạn không có quyền xóa."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    /**
     * API 3: Cập nhật số lượng
     */
    @PutMapping("/update/{cartItemId}")
    public ResponseEntity<?> updateQuantity(@PathVariable Long cartItemId, @RequestBody Map<String, Integer> payload, Authentication authentication) {
        Integer newQuantity = payload.get("quantity");
        if (newQuantity == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu số lượng cần cập nhật."));
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
        	String userId = authentication.getName();
            // Giả sử hàm updateQuantity trả về đối tượng đã cập nhật
            cartService.updateQuantity(cartItemId, newQuantity, userId);
            return ResponseEntity.ok(Map.of("message", "Cập nhật số lượng thành công."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * API 4: Lấy số lượng tổng (cho Badge trên Header)
     */
    @GetMapping("/count")
    public ResponseEntity<Integer> getCartCount(Authentication authentication) {
    	if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    	String userId = authentication.getName();
        return ResponseEntity.ok(cartService.getTotalItemCount(userId));
    }

    /**
     * API: Thêm sản phẩm vào giỏ hàng
     * Khớp với fetch(CART_ADD_URL, ...) trong product-detail.js
     */
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> payload, Authentication authentication) {
    	if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
        	String userId = authentication.getName();
            // Lấy dữ liệu từ JSON body
        	String identifier = String.valueOf(payload.get("productId")); // Ép kiểu về String để xử lý
            Integer quantity = (Integer) payload.get("quantity");

            if (identifier == null || quantity == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Thiếu thông tin sản phẩm hoặc số lượng"));
            }

            // Gọi service xử lý (Hàm này chúng ta đã viết ở bước trước)
            cartService.addToCart(identifier, quantity, userId);

            return ResponseEntity.ok(Map.of("message", "Thêm vào giỏ hàng thành công!"));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/merge")
    public ResponseEntity<?> silentMerge(@RequestBody List<CartItemDto> guestItems, Authentication authentication){
    	if (authentication == null || !authentication.isAuthenticated()) {
    		return ResponseEntity.status(401).body("Chưa đăng nhập, không thực hiện merge cart");
    	}

    	String userId = authentication.getName();

    	cartService.silentMerge(guestItems, userId);

    	return ResponseEntity.ok().build();
    }
}