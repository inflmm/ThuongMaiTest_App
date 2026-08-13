package com.example.demo.component;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.demo.service.SessionLogService;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class AuthenticationSuccessListener {
    
    private final SessionLogService sessionLogService;

    public AuthenticationSuccessListener(SessionLogService sessionLogService) {
        this.sessionLogService = sessionLogService;
    }

    @EventListener
    public void onAuthenticationSuccess(InteractiveAuthenticationSuccessEvent event) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            HttpServletRequest request = attributes.getRequest();
            String sessionId = request.getSession().getId();
            String username = event.getAuthentication().getName();
            Long userId = (Long) request.getSession().getAttribute("userId"); // Assuming userId is stored in session
            
            sessionLogService.updateUserForSession(sessionId, username, userId);
        }
    }
}
