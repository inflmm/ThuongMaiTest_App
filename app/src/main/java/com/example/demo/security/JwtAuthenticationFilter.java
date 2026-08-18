package com.example.demo.security;

import java.io.IOException;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs once per request, before Spring Security's normal auth machinery.
 * Reads the JWT (Authorization header, falling back to a cookie), validates
 * it, and populates the SecurityContext for THIS request only — nothing is
 * persisted between requests, which is what makes this stateless.
 *
 * If you go with the cookie transport, name it to match whatever your login
 * endpoint sets (this project uses "auth_token") and keep CSRF protection
 * in mind, since a cookie is auto-attached to any request to your domain.
 *
 * NOT a @Component: SecurityConfig constructs this explicitly and wires it
 * into the filter chain via addFilterBefore(). If this were also a
 * @Component, Spring Boot would additionally auto-register it as a global
 * servlet filter, running it twice per request.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_COOKIE_NAME = "auth_token";

    private final JwtService jwtService;
    // Marks the SecurityContext as already "saved" for this request so
    // SessionManagementFilter's containsContext(request) check sees it and
    // skips its session-authentication-strategy chain. Without this, every
    // request looked like a brand-new login (nothing persists between
    // stateless requests), which fired CsrfAuthenticationStrategy and wiped
    // the XSRF-TOKEN cookie on every OTHER authenticated request.
    private final SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtService.isTokenValid(token)) {
            String username = jwtService.extractUsername(token);
            String role = jwtService.extractRole(token);

            var authorities = List.of(new SimpleGrantedAuthority(role));
            var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContext context = SecurityContextHolder.getContext();
            context.setAuthentication(authentication);
            this.securityContextRepository.saveContext(context, request, response);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
