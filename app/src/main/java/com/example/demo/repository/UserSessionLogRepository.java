package com.example.demo.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.UserSessionLog;

public interface UserSessionLogRepository extends JpaRepository<UserSessionLog, Long> {
    // Custom query methods can be defined here if needed
    Optional<UserSessionLog> findFirstBySessionIdOrderByCreatedTimeDesc(String sessionId);
    Optional<UserSessionLog> findFirstByIpAddressAndUserAgentOrderByCreatedTimeDesc(String ipAddress, String userAgent);
}
