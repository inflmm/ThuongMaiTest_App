package com.example.demo.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.CartItemDto;
import com.example.demo.model.CartItem;
import com.example.demo.model.Product;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductRepository;

import jakarta.transaction.Transactional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * MỤC ĐÍCH: Chuyển đổi từ Entity sang DTO để gửi về Front-end.
     * GIẢI THÍCH: Giúp tên sản phẩm luôn cập nhật mới nhất từ bảng Product.
     */
    public CartItemDto convertToDto(CartItem item) {
        CartItemDto dto = new CartItemDto();
        dto.setId(item.getId());
        dto.setQuantity(item.getQuantity());

        Product p = item.getProduct();
        if (p != null) {
            // "Nhặt" thông tin từ object Product gắn kèm
            dto.setProductId(p.getId()); // Hoặc p.getId() tùy model của bạn
            dto.setProductName(p.getName());
            dto.setUnitPrice(p.getPrice());
            dto.setImageUrl(p.getImageUrl());
            dto.setProductSlug(p.getSlug());
        } else {
            dto.setProductName("Sản phẩm không còn tồn tại");
            dto.setUnitPrice(0.0);
        }
        return dto;
    }

    /**
     * MỤC ĐÍCH: Lấy danh sách giỏ hàng để hiển thị lên giao diện.
     */
    public List<CartItemDto> getCartItemsDto(String userId) {
        List<CartItem> items = cartRepository.findByUserIdAndDeletedFalse(userId);
        return items.stream()
                .map(this::convertToDto) // Sử dụng hàm convert đã viết ở trên
                .collect(Collectors.toList());
    }

    /**
     * MỤC ĐÍCH: Thêm sản phẩm vào giỏ (hoặc cộng dồn nếu đã có).
     */
    @Transactional
    public CartItem addToCart(String identifier, Integer quantity, String userId) {
        Product product;
        // 1. Kiểm tra sản phẩm có tồn tại không
    	if (identifier.matches("-?\\d+")) {
            // Nếu là số, tìm theo ID
            product = productRepository.findById(Long.parseLong(identifier))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ID sản phẩm: " + identifier));
        } else {
            // Nếu là chữ, tìm theo Slug
            product = productRepository.findBySlug(identifier)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Slug sản phẩm: " + identifier));
        }

        // 2. Tìm xem trong giỏ hàng của User đã có sản phẩm này chưa
        Optional<CartItem> existingItem = cartRepository.findByUserIdAndProductAndDeletedFalse(userId, product);

        if (existingItem.isPresent()) {
            // Nếu có rồi thì cộng dồn số lượng
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            return cartRepository.save(item);
        } else {
            // Nếu chưa có thì tạo mới
            CartItem newItem = new CartItem();
            newItem.setProduct(product); // Gán nguyên Object Product vào
            newItem.setQuantity(quantity);
            newItem.setUserId(userId);
            newItem.setDeleted(false);
            return cartRepository.save(newItem);
        }
    }

    /**
     * MỤC ĐÍCH: Cập nhật số lượng tại trang Giỏ hàng.
     * XỬ LÝ: Nếu số lượng <= 0 thì tự động xóa khỏi giỏ.
     */
    @Transactional
    public CartItem updateQuantity(Long cartItemId, Integer newQuantity, String userId) {
        CartItem item = cartRepository.findByIdAndUserId(cartItemId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mục giỏ hàng này!"));

        if (newQuantity <= 0) {
            item.setDeleted(true); // Coi như xóa nếu số lượng về 0
        } else {
            item.setQuantity(newQuantity);
        }

        return cartRepository.save(item);
    }

    /**
     * MỤC ĐÍCH: Xóa hẳn một mục khỏi giỏ (Soft delete).
     * @return
     */
    @Transactional
    public boolean removeFromCart(Long cartItemId, String userId) {
        Optional<CartItem> itemOpt = cartRepository.findByIdAndUserId(cartItemId, userId);
        if (itemOpt.isPresent()) {
            CartItem item = itemOpt.get();
            item.setDeleted(true); // Soft delete
            cartRepository.save(item);
            return true;
        }
        return false; // Không tìm thấy hoặc không thuộc user này
    }

    /**
     * MỤC ĐÍCH: Lấy tổng số lượng item để hiện trên Header.
     */
    public Integer getTotalItemCount(String userId) {
        Integer count = cartRepository.countTotalItemsByUserId(userId);
        return (count != null) ? count : 0;
    }

    @Transactional
    public void silentMerge(List<CartItemDto> guestItems, String userId) {
        // 1. Kiểm tra giỏ hàng hiện tại trong DB theo String userId
        List<CartItem> currentDbCart = cartRepository.findByUserIdAndDeletedFalse(userId);

        if (currentDbCart.isEmpty() && guestItems != null && !guestItems.isEmpty()) {
            try {
                for (CartItemDto item : guestItems) {
                    Product product = productRepository.findById(item.getProductId()).orElse(null);
                    if (product != null) {
                        CartItem newItem = new CartItem();
                        newItem.setUserId(userId); // Lưu chuỗi định danh user
                        newItem.setProduct(product);
                        newItem.setQuantity(item.getQuantity());
                        newItem.setDeleted(false);

                        cartRepository.save(newItem);
                    }
                }
                System.out.println("Backend: Merge thành công cho User: " + userId);
            } catch (Exception e) {
                System.err.println("Backend Error: " + e.getMessage());
            }
        }
    }
}