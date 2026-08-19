package com.example.demo.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.demo.model.UserSessionLog;
import com.example.demo.repository.UserSessionLogRepository;
import com.example.demo.utils.UserAgentParser;

import jakarta.transaction.Transactional;

@Service
public class SessionLogService {

    private final UserSessionLogRepository logRepository;

    public SessionLogService(UserSessionLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * Creates a new session log entry for an authenticated user.
     */
    @Async
    @Transactional
    public void createAuthenticatedSessionLog(String sessionId, String ipAddress, String userAgent, boolean isCronPing, String userId, String userName) {
        UserAgentParser.UserAgentInfo uaInfo = UserAgentParser.parse(userAgent);

        UserSessionLog sessionLog = UserSessionLog.builder()
                .sessionId(sessionId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .browserName(uaInfo.getBrowserName())
                .osName(uaInfo.getOsName())
                .deviceType(uaInfo.getDeviceType())
                .isCronPing(isCronPing)
                .userId(userId)
                .username(userName)
                .build();

        logRepository.save(sessionLog);
    }
}