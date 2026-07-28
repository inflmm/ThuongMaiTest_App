package com.example.demo.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.UserRegistrationDTO;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Nếu bạn có dùng mã hóa mật khẩu

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
}
