package com.example.demo.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.SessionLogService;
import com.example.demo.utils.SecurityUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Listener for successful authentication events.
 * Obsolete because of stateless JWT authentication
 */
@Component
public class AuthenticationSuccessListener {
    
    private final SessionLogService sessionLogService;
    private final UserRepository userRepository;

    public AuthenticationSuccessListener(SessionLogService sessionLogService, UserRepository userRepository) {
        this.sessionLogService = sessionLogService;
        this.userRepository = userRepository;
    }
    @Value("${app.analytics.enabled:true}")
    private boolean analyticsEnabled;

    @Value("${app.cron.secret-token}")
    private String cronSecretToken;

    @EventListener
    public void onAuthenticationSuccess(InteractiveAuthenticationSuccessEvent event) {

        if (!analyticsEnabled) {
            return; // Skip analytics tracking if disabled
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession(false);

        if (session == null) {
            return;
        }

        String requestedUserId = event.getAuthentication().getName();
        User user = userRepository.findByUserId(requestedUserId)
                .orElseGet(() -> userRepository.findByUsername(requestedUserId).orElse(null));

        if (user == null) {
            return;
        }

        String cronHeader = request.getHeader("X-Cron-Secret");
        boolean isCronPing = cronHeader != null && cronSecretToken.equals(cronHeader);

        String sessionId = session.getId();
        String ipAddress = SecurityUtils.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        sessionLogService.createAuthenticatedSessionLog(sessionId, ipAddress, userAgent, isCronPing, user.getUserId(), user.getUsername());
    }
}
