package com.puppies.api.cache.strategy;

import com.puppies.api.cache.UserCacheProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache strategy based on individual user behavior patterns.
 * 
 * This strategy personalizes caching decisions based on:
 * - User engagement level (high/medium/low)
 * - Historical cache hit rates
 * - Activity patterns and preferences
 * - Session frequency and duration
 */
@Component
@Slf4j
public class UserBehaviorCacheStrategy implements CacheStrategy {

    // Track user profiles for personalized caching
    private final Map<String, UserCacheProfile> userProfiles = new ConcurrentHashMap<>();
    
    // Strategy parameters
    private static final double HIGH_ENGAGEMENT_CACHE_HIT_THRESHOLD = 0.8;
    private static final int HIGH_ENGAGEMENT_MIN_ACCESSES = 50;
    private static final int MAX_PROFILES_CACHE = 10000;

    @Override
    public boolean shouldCache(Object content, String cacheKey, Object... context) {
        UserCacheProfile profile = extractUserProfile(cacheKey, context);
        if (profile == null) return true; // Default to caching
        
        // Always cache for high engagement users
        if (profile.isHighEngagement()) {
            return true;
        }
        
        // Cache selectively for low engagement users
        if (profile.isLowEngagement()) {
            // Only cache if user has good cache hit rate (indicates they return to content)
            return profile.getCacheHitRate() > 0.5;
        }
        
        // Medium engagement users get standard caching
        return true;
    }

    @Override
    public int getRecommendedTtlSeconds(Object content, String cacheKey, Object... context) {
        UserCacheProfile profile = extractUserProfile(cacheKey, context);
        if (profile == null) return 600; // 10 minutes default
        
        // Personalized TTL based on user behavior
        return profile.getRecommendedCacheTtlMinutes() * 60;
    }

    @Override
    public String getTargetCacheLayer(Object content, String cacheKey, Object... context) {
        UserCacheProfile profile = extractUserProfile(cacheKey, context);
        if (profile == null) return "warm_posts";
        
        // High engagement users get priority cache placement
        if (profile.shouldPrioritizeInCache()) {
            return "hot_posts";
        }
        
        // Engagement-based cache layer selection
        return switch (profile.getEngagementLevel()) {
            case "HIGH" -> "warm_posts";
            case "MEDIUM" -> "warm_posts";
            case "LOW" -> "cold_posts";
            default -> "warm_posts";
        };
    }

    @Override
    public boolean shouldEvict(String cacheKey, Object cachedContent, Object... context) {
        UserCacheProfile profile = extractUserProfile(cacheKey, context);
        if (profile == null) return false;
        
        // Don't evict content for high engagement users with good hit rates
        if (profile.isHighEngagement() && profile.getCacheHitRate() > HIGH_ENGAGEMENT_CACHE_HIT_THRESHOLD) {
            return false;
        }
        
        // Evict content for inactive users
        if (profile.shouldArchive()) {
            log.debug("🗑️ Evicting content for inactive user {}", extractUserId(cacheKey));
            return true;
        }
        
        // For low engagement users, evict if poor cache performance
        if (profile.isLowEngagement() && profile.getCacheHitRate() < 0.3) {
            return true;
        }
        
        return false;
    }

    @Override
    public String getStrategyName() {
        return "UserBehaviorStrategy";
    }

    /**
     * Extract user profile from context or cache key.
     */
    private UserCacheProfile extractUserProfile(String cacheKey, Object... context) {
        // Try to get from context first
        for (Object ctx : context) {
            if (ctx instanceof UserCacheProfile) {
                return (UserCacheProfile) ctx;
            }
        }
        
        // Extract user ID from cache key and get profile
        String userId = extractUserId(cacheKey);
        if (userId != null) {
            return userProfiles.get(userId);
        }
        
        return null;
    }

    /**
     * Extract user ID from cache key.
     * Assumes cache key format like "post:123:user:456" or "feed:main:user:456"
     */
    private String extractUserId(String cacheKey) {
        if (cacheKey != null && cacheKey.contains(":user:")) {
            String[] parts = cacheKey.split(":user:");
            if (parts.length > 1) {
                String userPart = parts[1];
                // Handle potential additional segments after user ID
                String[] userParts = userPart.split(":");
                return userParts[0];
            }
        }
        return null;
    }

    /**
     * Update user profile for strategy decisions.
     */
    public void updateUserProfile(String userId, UserCacheProfile profile) {
        userProfiles.put(userId, profile);
        
        // Periodic cleanup to prevent memory leaks
        if (userProfiles.size() > MAX_PROFILES_CACHE) {
            cleanupInactiveProfiles();
        }
    }

    /**
     * Record user interaction for behavior analysis.
     */
    public void recordUserInteraction(String userId, String interactionType, boolean cacheHit) {
        UserCacheProfile profile = userProfiles.computeIfAbsent(userId, 
            k -> new UserCacheProfile(Long.parseLong(userId)));
        
        if (cacheHit) {
            profile.incrementCacheHits();
        } else {
            profile.incrementCacheMisses();
        }
        
        profile.updateLastActivity();
        
        log.debug("👤 User {} interaction: {} (cache {})", userId, interactionType, 
                cacheHit ? "HIT" : "MISS");
    }

    /**
     * Analyze user behavior to provide caching recommendations.
     */
    public Map<String, Object> analyzeUserBehavior(String userId) {
        UserCacheProfile profile = userProfiles.get(userId);
        if (profile == null) {
            return Map.of("status", "No data available for user " + userId);
        }
        
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("userId", userId);
        analysis.put("engagementLevel", profile.getEngagementLevel());
        analysis.put("cacheHitRate", String.format("%.2f%%", profile.getCacheHitRate() * 100));
        analysis.put("recommendedTtl", profile.getRecommendedCacheTtlMinutes() + " minutes");
        analysis.put("shouldPrioritize", profile.shouldPrioritizeInCache());
        analysis.put("preferredCacheLayer", getTargetCacheLayer(null, "user:" + userId, profile));
        
        // Recommendations
        if (profile.getCacheHitRate() > 0.8) {
            analysis.put("recommendation", "Excellent cache performance - increase TTL and cache more content");
        } else if (profile.getCacheHitRate() < 0.3) {
            analysis.put("recommendation", "Poor cache performance - reduce TTL and cache less content");
        } else {
            analysis.put("recommendation", "Standard caching strategy is working well");
        }
        
        return analysis;
    }

    /**
     * Clean up inactive user profiles.
     */
    private void cleanupInactiveProfiles() {
        int sizeBefore = userProfiles.size();
        
        userProfiles.entrySet().removeIf(entry -> entry.getValue().shouldArchive());
        
        int sizeAfter = userProfiles.size();
        log.debug("🧹 Cleaned up user profiles: {} → {} (removed {})", 
                sizeBefore, sizeAfter, sizeBefore - sizeAfter);
    }

    /**
     * Get users by engagement level for analysis.
     */
    public Map<String, Long> getUsersByEngagementLevel() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("HIGH", 0L);
        counts.put("MEDIUM", 0L);
        counts.put("LOW", 0L);
        
        for (UserCacheProfile profile : userProfiles.values()) {
            String level = profile.getEngagementLevel();
            counts.merge(level, 1L, Long::sum);
        }
        
        return counts;
    }

    /**
     * Get strategy statistics for monitoring.
     */
    public Map<String, Object> getStrategyStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("trackedUsers", userProfiles.size());
        stats.put("usersByEngagement", getUsersByEngagementLevel());
        
        // Calculate average cache hit rate
        double avgHitRate = userProfiles.values().stream()
                .mapToDouble(UserCacheProfile::getCacheHitRate)
                .average()
                .orElse(0.0);
        stats.put("avgCacheHitRate", String.format("%.2f%%", avgHitRate * 100));
        
        // Count prioritized users
        long prioritizedUsers = userProfiles.values().stream()
                .mapToLong(p -> p.shouldPrioritizeInCache() ? 1 : 0)
                .sum();
        stats.put("prioritizedUsers", prioritizedUsers);
        
        // Recent activity
        LocalDateTime hourAgo = LocalDateTime.now().minusHours(1);
        long activeUsersLastHour = userProfiles.values().stream()
                .mapToLong(p -> p.getLastActivity().isAfter(hourAgo) ? 1 : 0)
                .sum();
        stats.put("activeUsersLastHour", activeUsersLastHour);
        
        stats.put("strategyName", getStrategyName());
        
        return stats;
    }

    /**
     * Get personalized cache recommendations for a user.
     */
    public Map<String, Object> getPersonalizedRecommendations(String userId) {
        UserCacheProfile profile = userProfiles.get(userId);
        if (profile == null) {
            return Map.of("status", "No data for user " + userId);
        }
        
        Map<String, Object> recommendations = new HashMap<>();
        
        // Cache layer recommendation
        String recommendedLayer = getTargetCacheLayer(null, "user:" + userId, profile);
        recommendations.put("cacheLayer", recommendedLayer);
        
        // TTL recommendation
        recommendations.put("ttlMinutes", profile.getRecommendedCacheTtlMinutes());
        
        // Content recommendations based on access patterns
        recommendations.put("preferredContent", profile.getPreferredContent());
        
        // Performance insights
        if (profile.getCacheHitRate() > 0.9) {
            recommendations.put("insight", "Excellent prediction accuracy - user has predictable behavior");
        } else if (profile.getCacheHitRate() < 0.4) {
            recommendations.put("insight", "Unpredictable behavior - consider reducing cache for this user");
        }
        
        return recommendations;
    }
}