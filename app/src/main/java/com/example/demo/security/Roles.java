package com.example.demo.security;

/**
 * Centralizes the role strings used across User.role, JWT claims, and
 * SecurityConfig's authorizeHttpRequests rules — avoids typo'd literals like
 * "ROLE_EMPLOYE" silently creating a role nobody can ever match.
 */
public final class Roles {

    public static final String ADMIN = "ROLE_ADMIN";
    public static final String EMPLOYEE = "ROLE_EMPLOYEE";
    public static final String USER = "ROLE_USER";

    private Roles() {
    }
}
