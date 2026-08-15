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
import com.example.demo.service.MemoryMonitorService;

@RestController
@RequestMapping("/api/internal/analytics")
public class InternalAnalyticsController {
    
    private final AnalyticsBufferService analyticsBufferService;

    private final MemoryMonitorService memoryMonitorService;

    @Value("${app.cron.secret-token}")
    private String cronSecretToken;

    @Value("${app.analytics.enabled:true}")
    private boolean analyticsEnabled;
    
    public InternalAnalyticsController(AnalyticsBufferService analyticsBufferService, MemoryMonitorService memoryMonitorService) {
        this.analyticsBufferService = analyticsBufferService;
        this.memoryMonitorService = memoryMonitorService;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Pong");
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncData (@RequestHeader(value = "X-Cron-Secret", required = false) String incomingToken) {
        if (isUnauthorized(incomingToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing secret token");
        }

        analyticsBufferService.flushToDatabase();
        return ResponseEntity.ok("Data sync successfully");
    }

    // 4. API kích hoạt chụp ảnh RAM/Metaspace thủ công
    @PostMapping("/memory-snapshot")
    public ResponseEntity<String> triggerMemorySnapshot(@RequestHeader(value = "X-Cron-Secret", required = false) String incomingToken) {
        if (isUnauthorized(incomingToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing secret token");
        }

        if (!analyticsEnabled){
            return ResponseEntity.ok("Analytic disabled");
        }

        memoryMonitorService.recordSnapshot(); // Gọi trực tiếp hàm ghi log RAM
        return ResponseEntity.ok("Memory snapshot recorded successfully");
    }

    // Helper kiểm tra Secret Token gọn gàng hơn
    private boolean isUnauthorized(String incomingToken) {
        return incomingToken == null || !incomingToken.equals(cronSecretToken);
    }
}
