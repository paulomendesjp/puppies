package com.puppies.api.command.service;

import com.puppies.api.common.constants.ApiConstants;
import com.puppies.api.service.DogImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service responsible for checking the health status of system components.
 * Provides centralized health monitoring functionality.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemHealthService {

    private final RestTemplate restTemplate;
    private final DogImageService dogImageService;

    @Value("${app.query-api.base-url:" + ApiConstants.ApiUrls.DEFAULT_QUERY_API_BASE_URL + "}")
    private String queryApiBaseUrl;

    /**
     * Get comprehensive system health status.
     * 
     * @return Map containing health status of all system components
     */
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new java.util.HashMap<>();
        
        // Check Command API (self)
        health.put("commandApi", Map.of(
            "status", "UP", 
            "port", ApiConstants.Ports.COMMAND_API_PORT
        ));
        
        // Check Query API
        health.put("queryApi", checkQueryApiHealth());
        
        // Check Sync Worker
        health.put("syncWorker", checkSyncWorkerHealth());
        
        // Check Dog APIs
        health.put("dogApis", dogImageService.getHealthStatus());
        
        // Overall status
        boolean allUp = isSystemHealthy(health);
        health.put("overall", Map.of(
            "status", allUp ? "UP" : "DEGRADED",
            "timestamp", LocalDateTime.now(),
            "message", allUp ? "All systems operational! 🟢" : "Some components are down 🟡"
        ));
        
        return health;
    }

    /**
     * Check Query API health status.
     */
    private Map<String, Object> checkQueryApiHealth() {
        try {
            restTemplate.getForObject(queryApiBaseUrl + "/actuator/health", Object.class);
            return Map.of(
                "status", "UP", 
                "port", ApiConstants.Ports.QUERY_API_PORT, 
                "url", queryApiBaseUrl
            );
        } catch (Exception e) {
            log.debug("Query API health check failed: {}", e.getMessage());
            return Map.of(
                "status", "DOWN", 
                "port", ApiConstants.Ports.QUERY_API_PORT, 
                "url", queryApiBaseUrl,
                "error", e.getMessage(),
                "solution", "Start with: ./start-query-api.sh"
            );
        }
    }

    /**
     * Check Sync Worker health status.
     */
    private Map<String, Object> checkSyncWorkerHealth() {
        try {
            restTemplate.getForObject(ApiConstants.ApiUrls.SYNC_WORKER_HEALTH_URL, Object.class);
            return Map.of(
                "status", "UP", 
                "port", ApiConstants.Ports.SYNC_WORKER_PORT
            );
        } catch (Exception e) {
            log.debug("Sync Worker health check failed: {}", e.getMessage());
            return Map.of(
                "status", "DOWN", 
                "port", ApiConstants.Ports.SYNC_WORKER_PORT,
                "error", e.getMessage(),
                "solution", "Start with: ./start-sync-worker.sh"
            );
        }
    }

    /**
     * Determine if the overall system is healthy.
     */
    private boolean isSystemHealthy(Map<String, Object> health) {
        for (Object component : health.values()) {
            if (component instanceof Map && "DOWN".equals(((Map<?, ?>) component).get("status"))) {
                return false;
            }
        }
        return true;
    }
}