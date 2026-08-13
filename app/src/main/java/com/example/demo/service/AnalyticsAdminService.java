package com.example.demo.service;

import com.example.demo.dto.AnalyticsOverviewDTO;
import com.example.demo.dto.DailyChartDTO;
import com.example.demo.model.DailyAnalytics;
import com.example.demo.repository.DailyAnalyticsRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class AnalyticsAdminService {

    private final Clock clock;
    
    private final DailyAnalyticsRepository analyticsRepository;

    public AnalyticsAdminService(DailyAnalyticsRepository analyticsRepository, Clock clock) {
        this.analyticsRepository = analyticsRepository;
        this.clock = clock;
    }
    
    public AnalyticsOverviewDTO getOverview() {
        LocalDate today = LocalDate.now(clock);

        // Fetch today's analytics
        Optional<DailyAnalytics> todayDb = analyticsRepository.findByDate(today);
        long todayTraffic = todayDb.map(DailyAnalytics::getTotalTraffic).orElse(0L);
        long todaySessions = todayDb.map(DailyAnalytics::getUniqueSessions).orElse(0L);

        // Fetch total analytics
        long totalTraffic = analyticsRepository.sumAllTraffic();
        long totalSessions = analyticsRepository.sumAllSessions();

        return new AnalyticsOverviewDTO(todayTraffic, todaySessions, totalTraffic, totalSessions);
    }

    public List<DailyChartDTO> getAnalyticsByDateRange(LocalDate startDate, LocalDate endDate) {
        List<DailyAnalytics> records = analyticsRepository.findAllByDateBetweenOrderByDateDesc(startDate, endDate);
    
        return records.stream()
                .map(r -> new DailyChartDTO(r.getDate(), r.getTotalTraffic(), r.getUniqueSessions()))
                .toList();
    }

}
