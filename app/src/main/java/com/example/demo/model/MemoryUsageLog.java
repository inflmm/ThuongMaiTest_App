package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "memory_usage_log")
public class MemoryUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime recordedAt;

    // All values in MB unless noted otherwise
    private long heapUsedMb;
    private long heapCommittedMb;
    private long heapMaxMb;

    private long nonHeapUsedMb;
    private long nonHeapCommittedMb;

    private long metaspaceUsedMb;
    private long metaspaceCommittedMb;
    private long metaspaceMaxMb; // -1 if unbounded

    private int loadedClassCount;
    private long totalLoadedClassCount; // cumulative since JVM start — a steadily rising gap
                                         // vs loadedClassCount is the DevTools-leak signature
    private long unloadedClassCount;

    private int threadCount;

    public MemoryUsageLog() {
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public long getHeapUsedMb() {
        return heapUsedMb;
    }

    public void setHeapUsedMb(long heapUsedMb) {
        this.heapUsedMb = heapUsedMb;
    }

    public long getHeapCommittedMb() {
        return heapCommittedMb;
    }

    public void setHeapCommittedMb(long heapCommittedMb) {
        this.heapCommittedMb = heapCommittedMb;
    }

    public long getHeapMaxMb() {
        return heapMaxMb;
    }

    public void setHeapMaxMb(long heapMaxMb) {
        this.heapMaxMb = heapMaxMb;
    }

    public long getNonHeapUsedMb() {
        return nonHeapUsedMb;
    }

    public void setNonHeapUsedMb(long nonHeapUsedMb) {
        this.nonHeapUsedMb = nonHeapUsedMb;
    }

    public long getNonHeapCommittedMb() {
        return nonHeapCommittedMb;
    }

    public void setNonHeapCommittedMb(long nonHeapCommittedMb) {
        this.nonHeapCommittedMb = nonHeapCommittedMb;
    }

    public long getMetaspaceUsedMb() {
        return metaspaceUsedMb;
    }

    public void setMetaspaceUsedMb(long metaspaceUsedMb) {
        this.metaspaceUsedMb = metaspaceUsedMb;
    }

    public long getMetaspaceCommittedMb() {
        return metaspaceCommittedMb;
    }

    public void setMetaspaceCommittedMb(long metaspaceCommittedMb) {
        this.metaspaceCommittedMb = metaspaceCommittedMb;
    }

    public long getMetaspaceMaxMb() {
        return metaspaceMaxMb;
    }

    public void setMetaspaceMaxMb(long metaspaceMaxMb) {
        this.metaspaceMaxMb = metaspaceMaxMb;
    }

    public int getLoadedClassCount() {
        return loadedClassCount;
    }

    public void setLoadedClassCount(int loadedClassCount) {
        this.loadedClassCount = loadedClassCount;
    }

    public long getTotalLoadedClassCount() {
        return totalLoadedClassCount;
    }

    public void setTotalLoadedClassCount(long totalLoadedClassCount) {
        this.totalLoadedClassCount = totalLoadedClassCount;
    }

    public long getUnloadedClassCount() {
        return unloadedClassCount;
    }

    public void setUnloadedClassCount(long unloadedClassCount) {
        this.unloadedClassCount = unloadedClassCount;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }
}
