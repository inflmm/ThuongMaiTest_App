package com.example.demo.service;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.demo.model.MemoryUsageLog;
import com.example.demo.repository.MemoryUsageLogRepository;

/**
 * Periodically snapshots JVM heap, non-heap, metaspace, and class-loading
 * stats and persists them, so metaspace trend and class-count growth can be
 * inspected over time instead of only being visible at the moment of an
 * OutOfMemoryError.
 *
 * Requires @EnableScheduling on a configuration class if not already present
 * elsewhere in the app.
 */
@Service
public class MemoryMonitorService {

    @Value("${app.analytics.enabled:true}")
    private boolean analyticsEnabled;

    private static final long MB = (long) 1024 * 1024;
    private final Clock clock;

    private final MemoryUsageLogRepository repository;

    public MemoryMonitorService(MemoryUsageLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    // Runs once an hour, independent of wall-clock alignment.
    // Swap to @Scheduled(cron = "0 0 * * * *") if you want it aligned to the top of each hour.
    @Scheduled(fixedRate = 60 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void recordSnapshot() {
        
        if(!analyticsEnabled){
            return;
        }
        
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ClassLoadingMXBean classBean = ManagementFactory.getClassLoadingMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();

        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();

        MemoryUsageLog log = new MemoryUsageLog();
        log.setRecordedAt(LocalDateTime.now(clock));

        log.setHeapUsedMb(heap.getUsed() / MB);
        log.setHeapCommittedMb(heap.getCommitted() / MB);
        log.setHeapMaxMb(heap.getMax() / MB);

        log.setNonHeapUsedMb(nonHeap.getUsed() / MB);
        log.setNonHeapCommittedMb(nonHeap.getCommitted() / MB);

        // Metaspace is exposed as a named memory pool, not part of MemoryMXBean directly
        for (MemoryPoolMXBean pool : pools) {
            if ("Metaspace".equals(pool.getName())) {
                MemoryUsage usage = pool.getUsage();
                log.setMetaspaceUsedMb(usage.getUsed() / MB);
                log.setMetaspaceCommittedMb(usage.getCommitted() / MB);
                log.setMetaspaceMaxMb(usage.getMax() < 0 ? -1 : usage.getMax() / MB);
                break;
            }
        }

        log.setLoadedClassCount(classBean.getLoadedClassCount());
        log.setTotalLoadedClassCount(classBean.getTotalLoadedClassCount());
        log.setUnloadedClassCount(classBean.getUnloadedClassCount());

        log.setThreadCount(threadBean.getThreadCount());

        repository.save(log);
    }
}
