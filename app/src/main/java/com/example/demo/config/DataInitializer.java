package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.demo.dto.UserRegistrationDTO;
import com.example.demo.model.Blog;
import com.example.demo.model.User;
import com.example.demo.repository.BlogRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuthService;


@Component
public class DataInitializer implements CommandLineRunner {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BlogRepository blogRepository;

	@Autowired
    private AuthService authService;

	@Override
	public void run(String... args) throws Exception {
		// 1. Khởi tạo tài khoản mẫu
        initializeSystemUsers();

		// Khởi tạo dữ liệu Blog mẫu
		if (blogRepository.count() == 0) {
			Blog sampleBlog = new Blog();
			sampleBlog.setTitle("Hướng dẫn tích hợp Table of Contents cho Website");
			sampleBlog.setSlug("huong-dan-tich-hop-toc");
			sampleBlog.setSummary(
					"Tìm hiểu cách tự động tạo mục lục từ nội dung bài viết bằng JavaScript và Spring Boot.");
			sampleBlog.setThumbnail("/assets/images/blog-thumb-1.jpg");
			sampleBlog.setContentPath("huong-dan-tich-hop-toc.html");
			sampleBlog.setPublished(true);

			blogRepository.save(sampleBlog);
			System.out.println("Đã tạo dữ liệu Blog mẫu thành công.");
		}
	}
	private void initializeSystemUsers() {
        // Tạo tài khoản Admin (user: admin, pass: 123456)
        if (!userRepository.existsByUsername("admin")) {
            UserRegistrationDTO adminDto = new UserRegistrationDTO();
            adminDto.setUsername("admin");
            adminDto.setPassword("123456");
            adminDto.setEmail("admin@example.com");
            adminDto.setFullName("System Administrator");

            User admin = authService.registerNewUser(adminDto); // Gọi service để mã hóa pass
            admin.setRole("ROLE_ADMIN"); // Cập nhật quyền Admin
            userRepository.save(admin);
            System.out.println("Đã khởi tạo tài khoản Admin thành công.");
        }

        // Tạo tài khoản User thường (user: 0123456, pass: 0123456)
        if (!userRepository.existsByUsername("0123456")) {
            UserRegistrationDTO userDto = new UserRegistrationDTO();
            userDto.setUsername("0123456");
            userDto.setPassword("0123456");
            userDto.setEmail("user@example.com");
            userDto.setFullName("Regular User");

            authService.registerNewUser(userDto); // Mặc định role sẽ là ROLE_USER
            System.out.println("Đã khởi tạo tài khoản User thành công.");
        }
    }

}