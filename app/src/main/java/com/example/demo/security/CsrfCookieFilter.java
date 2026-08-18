package com.example.demo.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * CsrfFilter stores the CsrfToken as a deferred (lazily-resolved) request
 * attribute — the XSRF-TOKEN cookie is only actually written once something
 * calls csrfToken.getToken(). Nothing in this app (no Thymeleaf template
 * references "_csrf") ever does that, so without this filter the cookie is
 * never sent and the JS clients' csrfHeaders() always reads an empty cookie.
 * This is Spring Security's own documented fix for SPA-style CSRF cookies.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken(); // forces the deferred token to resolve and save to the cookie repository
        }
        filterChain.doFilter(request, response);
    }
}
