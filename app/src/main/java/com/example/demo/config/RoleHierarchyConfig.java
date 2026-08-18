package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

@Configuration
public class RoleHierarchyConfig {

    /**
     * ADMIN inherits everything EMPLOYEE can do. Routes should be written with
     * the LOWEST role that should have access — e.g. hasRole("EMPLOYEE") for
     * anything both roles can reach, hasRole("ADMIN") only for admin-exclusive
     * routes — rather than hasAnyRole("ADMIN", "EMPLOYEE") everywhere.
     *
     * Must be a static @Bean method — Spring Security resolves this very
     * early in context startup, before normal bean instantiation order is
     * guaranteed, and a non-static method can trigger a circular dependency.
     */
    
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_EMPLOYEE");
    }
}
