package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.CartItem;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.OrderStatus;
import com.example.demo.model.Product;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;

import jakarta.transaction.Transactional; // Quan trọng để đảm bảo tính toàn vẹn dữ liệu

@Service
public class CheckoutService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Transactional // Đảm bảo: Hoặc thanh toán hết, hoặc không gì cả (tránh mất dữ liệu)
    public Order processCheckout(String userId) {

        // 1. Lấy tất cả mục trong giỏ hàng chưa bị xóa của User
        List<CartItem> cartItems = cartRepository.findByUserIdAndDeletedFalse(userId);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng của bạn đang trống, không thể thanh toán.");
        }

        // 2. Tạo hóa đơn mới (Order)
        Order newOrder = new Order();
        newOrder.setUserId(userId);
        newOrder.setStatus(OrderStatus.PENDING); // Trạng thái chờ xử lý

        // Lưu trước để lấy ID của Order (phục vụ cho OrderItem)
        Order savedOrder = orderRepository.save(newOrder);

        double totalAmount = 0;

        // 3. Duyệt danh sách giỏ hàng để chuyển sang OrderItem
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct(); // Lấy trực tiếp từ quan hệ ManyToOne

            if (product == null) {
                continue; // Bỏ qua nếu sản phẩm không còn tồn tại
            }

            // Tạo mục chi tiết hóa đơn (OrderItem)
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setProduct(product);
            orderItem.setProductSlug(product.getSlug());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(cartItem.getQuantity());

            // QUAN TRỌNG: Lưu giá tại thời điểm mua (Snapshot Price)
            orderItem.setUnitPrice(product.getPrice());

            totalAmount += product.getPrice() * cartItem.getQuantity();

            orderItemRepository.save(orderItem);

            // 4. Đánh dấu mục giỏ hàng này đã thanh toán (Xóa khỏi giỏ)
            cartItem.setDeleted(true);
            cartRepository.save(cartItem);
        }

        // 5. Cập nhật tổng tiền cuối cùng và hoàn tất
        savedOrder.setTotalAmount(totalAmount);
        return orderRepository.save(savedOrder);
    }
}