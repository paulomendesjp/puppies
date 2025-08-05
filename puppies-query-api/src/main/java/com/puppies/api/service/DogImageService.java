package com.puppies.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Service for fetching random dog images from external APIs.
 * Used to populate posts with beautiful dog pictures for demo purposes.
 */
@Service
@Slf4j
public class DogImageService {

    private final RestTemplate restTemplate;
    private final Random random = new Random();

    // Multiple dog API sources for variety
    private static final List<String> DOG_API_ENDPOINTS = Arrays.asList(
        "https://dog.ceo/api/breeds/image/random",
        "https://random.dog/woof.json",
        "https://api.thedogapi.com/v1/images/search"
    );

    // Dog breeds for themed content
    private static final List<String> DOG_BREEDS = Arrays.asList(
        "golden-retriever", "labrador", "husky", "bulldog", "poodle",
        "german-shepherd", "beagle", "rottweiler", "yorkie", "chihuahua",
        "boxer", "dachshund", "siberian-husky", "border-collie", "pug"
    );

    public DogImageService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofSeconds(5))
                .setReadTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    /**
     * Get a random dog image URL for posts.
     * Falls back to local placeholder if APIs fail.
     */
    public String getRandomDogImage() {
        try {
            return fetchFromDogCeoApi();
        } catch (Exception e) {
            log.warn("Failed to fetch from dog.ceo API, trying fallback", e);
            try {
                return fetchFromRandomDogApi();
            } catch (Exception e2) {
                log.warn("Failed to fetch from random.dog API, using placeholder", e2);
                return getPlaceholderDogImage();
            }
        }
    }

    /**
     * Get a breed-specific dog image for themed posts.
     */
    public String getBreedSpecificImage(String breed) {
        try {
            String url = "https://dog.ceo/api/breed/" + breed.toLowerCase() + "/images/random";
            DogCeoResponse response = restTemplate.getForObject(url, DogCeoResponse.class);
            
            if (response != null && "success".equals(response.status)) {
                log.debug("🐕 Fetched {} image: {}", breed, response.message);
                return response.message;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch {} image, using random", breed, e);
        }
        
        return getRandomDogImage();
    }

    /**
     * Get random breed name for post content generation.
     */
    public String getRandomBreed() {
        return DOG_BREEDS.get(random.nextInt(DOG_BREEDS.size()));
    }

    /**
     * Generate dog-themed text content for posts.
     */
    public String generateDogPostContent() {
        List<String> templates = Arrays.asList(
            "Look at this adorable %s! 🐕❤️",
            "My %s is having the best day ever! 🌟",
            "Can't get enough of this %s's cuteness! 😍",
            "This %s knows how to pose for the camera! 📸",
            "Spending quality time with my %s 🥰",
            "My %s is the goodest boy/girl! 🏆",
            "Adventure time with my %s! 🚶‍♂️🐕",
            "This %s has stolen my heart ❤️",
            "Living the best life with my %s! ✨",
            "My %s is pure joy on four legs! 🐾"
        );
        
        String template = templates.get(random.nextInt(templates.size()));
        String breed = getRandomBreed().replace("-", " ");
        
        return String.format(template, breed);
    }

    /**
     * Fetch from dog.ceo API (most reliable).
     */
    private String fetchFromDogCeoApi() {
        DogCeoResponse response = restTemplate.getForObject(
            "https://dog.ceo/api/breeds/image/random", 
            DogCeoResponse.class
        );
        
        if (response != null && "success".equals(response.status)) {
            log.debug("🐕 Fetched dog image from dog.ceo: {}", response.message);
            return response.message;
        }
        
        throw new RuntimeException("Invalid response from dog.ceo API");
    }

    /**
     * Fetch from random.dog API (fallback).
     */
    private String fetchFromRandomDogApi() {
        RandomDogResponse response = restTemplate.getForObject(
            "https://random.dog/woof.json", 
            RandomDogResponse.class
        );
        
        if (response != null && response.url != null) {
            log.debug("🐕 Fetched dog image from random.dog: {}", response.url);
            return response.url;
        }
        
        throw new RuntimeException("Invalid response from random.dog API");
    }

    /**
     * Generate placeholder image URL if all APIs fail.
     */
    private String getPlaceholderDogImage() {
        // Using picsum with a dog-themed seed
        int imageId = 200 + random.nextInt(100); // Random image between 200-300
        String placeholderUrl = "https://picsum.photos/600/600?random=" + imageId;
        
        log.debug("🐕 Using placeholder image: {}", placeholderUrl);
        return placeholderUrl;
    }

    /**
     * Check if external APIs are available.
     */
    public boolean isExternalApiAvailable() {
        try {
            fetchFromDogCeoApi();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get health status of dog image services.
     */
    public DogApiHealthStatus getHealthStatus() {
        DogApiHealthStatus status = new DogApiHealthStatus();
        
        // Test dog.ceo
        try {
            fetchFromDogCeoApi();
            status.dogCeoStatus = "UP";
        } catch (Exception e) {
            status.dogCeoStatus = "DOWN";
        }
        
        // Test random.dog
        try {
            fetchFromRandomDogApi();
            status.randomDogStatus = "UP";
        } catch (Exception e) {
            status.randomDogStatus = "DOWN";
        }
        
        status.fallbackAvailable = true; // Picsum is always available
        
        return status;
    }

    // Response DTOs
    public static class DogCeoResponse {
        public String message;
        public String status;
    }

    public static class RandomDogResponse {
        public String fileSizeBytes;
        public String url;
    }

    public static class DogApiHealthStatus {
        public String dogCeoStatus;
        public String randomDogStatus;
        public boolean fallbackAvailable;
    }
}