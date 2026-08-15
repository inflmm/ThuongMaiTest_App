package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider") // Kích hoạt JPA Auditing & chỉ định DateTimeProvider
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider dateTimeProvider(Clock clock) {
        // Bảo Spring JPA Auditing lấy thời gian từ Bean Clock đã cấu hình
        return () -> Optional.of(LocalDateTime.now(clock));
    }
}