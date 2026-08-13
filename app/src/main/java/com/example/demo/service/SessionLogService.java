package com.example.demo.service;

import java.util.Optional;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.demo.model.UserSessionLog;
import com.example.demo.repository.UserSessionLogRepository;
import com.example.demo.utils.SecurityUtils;
import com.example.demo.utils.UserAgentParser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@Service
public class SessionLogService {

    private final UserSessionLogRepository logRepository;

    public SessionLogService(UserSessionLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Async
    public Long recordSession(HttpServletRequest request, String username, String userId) {
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
                .userId(null)
                .username(null)
                .build();

        UserSessionLog saved = logRepository.save(sessionLog);
        return saved.getId();
    }

    public Long findAnonymousLogIdBySessionId(String sessionId) {
        return logRepository.findFirstBySessionIdOrderByCreatedTimeDesc(sessionId)
                .map(UserSessionLog::getId)
                .orElse(null);
    }

    @Async
    @Transactional
    public void updateUserForSession(Long anonymousLogId, String sessionId, String userId, String username) {
        if (anonymousLogId == null) {
            return;
        }

        logRepository.findById(anonymousLogId).ifPresent(log -> {
            log.setUserId(userId);
            log.setUsername(username);
            log.setSessionId(sessionId);
            logRepository.save(log);
        });
    }

    @Async
    @Transactional
    public void createAuthenticatedSessionLog(HttpServletRequest request, String userId, String username) {
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
                .userId(userId)
                .username(username)
                .build();

        logRepository.save(sessionLog);
    }
}
