package com.example.demo.component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.demo.service.AnalyticsBufferService;
import com.example.demo.service.SessionLogService;
import com.example.demo.utils.SecurityUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class VisitorInterceptor implements HandlerInterceptor{
    
    private static final String COOKIE_NAME = "visited_session";
    private final Clock clock;

    @Value("${app.analytics.enabled:true}")
    private boolean analyticsEnabled;

    @Value("${app.cron.secret-token}")
    private String cronSecretToken;

    private final AnalyticsBufferService analyticsBufferService;
    private final SessionLogService sessionLogService;

    public VisitorInterceptor(AnalyticsBufferService analyticsBufferService, SessionLogService sessionLogService, Clock clock) {
        this.analyticsBufferService = analyticsBufferService;
        this.sessionLogService = sessionLogService;
        this.clock = clock;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String cronHeader = request.getHeader("X-Cron-Secret");
        boolean isCronPing = cronHeader != null && cronSecretToken.equals(cronHeader);
        // Skip analytics tracking if disabled or cron ping or if the request is for Swagger UI or API docs
        // This allows the cron job to ping the endpoint without affecting analytics
        if (!analyticsEnabled || isCronPing || uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs")) {
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
            // A real per-session UUID, not a static "true" flag — this becomes
            // the sessionId stored on UserSessionLog below, so log rows can
            // actually be correlated back to "the same anonymous visitor,
            // same day" rather than sessionId being permanently null.
            String sessionValue = UUID.randomUUID().toString();

            Cookie sessionCookie = new Cookie(COOKIE_NAME, sessionValue);
            sessionCookie.setMaxAge(secondsUntilEndOfDay());
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
            // Most of the time, userName/userId will be null here, since this is
            // an anonymous first-visit-of-the-day event — these are only non-null
            // when a logged-in user's token was already present on this request.
            String userName = (String) request.getAttribute("userName");
            String userId = (String) request.getAttribute("userId");
            String ipAddress = SecurityUtils.getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
 
            analyticsBufferService.incrementSession();
            // isCronPing is always false at this call site (real cron pings already
            // returned above) — kept as an explicit parameter so the service method
            // stays reusable from other call sites without guessing.
            sessionLogService.createAuthenticatedSessionLog(sessionValue, ipAddress, userAgent, false, userId, userName);

        }
        analyticsBufferService.incrementTraffic();
        //System.out.println("Ghi nhận 1 Lượt Traffic vào URI:" + uri);

        return true;
    }

    /**
     * Seconds remaining until midnight, using the shared app Clock (see
     * ClockConfig — Asia/Ho_Chi_Minh) so this always agrees with whatever
     * timezone DailyAnalytics buckets on, rather than relying on the JVM's
     * possibly-different default zone.
     */
    private int secondsUntilEndOfDay() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return (int) Duration.between(now, midnight).getSeconds();
    }
}