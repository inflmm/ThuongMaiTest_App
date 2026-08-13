package com.example.demo.service;

import org.springframework.scheduling.annotation.Async;

import com.example.demo.model.UserSessionLog;
import com.example.demo.repository.UserSessionLogRepository;
import com.example.demo.utils.SecurityUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

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

        UserSessionLog sessionLog = UserSessionLog.builder()
                .userId(userId)
                .username(username)
                .ipAddress(SecurityUtils.getClientIpAddress(request))
                .userAgent(userAgentStr)
                .sessionId(request.getSession().getId())
                .isCronPing(isCron != null && !isCron.isEmpty())
                .build();

        logRepository.save(sessionLog);
    }
}
