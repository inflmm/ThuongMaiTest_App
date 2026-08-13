package com.example.demo.controller;

import java.util.List;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AnalyticsOverviewDTO;
import com.example.demo.dto.DailyChartDTO;
import com.example.demo.service.AnalyticsAdminService;

@RestController
@RequestMapping("api/admin/analytics")
public class AnalyticsAdminController {
    
    private final Clock clock;
    private final AnalyticsAdminService adminService;

    public AnalyticsAdminController(AnalyticsAdminService adminService, Clock clock) {
        this.adminService = adminService;
        this.clock = clock;
    }
    
    /**
     * Endpoint to retrieve an overview of analytics data.
     * @return ResponseEntity containing AnalyticsOverviewDTO
     */
    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewDTO> getOverview() {
        return ResponseEntity.ok(adminService.getOverview());
    }

    @GetMapping("/daily")
    public ResponseEntity<List<DailyChartDTO>> getDailyAnalytics(
        @RequestParam(value = "days", required = false) Integer days,
        @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (endDate == null){
            endDate = LocalDate.now(clock);
        }

        if (startDate == null) {
            int rangeDays = (days != null && days > 0) ? days : 7; // Default to last 7 days if not specified
            startDate = endDate.minusDays((long)rangeDays - 1); 
        }

        List<DailyChartDTO> result = adminService.getAnalyticsByDateRange(startDate, endDate);
        return ResponseEntity.ok(result);
    }
}
