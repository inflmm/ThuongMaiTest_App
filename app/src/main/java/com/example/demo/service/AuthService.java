package com.example.demo.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.UserRegistrationDTO;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

/**
 * Service for user registration and password management.
 * Handles generating unique user IDs, registering new users, and changing passwords.
 * Note: username here refers to the unique identifier (userId) generated at registration, not the email or phone number.
 */
@Service
public class AuthService {

    private final Clock clock;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public String generateUniqueUserId() {
        String newId;
        boolean isDuplicate;
        do {
            // Cấu trúc: USER_ + NămThángNgày + 5 ký tự ngẫu nhiên
            String datePart = LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
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
     *
     * The parameter is named userId, not username, deliberately —
     * CustomUserDetailsService makes userId (the stable business ID) the
     * Spring Security principal, so authentication.getName() always returns
     * userId, never the login identifier (email/phone) or the `username`
     * column. Every caller passes authentication.getName() in here, so this
     * must look up by userId to match — looking up by the `username` column
     * instead was the actual bug (it happened to work in login() only
     * because that method already used findByUserId correctly).
     */
    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản: " + userId));
 
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