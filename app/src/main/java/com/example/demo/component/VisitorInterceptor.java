package com.example.demo.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.demo.service.AnalyticsBufferService;
import com.example.demo.service.SessionLogService;
import com.example.demo.utils.SecurityUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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

        String cronHeader = request.getHeader("X-Cron-Secret");
        boolean isCronPing = cronHeader != null && cronSecretToken.equals(cronHeader);
        if (isCronPing) {
            return true;
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
        
        
        // If the session cookie is not present and it's not a cron job request, set the cookie and increment unique sessions
        if (!hasSessionCookie) {
            Cookie sessionCookie = new Cookie(COOKIE_NAME, "true");
            sessionCookie.setMaxAge(COOKIE_MAX_AGE);
            sessionCookie.setPath("/");
            sessionCookie.setHttpOnly(true);
            response.addCookie(sessionCookie);

            // Everything the async logger needs must be extracted HERE, on the
            // original request thread — never pass the HttpServletRequest itself
            // into @Async code. Tomcat pools and recycles Request/Response objects
            // once a request completes; by the time an @Async method body actually
            // runs (on a different thread, possibly after this method has already
            // returned), the request may already be reset for reuse, causing
            // IllegalStateException: The request object has been recycled...
            HttpSession session = request.getSession();
            String username = (String) session.getAttribute("username");
            String userId = (String) session.getAttribute("userId");
            String sessionId = session.getId();
            String ipAddress = SecurityUtils.getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
 
            analyticsBufferService.incrementSession();
            // isCronPing is always false at this call site (real cron pings already
            // returned above) — kept as an explicit parameter so the service method
            // stays reusable from other call sites without guessing.
            sessionLogService.recordSession(sessionId, ipAddress, userAgent, isCronPing, userId, username);

        }
        analyticsBufferService.incrementTraffic();
        //System.out.println("Ghi nhận 1 Lượt Traffic vào URI:" + uri);

        return true;
    }
}
