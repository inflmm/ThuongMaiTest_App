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
     * Fire-and-forget visitor/session log write. Deliberately takes only plain
     * values — never an HttpServletRequest. Callers (see VisitorInterceptor)
     * must extract everything they need while still on the original request
     * thread; touching the request object from here would risk
     * IllegalStateException, since Tomcat may have already recycled it by the
     * time this method actually runs on the async executor's thread.
     */
    @Async
    public void recordSession(String sessionId, String ipAddress, String userAgent, boolean isCronPing, String userId, String username) {
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
                .username(username)
                .build();

        logRepository.save(sessionLog);
    }

    /**
     * Same plain-values contract as recordSession() — whatever calls this
     * (likely your login success handler) needs the same fix: extract
     * sessionId/ipAddress/userAgent from the request synchronously, before
     * calling this method, rather than passing the request through.
     */
    @Async
    @Transactional
    public void createAuthenticatedSessionLog(String sessionId, String ipAddress, String userAgent, boolean isCronPing, String userId, String username) {
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
                .username(username)
                .build();

        logRepository.save(sessionLog);
    }
}