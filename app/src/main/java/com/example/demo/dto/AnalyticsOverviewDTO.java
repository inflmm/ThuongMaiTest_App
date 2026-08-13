package com.example.demo.dto;

public class AnalyticsOverviewDTO {
    private Long todayTraffic;
    private Long todaySessions;
    private Long totalTraffic;
    private Long totalSessions;

    public AnalyticsOverviewDTO(Long todayTraffic, Long todaySessions, Long totalTraffic, Long totalSessions) {
        this.todayTraffic = todayTraffic;
        this.todaySessions = todaySessions;
        this.totalTraffic = totalTraffic;
        this.totalSessions = totalSessions;
    }

    // Getters
    public Long getTodayTraffic() {
        return todayTraffic;
    }
    public Long getTodaySessions() {
        return todaySessions;
    }
    public Long getTotalTraffic() {
        return totalTraffic;
    }
    public Long getTotalSessions() {
        return totalSessions;
    }
}
