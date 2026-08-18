package com.example.demo.controller.admin;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AnalyticsOverviewDTO;
import com.example.demo.dto.DailyChartDTO;
import com.example.demo.service.AnalyticsAdminService;

// Read-only analytics — left accessible to both ADMIN and EMPLOYEE via the
// coarse /api/admin/** rule in SecurityConfig; no @PreAuthorize needed here.
@RestController
@RequestMapping("/api/admin/analytics")
public class AnalyticsAdminController {

    private final AnalyticsAdminService adminService;
    private final Clock clock;

    public AnalyticsAdminController(AnalyticsAdminService adminService, Clock clock) {
        this.adminService = adminService;
        this.clock = clock;
    }

    // ---------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewDTO> getOverview() {
        return ResponseEntity.ok(adminService.getOverview());
    }

    @GetMapping("/daily")
    public ResponseEntity<List<DailyChartDTO>> getDailyAnalytics(
            @RequestParam(value = "days", required = false) Integer days,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (endDate == null) {
            endDate = LocalDate.now(clock);
        }
        if (startDate == null) {
            int rangeDays = (days != null && days > 0) ? days : 7; // Default to last 7 days if not specified
            startDate = endDate.minusDays((long) rangeDays - 1);
        }

        List<DailyChartDTO> result = adminService.getAnalyticsByDateRange(startDate, endDate);
        return ResponseEntity.ok(result);
    }
}
