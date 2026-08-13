package com.example.demo.dto;

import java.time.LocalDate;

public class DailyChartDTO {
    private LocalDate date;
    private Long traffic;
    private Long sessions;

    public DailyChartDTO(LocalDate date, Long traffic, Long sessions) {
        this.date = date;
        this.traffic = traffic;
        this.sessions = sessions;
    }

    // Getters
    public LocalDate getDate() {
        return date;
    }
    public Long getTraffic() {
        return traffic;
    }
    public Long getSessions() {
        return sessions;
    }
}
