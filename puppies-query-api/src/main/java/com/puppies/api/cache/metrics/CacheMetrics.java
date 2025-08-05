package com.puppies.api.cache.metrics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive cache metrics collection and analysis.
 * 
 * Tracks performance metrics for intelligent cache optimization:
 * - Hit/miss ratios per cache layer
 * - Response time improvements
 * - Memory usage and efficiency
 * - Trending patterns and predictions
 * - Performance alerts and optimization suggestions
 */
@Component
@Slf4j
public class CacheMetrics {

    // Per-cache layer metrics
    private final Map<String, AtomicLong> cacheHits = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> cacheMisses = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> responseTimeHistory = new ConcurrentHashMap<>();
    
    // Overall performance tracking
    private final Map<LocalDateTime, CacheSnapshot> hourlySnapshots = new ConcurrentHashMap<>();
    private final List<CacheAlert> activeAlerts = Collections.synchronizedList(new ArrayList<>());
    
    // Thresholds for alerts
    private static final double MIN_HIT_RATE = 0.7;  // 70% minimum hit rate
    private static final long MAX_RESPONSE_TIME = 100; // 100ms max response time
    private static final int HISTORY_HOURS = 24; // Keep 24 hours of data

    /**
     * Record a cache hit for the specified cache layer.
     */
    public void recordHit(String cacheLayer) {
        cacheHits.computeIfAbsent(cacheLayer, k -> new AtomicLong(0)).incrementAndGet();
        checkPerformanceThresholds(cacheLayer);
    }

    /**
     * Record a cache miss for the specified cache layer.
     */
    public void recordMiss(String cacheLayer) {
        cacheMisses.computeIfAbsent(cacheLayer, k -> new AtomicLong(0)).incrementAndGet();
        checkPerformanceThresholds(cacheLayer);
    }

    /**
     * Record response time for performance tracking.
     */
    public void recordResponseTime(String operation, long responseTimeMs) {
        List<Long> history = responseTimeHistory.computeIfAbsent(operation, k -> 
            Collections.synchronizedList(new ArrayList<>()));
        
        history.add(responseTimeMs);
        
        // Keep only recent measurements (last 1000)
        if (history.size() > 1000) {
            history.remove(0);
        }
        
        // Check if response time is concerning
        if (responseTimeMs > MAX_RESPONSE_TIME) {
            createAlert(AlertType.SLOW_RESPONSE, operation, 
                String.format("Response time %dms exceeds threshold %dms", responseTimeMs, MAX_RESPONSE_TIME));
        }
    }

    /**
     * Get hit rate for a specific cache layer.
     */
    public double getHitRate(String cacheLayer) {
        long hits = cacheHits.getOrDefault(cacheLayer, new AtomicLong(0)).get();
        long misses = cacheMisses.getOrDefault(cacheLayer, new AtomicLong(0)).get();
        long total = hits + misses;
        
        return total > 0 ? (double) hits / total : 0.0;
    }

    /**
     * Get average response time for an operation.
     */
    public double getAverageResponseTime(String operation) {
        List<Long> history = responseTimeHistory.get(operation);
        if (history == null || history.isEmpty()) {
            return 0.0;
        }
        
        return history.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Take an hourly snapshot of cache performance.
     */
    public void takeHourlySnapshot() {
        LocalDateTime hour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        
        CacheSnapshot snapshot = new CacheSnapshot();
        snapshot.timestamp = hour;
        
        // Collect hit rates for all cache layers
        for (String layer : cacheHits.keySet()) {
            snapshot.hitRates.put(layer, getHitRate(layer));
        }
        
        // Collect response times
        for (String operation : responseTimeHistory.keySet()) {
            snapshot.avgResponseTimes.put(operation, getAverageResponseTime(operation));
        }
        
        hourlySnapshots.put(hour, snapshot);
        
        // Clean old snapshots
        LocalDateTime cutoff = LocalDateTime.now().minusHours(HISTORY_HOURS);
        hourlySnapshots.entrySet().removeIf(entry -> entry.getKey().isBefore(cutoff));
        
        log.debug("📊 Cache snapshot taken for hour: {}", hour);
    }

    /**
     * Check performance thresholds and create alerts if needed.
     */
    private void checkPerformanceThresholds(String cacheLayer) {
        double hitRate = getHitRate(cacheLayer);
        
        if (hitRate < MIN_HIT_RATE && getTotalRequests(cacheLayer) > 100) {
            createAlert(AlertType.LOW_HIT_RATE, cacheLayer,
                String.format("Hit rate %.2f%% below threshold %.2f%%", hitRate * 100, MIN_HIT_RATE * 100));
        }
    }

    /**
     * Create a performance alert.
     */
    private void createAlert(AlertType type, String component, String message) {
        // Check if similar alert already exists (avoid spam)
        boolean exists = activeAlerts.stream()
                .anyMatch(alert -> alert.type == type && alert.component.equals(component));
        
        if (!exists) {
            CacheAlert alert = new CacheAlert();
            alert.type = type;
            alert.component = component;
            alert.message = message;
            alert.timestamp = LocalDateTime.now();
            alert.severity = determineSeverity(type);
            
            activeAlerts.add(alert);
            
            log.warn("🚨 Cache alert: [{}] {} - {}", type, component, message);
        }
    }

    /**
     * Determine alert severity based on type.
     */
    private AlertSeverity determineSeverity(AlertType type) {
        return switch (type) {
            case LOW_HIT_RATE -> AlertSeverity.WARNING;
            case SLOW_RESPONSE -> AlertSeverity.ERROR;
            case HIGH_MEMORY_USAGE -> AlertSeverity.WARNING;
            case CACHE_EVICTION_HIGH -> AlertSeverity.INFO;
        };
    }

    /**
     * Get total requests (hits + misses) for a cache layer.
     */
    private long getTotalRequests(String cacheLayer) {
        long hits = cacheHits.getOrDefault(cacheLayer, new AtomicLong(0)).get();
        long misses = cacheMisses.getOrDefault(cacheLayer, new AtomicLong(0)).get();
        return hits + misses;
    }

    /**
     * Get comprehensive cache statistics.
     */
    public Map<String, Object> getOverallStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Hit rates by cache layer
        Map<String, String> hitRates = new HashMap<>();
        for (String layer : cacheHits.keySet()) {
            hitRates.put(layer, String.format("%.2f%%", getHitRate(layer) * 100));
        }
        stats.put("hitRates", hitRates);
        
        // Response times
        Map<String, String> responseTimes = new HashMap<>();
        for (String operation : responseTimeHistory.keySet()) {
            responseTimes.put(operation, String.format("%.1fms", getAverageResponseTime(operation)));
        }
        stats.put("avgResponseTimes", responseTimes);
        
        // Alert summary
        Map<String, Long> alertCounts = new HashMap<>();
        for (AlertSeverity severity : AlertSeverity.values()) {
            long count = activeAlerts.stream()
                    .mapToLong(alert -> alert.severity == severity ? 1 : 0)
                    .sum();
            alertCounts.put(severity.name(), count);
        }
        stats.put("activeAlerts", alertCounts);
        
        // Performance trends
        stats.put("hourlySnapshots", hourlySnapshots.size());
        stats.put("overallPerformance", calculateOverallPerformance());
        
        return stats;
    }

    /**
     * Calculate overall cache performance score (0-100).
     */
    private int calculateOverallPerformance() {
        if (cacheHits.isEmpty()) return 0;
        
        // Weight different factors
        double avgHitRate = cacheHits.keySet().stream()
                .mapToDouble(this::getHitRate)
                .average()
                .orElse(0.0);
        
        double avgResponseTime = responseTimeHistory.values().stream()
                .mapToDouble(history -> history.stream().mapToLong(Long::longValue).average().orElse(0))
                .average()
                .orElse(0.0);
        
        // Score based on hit rate (70%) and response time (30%)
        int hitRateScore = (int) (avgHitRate * 70);
        int responseTimeScore = avgResponseTime > 0 ? Math.max(0, 30 - (int)(avgResponseTime / 10)) : 30;
        
        return Math.min(100, hitRateScore + responseTimeScore);
    }

    /**
     * Get recent performance trends.
     */
    public Map<String, Object> getPerformanceTrends() {
        Map<String, Object> trends = new HashMap<>();
        
        if (hourlySnapshots.size() < 2) {
            trends.put("message", "Insufficient data for trend analysis");
            return trends;
        }
        
        // Analyze hit rate trends
        List<CacheSnapshot> recent = hourlySnapshots.values().stream()
                .sorted(Comparator.comparing(s -> s.timestamp))
                .toList();
        
        if (recent.size() >= 2) {
            CacheSnapshot latest = recent.get(recent.size() - 1);
            CacheSnapshot previous = recent.get(recent.size() - 2);
            
            for (String layer : latest.hitRates.keySet()) {
                if (previous.hitRates.containsKey(layer)) {
                    double trend = latest.hitRates.get(layer) - previous.hitRates.get(layer);
                    trends.put(layer + "_trend", String.format("%+.2f%%", trend * 100));
                }
            }
        }
        
        return trends;
    }

    /**
     * Get optimization recommendations based on metrics.
     */
    public List<String> getOptimizationRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        // Analyze hit rates
        for (String layer : cacheHits.keySet()) {
            double hitRate = getHitRate(layer);
            long totalRequests = getTotalRequests(layer);
            
            if (hitRate < 0.5 && totalRequests > 50) {
                recommendations.add(String.format(
                    "Consider increasing TTL or cache size for %s layer (hit rate: %.1f%%)", 
                    layer, hitRate * 100));
            }
            
            if (hitRate > 0.95 && totalRequests > 100) {
                recommendations.add(String.format(
                    "Excellent performance in %s layer - consider using it for more content", 
                    layer));
            }
        }
        
        // Analyze response times
        for (String operation : responseTimeHistory.keySet()) {
            double avgTime = getAverageResponseTime(operation);
            if (avgTime > MAX_RESPONSE_TIME) {
                recommendations.add(String.format(
                    "Optimize %s operation - average response time %.1fms exceeds threshold", 
                    operation, avgTime));
            }
        }
        
        return recommendations;
    }

    // Helper classes for metrics storage
    private static class CacheSnapshot {
        LocalDateTime timestamp;
        Map<String, Double> hitRates = new HashMap<>();
        Map<String, Double> avgResponseTimes = new HashMap<>();
    }

    private static class CacheAlert {
        AlertType type;
        String component;
        String message;
        LocalDateTime timestamp;
        AlertSeverity severity;
    }

    private enum AlertType {
        LOW_HIT_RATE,
        SLOW_RESPONSE,
        HIGH_MEMORY_USAGE,
        CACHE_EVICTION_HIGH
    }

    private enum AlertSeverity {
        INFO, WARNING, ERROR
    }
}