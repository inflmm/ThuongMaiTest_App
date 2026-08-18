package com.example.demo.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.demo.model.DailyAnalytics;
import com.example.demo.repository.DailyAnalyticsRepository;

import jakarta.transaction.Transactional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsBufferService {
    
    private final AtomicInteger trafficDelta = new AtomicInteger(0);
    private final AtomicInteger sessionDelta = new AtomicInteger(0);

    private final DailyAnalyticsRepository analyticsRepository;
    private final Clock clock;

    public AnalyticsBufferService(DailyAnalyticsRepository analyticsRepository, Clock clock) {
        this.analyticsRepository = analyticsRepository;
        this.clock = clock;
    }

    public void incrementTraffic() {
        trafficDelta.incrementAndGet();
    }

    public void incrementSession() {
        sessionDelta.incrementAndGet();
    }

    @Transactional
    @Scheduled(cron = "0 */6 0 * * *") // Every 6 hours
    public void flushToDatabase() {
        int currentTraffic = trafficDelta.getAndSet(0);
        int currentSessions = sessionDelta.getAndSet(0);

        if (currentTraffic == 0 && currentSessions == 0) {
            return; // No changes to flush
        }

        LocalDate today = LocalDate.now(clock);

        int updatedRows = analyticsRepository.addDeltasByDate(today, (long)currentTraffic, (long)currentSessions);

        if (updatedRows == 0) {
            // If no rows were updated, it means there's no entry for today yet. Create one.
            DailyAnalytics newRecord = new DailyAnalytics(today, (long)currentTraffic, (long)currentSessions);
            analyticsRepository.save(newRecord);
        }
        
    }
}
