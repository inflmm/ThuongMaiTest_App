package com.example.demo.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.demo.service.AnalyticsBufferService;
import com.example.demo.service.SessionLogService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class VisitorInterceptor implements HandlerInterceptor{
    
    private static final String COOKIE_NAME = "visited_session";
    private static final int COOKIE_MAX_AGE = 60 * 60; // 1 hour in seconds

    @Value("${app.analytics.enabled:true}")
    private boolean analyticsEnabled;

    @Value("${app.cron.secret-token}")
    private String cronSecretToken;

    private final AnalyticsBufferService analyticsBufferService;
    private final SessionLogService sessionLogService;

    public VisitorInterceptor(AnalyticsBufferService analyticsBufferService, SessionLogService sessionLogService) {
        this.analyticsBufferService = analyticsBufferService;
        this.sessionLogService = sessionLogService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        if (!analyticsEnabled) {
            return true; // Skip analytics tracking if disabled
        }
        String uri = request.getRequestURI();

        if (uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs")) {
            return true; // Allow access to Swagger UI and API docs without checking the cookie
        }

        boolean hasSessionCookie = false;
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    hasSessionCookie = true;
                    break;
                }
            }
        }

        String isCronJob = request.getHeader("X-Cron-Secret");
        
        
        // If the session cookie is not present and it's not a cron job request, set the cookie and increment unique sessions
        if (!hasSessionCookie && (isCronJob == null || !cronSecretToken.equals(isCronJob))) {
            Cookie sessionCookie = new Cookie(COOKIE_NAME, "true");
            sessionCookie.setMaxAge(COOKIE_MAX_AGE);
            sessionCookie.setPath("/");
            sessionCookie.setHttpOnly(true);
            response.addCookie(sessionCookie);

            String username = (String) request.getSession().getAttribute("username");
            Long userId = (Long) request.getSession().getAttribute("userId");

            analyticsBufferService.incrementSession();
            sessionLogService.recordSession(request, username, userId); // Log the session for anonymous users
            //System.out.println("Đây là 1 Unique Session mới!" + request.getRemoteAddr());
        }
        analyticsBufferService.incrementTraffic();
        //System.out.println("Ghi nhận 1 Lượt Traffic vào URI:" + uri);

        return true;
    }
}
