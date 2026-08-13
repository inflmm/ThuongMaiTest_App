package com.example.demo.component;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.SessionLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Component
public class AuthenticationSuccessListener {
    
    private final SessionLogService sessionLogService;
    private final UserRepository userRepository;

    public AuthenticationSuccessListener(SessionLogService sessionLogService, UserRepository userRepository) {
        this.sessionLogService = sessionLogService;
        this.userRepository = userRepository;
    }

    @EventListener
    public void onAuthenticationSuccess(InteractiveAuthenticationSuccessEvent event) {
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

        session.setAttribute("userId", user.getUserId());
        session.setAttribute("username", user.getUsername());

        Long anonymousLogId = (Long) request.getAttribute("anonymousSessionLogId");
        if (anonymousLogId == null) {
            anonymousLogId = sessionLogService.findAnonymousLogIdBySessionId(session.getId());
        }

        String currentSessionId = session.getId();

        if (anonymousLogId != null) {
            sessionLogService.updateUserForSession(anonymousLogId, currentSessionId, user.getUserId(), user.getUsername());
            request.removeAttribute("anonymousSessionLogId");
        } else {
            sessionLogService.createAuthenticatedSessionLog(request, user.getUserId(), user.getUsername());
        }
    }
}
