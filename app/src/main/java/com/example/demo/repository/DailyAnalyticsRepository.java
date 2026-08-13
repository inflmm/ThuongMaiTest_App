package com.example.demo.repository;

import com.example.demo.model.DailyAnalytics;

import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyAnalyticsRepository extends JpaRepository<DailyAnalytics, Long> {
    
    Optional<DailyAnalytics> findByDate(LocalDate date);

    List<DailyAnalytics> findAllByDateBetweenOrderByDateDesc(LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(d.totalTraffic), 0) FROM DailyAnalytics d")
    long sumAllTraffic();

    @Query("SELECT COALESCE(SUM(d.uniqueSessions), 0) FROM DailyAnalytics d")
    long sumAllSessions();

    //Câu lệnh Atomic SQL: Cộng dồn giá trị totalTraffic và uniqueSessions cho một ngày cụ thể
    @Transactional
    @Modifying
    @Query("UPDATE DailyAnalytics d SET " +
           "d.totalTraffic = d.totalTraffic + :trafficDelta, " +
           "d.uniqueSessions = d.uniqueSessions + :sessionDelta " +
           "WHERE d.date = :date")
    int addDeltasByDate(@Param("date") LocalDate date,
                        @Param("trafficDelta") Long trafficDelta,
                        @Param("sessionDelta") Long sessionDelta);
}
