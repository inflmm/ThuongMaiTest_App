package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	    http
	        .csrf(csrf -> csrf.disable())
	        .authorizeHttpRequests(auth -> auth
	                // 1. Cho phép tất cả mọi người truy cập các file tĩnh (CSS, JS, Images)
	                .requestMatchers("/css/**", "/js/**", "/assets/**", "/favicon.ico").permitAll()
	                .requestMatchers("/assets/css/admin/**", "/assets/js/admin/**").permitAll()

	                // 2. Cho phép xem Trang chủ và các trang hiển thị sản phẩm
	                .requestMatchers("/", "/homepage", "/collections", "/collections/**", "/products/**", "/blogs/**", "/admin/login").permitAll()

	                // 3. Mở khóa toàn bộ API lấy dữ liệu sản phẩm và Giỏ hàng tạm thời
	                .requestMatchers("/api/products/**").permitAll()
	                .requestMatchers("/api/cart/**").permitAll() // Khách chưa login vẫn thêm được vào guest_001

	                // 4. Mở khóa API Auth (Đăng ký/Đăng nhập/Đăng xuất)
	                .requestMatchers("/api/auth/**").permitAll()

	                // 5. CHẶN: Chỉ những trang này mới cần Đăng nhập
	                .requestMatchers("/checkout/**", "/order/**", "/profile/**").authenticated()
	                .requestMatchers("/api/cart/merge").authenticated() // Chỉ user đã login mới được gọi merge
	                .requestMatchers("/admin/**").hasRole("ADMIN") // Bảo vệ trang HTML Admin
	                .requestMatchers("/api/admin/**").hasRole("ADMIN") // Bảo vệ các API xử lý dữ liệu của Admin

	                .anyRequest().permitAll() // Các yêu cầu khác cho phép hết để tránh lỗi 403 phát sinh
	            )
	        .exceptionHandling(exception -> exception
	        	    // Nếu vào /admin mà chưa login, đá về trang login của admin thay vì homepage của khách
	        	    .authenticationEntryPoint((request, response, authException) -> {
	        	        if (request.getRequestURI().startsWith("/admin")) {
	        	            response.sendRedirect("/admin/login");
	        	        } else {
	        	            response.sendRedirect("/homepage");
	        	        }
	        	    })
	        	)
	        .formLogin(login -> login
	            .loginPage("/homepage") // Nếu chưa auth mà vào trang cấm, sẽ về đây
	            .loginProcessingUrl("/api/auth/login") // URL mà JS sẽ fetch tới
	            .successHandler((request, response, authentication) -> {
	                response.setStatus(200);
	                response.setContentType("application/json;charset=UTF-8");

	                // Lấy danh sách quyền (Roles) của user vừa đăng nhập thành công
	                String role = authentication.getAuthorities().stream()
	                        .map(r -> r.getAuthority())
	                        .findFirst()
	                        .orElse("ROLE_USER");

	                // Trả về JSON chứa message và role
	                String jsonResponse = String.format("{\"message\": \"Login Success\", \"role\": \"%s\"}", role);
	                response.getWriter().write(jsonResponse);
	            })
	            .failureHandler((request, response, exception) -> {
	                response.setStatus(401);
	                response.getWriter().write("{\"message\": \"Login Failed\"}");
	            })
	        )
	        .logout(logout -> logout
	            .logoutUrl("/api/auth/logout")
	            .logoutSuccessHandler((request, response, authentication) -> {
	                response.setStatus(200);
	                response.getWriter().write("{\"message\": \"Logout Success\"}");
	            })
	            .invalidateHttpSession(true)
	            .deleteCookies("JSESSIONID")
	        );
	    return http.build();
	}

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
