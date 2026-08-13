package com.example.demo.controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AnalyticsBufferService;

@RestController
@RequestMapping("/api/internal/analytics")
public class InternalAnalyticsController {
    
    private final AnalyticsBufferService analyticsBufferService;

    @Value("${app.cron.secret-token}")
    private String cronSecretToken;
    
    public InternalAnalyticsController(AnalyticsBufferService analyticsBufferService) {
        this.analyticsBufferService = analyticsBufferService;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Pong");
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncData (@RequestHeader(value = "X-Cron-Secret", required = false) String incomingToken) {
        if (incomingToken == null || !incomingToken.equals(cronSecretToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing secret token");
        }

        analyticsBufferService.flushToDatabase();
        return ResponseEntity.ok("Data sync successfully");
    }
}
