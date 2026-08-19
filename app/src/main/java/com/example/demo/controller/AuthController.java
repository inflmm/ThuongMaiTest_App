package com.example.demo.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

import com.example.demo.dto.ChangePasswordDTO;
import com.example.demo.dto.LoginRequestDTO;
import com.example.demo.dto.UserRegistrationDTO;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import com.example.demo.service.AuthService;
import com.example.demo.service.SessionLogService;
import com.example.demo.utils.SecurityUtils;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles registration, login, and password change. Login is JWT-based
 * (stateless) rather than session-based.
 * Note: username here is the unique identifier (userId) generated at registration, not the email or phone number.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${app.analytics.enabled:true}")
    private boolean analyticsEnabled;

    @Value("${app.cron.secret-token}")
    private String cronSecretToken;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final SessionLogService sessionLogService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, UserRepository userRepository, AuthService authService, SessionLogService sessionLogService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.authService = authService;
        this.sessionLogService = sessionLogService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserRegistrationDTO dto) {
        try {
            User savedUser = authService.registerNewUser(dto);
            return ResponseEntity.ok("Đăng ký thành công với ID: " + savedUser.getUserId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi đăng ký: " + e.getMessage());
        }
    }

    /**
     * Replaces the old formLogin flow. Authenticates credentials manually via
     * AuthenticationManager (this still uses whatever UserDetailsService +
     * PasswordEncoder you already had wired for the session-based version —
     * nothing changes there), then issues a JWT as an httpOnly cookie instead
     * of creating an HttpSession.
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDTO dto, HttpServletResponse response, HttpServletRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.username(), dto.password()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("{\"message\": \"Login Failed\"}");
        }

        String userid = authentication.getName();
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");

        User user = userRepository.findByUserId(userid)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + userid));

        String token = jwtService.generateToken(userid, user.getUserId(), role);

        ResponseCookie cookie = ResponseCookie.from("auth_token", token)
                .httpOnly(true)
                .secure(true) // requires HTTPS — correct for Render; if it causes issues testing over plain http on localhost, flip to false only for local profiles
                .path("/")
                .maxAge(60 * 60L) // keep in sync with app.jwt.expiration-ms
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        // --- GHI LOG ĐĂNG NHẬP THÀNH CÔNG TẠI ĐÂY ---
        if (analyticsEnabled) {
            String userName = (String) request.getAttribute("userName");
            String userId = (String) request.getAttribute("userId");
            String ipAddress = SecurityUtils.getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            String sessionId = null; 
            
            String cronHeader = request.getHeader("X-Cron-Secret");
            boolean isCronPing = cronHeader != null && cronSecretToken.equals(cronHeader);

            // Gọi Async Log
            sessionLogService.createAuthenticatedSessionLog(
                sessionId, ipAddress, userAgent, isCronPing, userId, userName
            );
        }

        return ResponseEntity.ok(String.format("{\"message\": \"Login Success\", \"role\": \"%s\"}", role));
    }

    /**
     * Self-service password change for regular USER accounts. Note this is
     * NOT under the /api/auth/** permitAll+CSRF-exempt umbrella despite
     * living in this controller — SecurityConfig carves out
     * /api/auth/change-password specifically to require authentication and
     * CSRF, since this is a real state-changing action, unlike
     * register/login/logout.
     */
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordDTO dto, Authentication authentication) {
        // Username comes from the validated JWT principal, never the request
        // body — a client-supplied identifier here would let anyone change
        // anyone else's password by just editing the request.
        String userid = authentication.getName();
 
        try {
            authService.changePassword(userid, dto.currentPassword(), dto.newPassword());
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Mật khẩu hiện tại không đúng"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
