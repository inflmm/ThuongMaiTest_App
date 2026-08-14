package com.example.demo.service;

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
    public void recordSession(HttpServletRequest request, String username, String userId) {
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

        logRepository.save(sessionLog);
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
