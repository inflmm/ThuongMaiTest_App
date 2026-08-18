package com.example.demo.controller.admin;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ChangePasswordDTO;
import com.example.demo.service.AuthService;

/**
 * Self-service account actions for the admin panel — deliberately
 * EMPLOYEE-only, not ADMIN. The ADMIN account's password is changed
 * out-of-band instead (locally generated BCrypt hash, pasted directly into
 * Supabase), keeping that single highest-privilege credential off the HTTP
 * surface entirely.
 *
 * hasRole("EMPLOYEE") below genuinely excludes ADMIN accounts — this only
 * works cleanly because SecurityConfig deliberately does NOT wire a
 * RoleHierarchy into the expression handlers (see its class doc). If a
 * hierarchy were active, ADMIN would silently inherit EMPLOYEE-level access
 * here too, which is exactly what we don't want on this one endpoint.
 */
@RestController
@RequestMapping("/api/admin/account")
public class AdminAccountController {

    private final AuthService authService;

    public AdminAccountController(AuthService authService) {
        this.authService = authService;
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordDTO dto, Authentication authentication) {
        String username = authentication.getName();

        try {
            authService.changePassword(username, dto.currentPassword(), dto.newPassword());
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Mật khẩu hiện tại không đúng"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}