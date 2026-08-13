package com.example.demo.service;

import java.util.Optional;

import org.springframework.scheduling.annotation.Async;

import com.example.demo.model.UserSessionLog;
import com.example.demo.repository.UserSessionLogRepository;
import com.example.demo.utils.SecurityUtils;
import com.example.demo.utils.UserAgentParser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

@Service
public class SessionLogService {

    private final UserSessionLogRepository logRepository;

    public SessionLogService(UserSessionLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Async
    public void recordSession(HttpServletRequest request, String username, Long userId) {
        String userAgentStr = request.getHeader("User-Agent");
        String isCron = request.getHeader("X-Cron-Secret");

        UserAgentParser.UserAgentInfo uaInfo = UserAgentParser.parse(userAgentStr);

        UserSessionLog sessionLog = UserSessionLog.builder()
                .sessionId(request.getSession().getId())
                .ipAddress(SecurityUtils.getClientIpAddress(request))
                .userAgent(userAgentStr)
                .browserName(uaInfo.getBrowserName())
                .osName(uaInfo.getOsName())
                .deviceType(uaInfo.getDeviceType())
                .isCronPing("true".equals(isCron))
                .userId(null)   // Tạm thời null cho khách vãng lai
                .username(null) // Tạm thời null cho khách vãng lai
                .build();

        logRepository.save(sessionLog);
    }

    @Async
    @Transactional
    public void updateUserForSession(String sessionId, String ipAddress, String userAgent, Long userId, String username) {
        Optional<UserSessionLog> logOptional = logRepository.findFirstBySessionIdOrderByCreatedTimeDesc(sessionId);
        
        if (logOptional.isEmpty()) {
            logOptional = logRepository.findFirstByIpAddressAndUserAgentOrderByCreatedTimeDesc(ipAddress, userAgent);
        }

        logOptional.ifPresent(log -> {
            log.setUserId(userId);
            log.setUsername(username);
            log.setSessionId(sessionId);
            logRepository.save(log);
        });
    }
}
