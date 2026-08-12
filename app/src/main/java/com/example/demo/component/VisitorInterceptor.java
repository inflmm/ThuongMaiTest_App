package com.example.demo.component;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.demo.service.AnalyticsBufferService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class VisitorInterceptor implements HandlerInterceptor{
    
    private static final String COOKIE_NAME = "visited_session";
    private static final int COOKIE_MAX_AGE = 60 * 60; // 1 hour in seconds

    private final AnalyticsBufferService analyticsBufferService;

    public VisitorInterceptor(AnalyticsBufferService analyticsBufferService) {
        this.analyticsBufferService = analyticsBufferService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
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

        if (!hasSessionCookie) {
            Cookie sessionCookie = new Cookie(COOKIE_NAME, "true");
            sessionCookie.setMaxAge(COOKIE_MAX_AGE);
            sessionCookie.setPath("/");
            sessionCookie.setHttpOnly(true);
            response.addCookie(sessionCookie);

            analyticsBufferService.incrementSession();
            //System.out.println("Đây là 1 Unique Session mới!" + request.getRemoteAddr());
        }
        analyticsBufferService.incrementTraffic();
        //System.out.println("Ghi nhận 1 Lượt Traffic vào URI:" + uri);

        return true;
    }
}
