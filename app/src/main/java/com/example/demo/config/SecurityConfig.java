package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import com.example.demo.security.CsrfCookieFilter;
import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.JwtService;

/**
 * Stateless JWT auth. Two things worth knowing before touching this file:
 *
 * 1. The JWT is carried in an httpOnly cookie, not an Authorization header.
 *    This app serves /admin/** as full server-rendered Thymeleaf pages
 *    (see WebController) — a plain browser page navigation has no way to
 *    attach a custom header, only a cookie rides along automatically.
 * 2. Because auth now lives in a cookie, CSRF protection is required (a
 *    cookie is auto-attached to ANY request to this domain, including ones
 *    a malicious page triggers) — hence CookieCsrfTokenRepository below,
 *    which works without server-side sessions.
 *
 * ADMIN vs EMPLOYEE: this class only enforces the coarse "must be at least
 * EMPLOYEE to reach /admin/** or /api/admin/** at all" check via
 * hasAnyRole(...). Endpoints that must stay ADMIN-exclusive are scattered
 * across controllers rather than under a clean sub-path, so those are
 * enforced individually with @PreAuthorize("hasRole('ADMIN')") on the
 * specific controller methods instead — see the example in AuthController's
 * companion notes. @EnableMethodSecurity below turns that on.
 *
 * Note: this deliberately does NOT rely on a RoleHierarchy bean (e.g.
 * "ROLE_ADMIN > ROLE_EMPLOYEE") applying automatically to hasRole/hasAnyRole
 * checks — making that apply to authorizeHttpRequests and @PreAuthorize both
 * requires explicitly wiring it into the relevant expression handlers, and
 * getting that wiring subtly wrong would silently misroute access. Writing
 * hasAnyRole("ADMIN", "EMPLOYEE") explicitly is a few extra characters and
 * is guaranteed correct without extra wiring. This also means hasRole
 * checks never accidentally widen — AdminAccountController relies on this:
 * hasRole("EMPLOYEE") there genuinely excludes ADMIN accounts, with no
 * hierarchy silently letting them through.
 *
 * /api/auth/change-password is a deliberate carve-out from the /api/auth/**
 * wildcard: register/login/logout are genuinely public and CSRF-exempt
 * (there's no session/token yet to attach a CSRF token to), but changing a
 * password is a real authenticated write and must NOT inherit that
 * exemption. Order matters below — the specific rule/exclusion must be
 * declared before the broader /api/auth/** one, since authorizeHttpRequests
 * and ignoringRequestMatchers both use first-match-wins evaluation.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
	    http
	        .csrf(csrf -> csrf
	                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
	                // Default handler (XorCsrfTokenRequestAttributeHandler) BREACH-protects
	                // the token by XOR-encoding it for HTML forms — but nothing here ever
	                // resolves/renders that request attribute, and the JS clients just copy
	                // the raw XSRF-TOKEN cookie into the X-XSRF-TOKEN header (plain
	                // double-submit, no HTML reflection to protect against). Without this,
	                // the cookie is never actually written AND a matching cookie+header pair
	                // still gets rejected, so every non-exempt write silently 403s.
	                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
	                // Explicit list, NOT a "/api/auth/**" wildcard — change-password
	                // lives under this same controller/prefix but must still require
	                // a valid CSRF token like any other authenticated write.
	                .ignoringRequestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/logout")
	        )
	        .sessionManagement(session -> session
	                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
	        )
	        // Must match what JwtAuthenticationFilter saves into — otherwise
	        // SessionManagementFilter's containsContext(request) check (used to
	        // decide whether this looks like a "new" authentication) reads from a
	        // different repository than what the filter wrote to, always sees
	        // "missing", and fires CsrfAuthenticationStrategy on every request —
	        // wiping the XSRF-TOKEN cookie every other authenticated request.
	        .securityContext(context -> context.securityContextRepository(new RequestAttributeSecurityContextRepository()))
	        .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
	        .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
	        .authorizeHttpRequests(auth -> auth
	                // 1. Cho phép tất cả mọi người truy cập các file tĩnh (CSS, JS, Images)
	                .requestMatchers("/css/**", "/js/**", "/assets/**", "/favicon.ico", "/images/**", "/articles/**").permitAll()
	                .requestMatchers("/assets/css/admin/**", "/assets/js/admin/**").permitAll()

	                // 2. Cho phép xem Trang chủ và các trang hiển thị sản phẩm
					.requestMatchers("/", "/homepage", "/categories", "/categories/**", "/collection", "/collection/**", 
					"/collections", "/collections/**", "/products/**", "/blogs/**", "/admin/login").permitAll()

	                // 3. Mở khóa toàn bộ API lấy dữ liệu sản phẩm và Giỏ hàng tạm thời
	                .requestMatchers("/api/products/**").permitAll()
	                .requestMatchers("/api/cart/**").permitAll() // Khách chưa login vẫn thêm được vào guest_001

	                // 4. Change-password must be checked BEFORE the broader /api/auth/**
	                // permitAll rule below — first-match-wins, so this needs to come first.
	                .requestMatchers("/api/auth/change-password").authenticated()

	                // Mở khóa API Auth (Đăng ký/Đăng nhập/Đăng xuất)
	                .requestMatchers("/api/auth/**").permitAll()
					.requestMatchers("/api/internal/**").permitAll()

	                // 5. CHẶN: Chỉ những trang này mới cần Đăng nhập
	                .requestMatchers("/checkout/**", "/order/**", "/profile/**").authenticated()
	                .requestMatchers("/api/cart/merge").authenticated() // Chỉ user đã login mới được gọi merge

	                // 6. Admin panel + admin API — EMPLOYEE and ADMIN both allowed at
	                // this prefix level; ADMIN-exclusive endpoints are enforced with
	                // @PreAuthorize on the specific controller methods (see class doc).
	                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "EMPLOYEE")
	                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "EMPLOYEE")

	                .anyRequest().permitAll() // Các yêu cầu khác cho phép hết để tránh lỗi 403 phát sinh
	            )
	        .exceptionHandling(exception -> exception
	        	    .authenticationEntryPoint((request, response, authException) -> {
	        	        // Not authenticated at all (no valid token/cookie present)
	        	        String uri = request.getRequestURI();
	        	        if (uri.startsWith("/api/")) {
	        	            response.setStatus(HttpStatus.UNAUTHORIZED.value());
	        	            response.setContentType("application/json;charset=UTF-8");
	        	            response.setCharacterEncoding("UTF-8");
	        	            response.getWriter().write("{\"message\":\"Unauthorized\"}");
	        	        } else if (uri.startsWith("/admin")) {
	        	            response.sendRedirect("/admin/login");
	        	        } else {
	        	            response.sendRedirect("/homepage");
	        	        }
	        	    })
	        	    .accessDeniedHandler((request, response, accessDeniedException) -> {
	        	        // Authenticated, but wrong role — e.g. an EMPLOYEE hitting an
	        	        // @PreAuthorize("hasRole('ADMIN')") endpoint. Distinct from "not
	        	        // logged in" above; this case didn't exist under the old
	        	        // ADMIN-only /admin/** rule since no other authenticated role
	        	        // could reach it to trigger this path.
	        	        String uri = request.getRequestURI();
	        	        if (uri.startsWith("/api/")) {
	        	            response.setStatus(HttpStatus.FORBIDDEN.value());
	        	            response.setContentType("application/json;charset=UTF-8");
	        	            response.getWriter().write("{\"message\":\"Forbidden\"}");
	        	        } else {
	        	            response.sendRedirect("/homepage");
	        	        }
	        	    })
	        	)
	        .logout(logout -> logout
	            .logoutUrl("/api/auth/logout")
	            .logoutSuccessHandler((request, response, authentication) -> {
	                response.setStatus(200);
	                response.setContentType("application/json;charset=UTF-8");
	                response.getWriter().write("{\"message\": \"Logout Success\"}");
	            })
	            // Stateless: there's no server-side session to invalidate, so logout
	            // is just "tell the browser to drop the auth cookie."
	            .deleteCookies("auth_token")
	        );
	    return http.build();
	}

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}