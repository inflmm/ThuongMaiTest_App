package com.example.demo.component;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.SessionLogService;

import jakarta.servlet.http.HttpSession;

class AuthenticationSuccessListenerTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void onAuthenticationSuccess_shouldStoreUserInfoInHttpSessionOnly() {
        SessionLogService sessionLogService = mock(SessionLogService.class);
        UserRepository userRepository = mock(UserRepository.class);
        AuthenticationSuccessListener listener = new AuthenticationSuccessListener(sessionLogService, userRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession();

        User user = new User();
        user.setId(55L);
        user.setUserId("USER_123");
        user.setUsername("alice");
        when(userRepository.findByUserId("USER_123")).thenReturn(Optional.of(user));

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        TestingAuthenticationToken authentication = new TestingAuthenticationToken("USER_123", "password");
        InteractiveAuthenticationSuccessEvent event = mock(InteractiveAuthenticationSuccessEvent.class);
        when(event.getAuthentication()).thenReturn(authentication);

        listener.onAuthenticationSuccess(event);

        verify(sessionLogService).createAuthenticatedSessionLog(
                session.getId(),
                "0:0:0:0:0:0:0:1", // Localhost IP in IPv6 format
                null, // User-Agent is not set in MockHttpServletRequest
                false, // Not a cron ping
                "USER_123",
                "alice"
        );
        assert session.getAttribute("userId") != null;
        assert session.getAttribute("username") != null;
    }
}
