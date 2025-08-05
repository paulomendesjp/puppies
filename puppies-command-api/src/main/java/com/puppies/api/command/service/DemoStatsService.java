package com.puppies.api.command.service;

import com.puppies.api.common.constants.ApiConstants;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

/**
 * Service responsible for providing demo statistics and system information.
 * Centralizes demo-related data and metrics.
 */
@Service
public class DemoStatsService {

    /**
     * Get comprehensive demo statistics.
     * 
     * @return Map containing demo system statistics and information
     */
    public Map<String, Object> getDemoStats() {
        return Map.of(
            "availableBreeds", Arrays.asList(ApiConstants.DemoMessages.DOG_BREEDS),
            "dogApiEndpoints", Arrays.asList(
                ApiConstants.ApiUrls.DOG_CEO_API_RANDOM, 
                ApiConstants.ApiUrls.RANDOM_DOG_API
            ),
            "demoMessages", ApiConstants.DemoMessages.DOG_MESSAGES.length,
            "maxDemoPosts", ApiConstants.BusinessRules.MAX_DEMO_POSTS,
            "commandApiPort", ApiConstants.Ports.COMMAND_API_PORT,
            "queryApiPort", ApiConstants.Ports.QUERY_API_PORT,
            "syncWorkerPort", ApiConstants.Ports.SYNC_WORKER_PORT,
            "message", "Demo system ready! 🚀"
        );
    }

    /**
     * Get a random dog message for demo posts.
     * 
     * @return Random dog message string
     */
    public String getRandomDogMessage() {
        String[] messages = ApiConstants.DemoMessages.DOG_MESSAGES;
        int randomIndex = (int) (Math.random() * messages.length);
        return messages[randomIndex];
    }

    /**
     * Get a random dog message with post number for multiple posts.
     * 
     * @param postNumber The post number to include in the message
     * @return Random dog message with post number
     */
    public String getRandomDogMessage(int postNumber) {
        return getRandomDogMessage() + " (Demo Post #" + postNumber + ")";
    }
}