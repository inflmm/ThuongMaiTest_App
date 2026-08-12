package com.example.demo.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "daily_analytics")
public class DailyAnalytics extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "total_traffic", nullable = false)
    private Long totalTraffic = (long) 0;

    @Column(name = "unique_sessions", nullable = false)
    private Long uniqueSessions = (long) 0;

    public DailyAnalytics() {}

    public DailyAnalytics(LocalDate date, Long totalTraffic, Long uniqueSessions) {
        this.date = date;
        this.totalTraffic = totalTraffic;
        this.uniqueSessions = uniqueSessions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getTotalTraffic() {
        return totalTraffic;
    }

    public void setTotalTraffic(Long totalTraffic) {
        this.totalTraffic = totalTraffic;
    }

    public Long getUniqueSessions() {
        return uniqueSessions;
    }

    public void setUniqueSessions(Long uniqueSessions) {
        this.uniqueSessions = uniqueSessions;
    }
}
