package com.puppies.api.cache;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User behavior profile for personalized caching strategies.
 * 
 * Tracks user patterns to optimize cache placement and TTL:
 * - Activity level (high/medium/low engagement users)
 * - Content preferences and access patterns
 * - Cache hit/miss ratios for performance optimization
 * - Session behavior and timing patterns
 */
@Getter
public class UserCacheProfile {
    
    private final Long userId;
    private final AtomicLong totalPostAccesses = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    
    // Activity tracking
    private LocalDateTime lastActivity = LocalDateTime.now();
    private LocalDateTime profileCreated = LocalDateTime.now();
    private final Set<Long> accessedPosts = Collections.synchronizedSet(new LinkedHashSet<>());
    
    // Engagement patterns
    private final List<LocalDateTime> sessionTimes = Collections.synchronizedList(new ArrayList<>());
    private String engagementLevel = "MEDIUM"; // LOW, MEDIUM, HIGH

    public UserCacheProfile(Long userId) {
        this.userId = userId;
    }

    /**
     * Record a post access for behavior analysis.
     */
    public void recordPostAccess(Long postId) {
        totalPostAccesses.incrementAndGet();
        accessedPosts.add(postId);
        updateLastActivity();
        
        // Keep only recent accessed posts (last 100)
        if (accessedPosts.size() > 100) {
            Iterator<Long> iterator = accessedPosts.iterator();
            iterator.next();
            iterator.remove();
        }
        
        // Update engagement level based on activity
        updateEngagementLevel();
    }

    /**
     * Record cache hit for performance tracking.
     */
    public void incrementCacheHits() {
        cacheHits.incrementAndGet();
    }

    /**
     * Record cache miss for performance tracking.
     */
    public void incrementCacheMisses() {
        cacheMisses.incrementAndGet();
    }

    /**
     * Update last activity timestamp.
     */
    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
        recordSessionActivity();
    }

    /**
     * Record session activity for pattern analysis.
     */
    private void recordSessionActivity() {
        sessionTimes.add(LocalDateTime.now());
        
        // Keep only recent sessions (last 50)
        if (sessionTimes.size() > 50) {
            sessionTimes.remove(0);
        }
    }

    /**
     * Analyze user activity to update engagement level.
     */
    private void updateEngagementLevel() {
        long accessesPerDay = getAccessesInLastDay();
        double sessionFrequency = getSessionFrequency();
        double cacheHitRate = getCacheHitRate();
        
        // High engagement: >50 accesses/day OR >10 sessions/day with good hit rate
        if (accessesPerDay > 50 || (sessionFrequency > 10 && cacheHitRate > 0.7)) {
            engagementLevel = "HIGH";
        }
        // Low engagement: <5 accesses/day OR poor cache performance
        else if (accessesPerDay < 5 || cacheHitRate < 0.3) {
            engagementLevel = "LOW";
        }
        // Everything else is medium
        else {
            engagementLevel = "MEDIUM";
        }
    }

    /**
     * Get post accesses in the last 24 hours.
     */
    private long getAccessesInLastDay() {
        LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);
        return sessionTimes.stream()
                .mapToLong(time -> time.isAfter(dayAgo) ? 1 : 0)
                .sum();
    }

    /**
     * Calculate average sessions per day.
     */
    private double getSessionFrequency() {
        if (sessionTimes.isEmpty()) return 0;
        
        LocalDateTime firstSession = sessionTimes.get(0);
        long daysSinceFirst = java.time.temporal.ChronoUnit.DAYS.between(firstSession, LocalDateTime.now());
        
        if (daysSinceFirst == 0) daysSinceFirst = 1; // Avoid division by zero
        
        return (double) sessionTimes.size() / daysSinceFirst;
    }

    /**
     * Calculate cache hit rate for this user.
     */
    public double getCacheHitRate() {
        long totalRequests = cacheHits.get() + cacheMisses.get();
        if (totalRequests == 0) return 0;
        
        return (double) cacheHits.get() / totalRequests;
    }

    /**
     * Check if this is a high engagement user.
     */
    public boolean isHighEngagement() {
        return "HIGH".equals(engagementLevel);
    }

    /**
     * Check if this is a low engagement user.
     */
    public boolean isLowEngagement() {
        return "LOW".equals(engagementLevel);
    }

    /**
     * Get recommended cache TTL based on user behavior.
     */
    public int getRecommendedCacheTtlMinutes() {
        return switch (engagementLevel) {
            case "HIGH" -> 30;   // High engagement users get longer cache
            case "MEDIUM" -> 15; // Medium engagement gets standard cache
            case "LOW" -> 5;     // Low engagement gets shorter cache
            default -> 10;
        };
    }

    /**
     * Get cache capacity recommendation for this user.
     */
    public int getRecommendedCacheCapacity() {
        return switch (engagementLevel) {
            case "HIGH" -> 100;   // Cache more items for active users
            case "MEDIUM" -> 50;  // Standard cache size
            case "LOW" -> 20;     // Smaller cache for inactive users
            default -> 50;
        };
    }

    /**
     * Check if user should get prioritized caching.
     */
    public boolean shouldPrioritizeInCache() {
        return isHighEngagement() && getCacheHitRate() > 0.8;
    }

    /**
     * Get user content preferences based on access history.
     */
    public List<Long> getPreferredContent() {
        // Return recently accessed posts as preference indicators
        return new ArrayList<>(accessedPosts).subList(
            Math.max(0, accessedPosts.size() - 10), 
            accessedPosts.size()
        );
    }

    /**
     * Check if user profile should be archived (inactive).
     */
    public boolean shouldArchive() {
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        return lastActivity.isBefore(weekAgo) && isLowEngagement();
    }

    /**
     * Get profile summary for monitoring and debugging.
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", userId);
        summary.put("engagementLevel", engagementLevel);
        summary.put("totalAccesses", totalPostAccesses.get());
        summary.put("cacheHitRate", String.format("%.2f%%", getCacheHitRate() * 100));
        summary.put("recommendedTtlMinutes", getRecommendedCacheTtlMinutes());
        summary.put("recommendedCacheCapacity", getRecommendedCacheCapacity());
        summary.put("shouldPrioritize", shouldPrioritizeInCache());
        summary.put("shouldArchive", shouldArchive());
        summary.put("lastActivity", lastActivity);
        summary.put("accessesLastDay", getAccessesInLastDay());
        summary.put("sessionFrequency", String.format("%.1f/day", getSessionFrequency()));
        
        return summary;
    }
}