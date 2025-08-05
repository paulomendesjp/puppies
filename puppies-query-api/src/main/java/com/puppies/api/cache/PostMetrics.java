package com.puppies.api.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks metrics for individual posts to enable intelligent caching decisions.
 * 
 * Metrics tracked:
 * - Views (total and per hour)
 * - Unique viewers  
 * - Engagement rate (likes, comments vs views)
 * - Access patterns and recency
 * - Trending indicators
 */
@Getter
@Slf4j
public class PostMetrics {
    
    private final Long postId;
    private final AtomicLong totalViews = new AtomicLong(0);
    private final AtomicLong totalLikes = new AtomicLong(0);
    private final AtomicLong totalComments = new AtomicLong(0);
    
    // Time-based tracking
    private final Map<LocalDateTime, Long> hourlyViews = new ConcurrentHashMap<>();
    private final Set<Long> uniqueViewers = Collections.synchronizedSet(new HashSet<>());
    private final Set<Long> recentViewers = Collections.synchronizedSet(new HashSet<>());
    
    private LocalDateTime lastAccessed = LocalDateTime.now();
    private LocalDateTime createdAt = LocalDateTime.now();
    private boolean isWarmed = false;
    
    // Engagement tracking
    private final List<LocalDateTime> recentEngagements = Collections.synchronizedList(new ArrayList<>());

    public PostMetrics(Long postId) {
        this.postId = postId;
    }

    /**
     * Increment view count and update hourly tracking.
     */
    public void incrementViews() {
        totalViews.incrementAndGet();
        
        LocalDateTime hour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        hourlyViews.merge(hour, 1L, Long::sum);
        
        // Clean old hourly data (keep last 24 hours)
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        hourlyViews.entrySet().removeIf(entry -> entry.getKey().isBefore(cutoff));
    }

    /**
     * Track a user accessing this post.
     */
    public void addRecentAccess(Long userId) {
        uniqueViewers.add(userId);
        recentViewers.add(userId);
        
        // Keep only recent viewers (last hour)
        if (recentViewers.size() > 1000) {
            recentViewers.clear();
        }
    }

    /**
     * Record engagement (like, comment, share).
     */
    public void recordEngagement(String type) {
        recentEngagements.add(LocalDateTime.now());
        
        switch (type.toLowerCase()) {
            case "like" -> totalLikes.incrementAndGet();
            case "comment" -> totalComments.incrementAndGet();
        }
        
        // Clean old engagements (keep last 2 hours)
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        recentEngagements.removeIf(time -> time.isBefore(cutoff));
    }

    /**
     * Get views in the last hour.
     */
    public long getViewsInLastHour() {
        LocalDateTime currentHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        return hourlyViews.getOrDefault(currentHour, 0L);
    }

    /**
     * Get unique viewers in the last hour.
     */
    public long getUniqueViewersInLastHour() {
        return recentViewers.size();
    }

    /**
     * Calculate engagement rate (likes + comments) / views.
     */
    public double getEngagementRate() {
        long views = totalViews.get();
        if (views == 0) return 0.0;
        
        long engagements = totalLikes.get() + totalComments.get();
        return (double) engagements / views;
    }

    /**
     * Check if this post is currently trending.
     * Based on recent views, engagement, and velocity.
     */
    public boolean isTrending() {
        // Must have minimum activity
        if (totalViews.get() < 10) return false;
        
        // Check recent engagement velocity
        long recentEngagements = this.recentEngagements.size();
        long viewsLastHour = getViewsInLastHour();
        
        // Trending criteria:
        // 1. Recent views > 20 per hour OR
        // 2. High engagement rate (>5%) with recent activity OR  
        // 3. Rapid engagement growth (>10 engagements in 2 hours)
        return (viewsLastHour > 20) ||
               (getEngagementRate() > 0.05 && viewsLastHour > 5) ||
               (recentEngagements > 10);
    }

    /**
     * Calculate overall popularity score for ranking.
     */
    public double getPopularityScore() {
        double viewsScore = Math.log(totalViews.get() + 1) * 0.3;
        double engagementScore = getEngagementRate() * 1000 * 0.4;
        double recencyScore = getRecencyScore() * 0.2;
        double trendingBonus = isTrending() ? 100 : 0;
        
        return viewsScore + engagementScore + recencyScore + trendingBonus;
    }

    /**
     * Calculate recency score (newer posts get higher scores).
     */
    private double getRecencyScore() {
        long hoursOld = ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
        
        // Posts lose 10% score per hour, but level off after 24 hours
        if (hoursOld >= 24) {
            return 10; // Minimum score for old posts
        }
        
        return Math.max(10, 100 - (hoursOld * 3.75)); // 90 points spread over 24 hours
    }

    /**
     * Check if post should be evicted from cache due to low activity.
     */
    public boolean shouldEvict() {
        // Evict if no activity in last 2 hours AND low total engagement
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        boolean noRecentActivity = lastAccessed.isBefore(twoHoursAgo);
        boolean lowEngagement = getEngagementRate() < 0.01 && totalViews.get() < 50;
        
        return noRecentActivity && lowEngagement;
    }

    /**
     * Update last accessed time.
     */
    public void updateLastAccessed() {
        this.lastAccessed = LocalDateTime.now();
    }

    /**
     * Mark this post as cache-warmed.
     */
    public void markAsWarmed() {
        this.isWarmed = true;
    }

    /**
     * Get summary for debugging/monitoring.
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("postId", postId);
        summary.put("totalViews", totalViews.get());
        summary.put("totalLikes", totalLikes.get());
        summary.put("totalComments", totalComments.get());
        summary.put("uniqueViewers", uniqueViewers.size());
        summary.put("viewsLastHour", getViewsInLastHour());
        summary.put("engagementRate", String.format("%.2f%%", getEngagementRate() * 100));
        summary.put("isTrending", isTrending());
        summary.put("popularityScore", String.format("%.1f", getPopularityScore()));
        summary.put("shouldEvict", shouldEvict());
        summary.put("isWarmed", isWarmed);
        summary.put("lastAccessed", lastAccessed);
        
        return summary;
    }
}