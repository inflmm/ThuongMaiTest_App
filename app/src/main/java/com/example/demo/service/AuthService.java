package com.example.demo.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.UserRegistrationDTO;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String generateUniqueUserId() {
        String newId;
        boolean isDuplicate;
        do {
            // Cấu trúc: USER_ + NămThángNgày + 5 ký tự ngẫu nhiên
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String randomPart = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            newId = "USER_" + datePart + "_" + randomPart;

            isDuplicate = userRepository.existsByUserId(newId);
        } while (isDuplicate);
        return newId;
    }

    @Transactional
    public User registerNewUser(UserRegistrationDTO dto) {
        // 1. Tạo đối tượng Entity User mới
        User user = new User();

        // 2. Gán Business ID duy nhất (USER_2026...)
        user.setUserId(generateUniqueUserId());

        // 3. Gán thông tin cơ bản từ DTO sang Entity
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setFullName(dto.getFullName());

        // 4. MÃ HÓA MẬT KHẨU: Tuyệt đối không lưu mật khẩu dạng thô (plain text)
        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        user.setPassword(encodedPassword);

        // 5. Gán vai trò mặc định (Nếu User.java đã để mặc định là ROLE_USER thì có thể bỏ qua)
        user.setRole("ROLE_USER");

        // 6. Lưu vào Database
        return userRepository.save(user);
    }

    /**
     * Self-service password change — used by both the regular-user endpoint
     * (AuthController) and the employee endpoint (AdminAccountController).
     * username is always resolved from the authenticated JWT principal by
     * the calling controller, never trusted from a request body.
     */
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUserId(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản: " + username));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadCredentialsException("Mật khẩu hiện tại không đúng");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 8 ký tự");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}