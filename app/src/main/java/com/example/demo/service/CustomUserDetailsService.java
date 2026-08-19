package com.example.demo.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //Service đăng nhập
    @Override
    public UserDetails loadUserByUsername(String loginInput) throws UsernameNotFoundException {
        // Tìm user bằng username (là email hoặc SĐT từ form login)
        User user = userRepository.findByUsernameOrEmailOrPhoneNumber(loginInput, loginInput, loginInput)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + loginInput));

        // QUAN TRỌNG: Trả về userId thay vì username
        // Khi đó, authentication.getName() ở các Controller sẽ trả về "USER_2026..."
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserId()) // <--- Đưa userId vào đây
                .password(user.getPassword())
                .authorities(user.getRole())
                .build();
    }
}