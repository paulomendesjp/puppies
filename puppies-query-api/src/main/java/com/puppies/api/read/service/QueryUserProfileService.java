package com.puppies.api.read.service;

import com.puppies.api.read.model.ReadUserProfile;
import com.puppies.api.read.repository.ReadUserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Query service for user profiles - read-only operations from read store
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QueryUserProfileService {

    private final ReadUserProfileRepository readUserProfileRepository;

    /**
     * Get user profile by ID
     */
    @Cacheable(value = "user_profile", key = "#userId")
    public Optional<ReadUserProfile> getUserProfile(Long userId) {
        log.info("👤 CACHE MISS - Loading user profile from DB: userId={}", userId);
        Optional<ReadUserProfile> result = readUserProfileRepository.findById(userId);
        if (result.isPresent()) {
            log.info("👤 CACHE STORE - Loaded user profile: userId={}, name={}", userId, result.get().getName());
        } else {
            log.warn("👤 CACHE STORE - User profile not found: userId={}", userId);
        }
        return result;
    }

    /**
     * Get user profile by email
     */
    @Cacheable(value = "user_profile_by_email", key = "#email")
    public Optional<ReadUserProfile> getUserProfileByEmail(String email) {
        log.info("📧 CACHE MISS - Loading user profile by email from DB: email={}", email);
        Optional<ReadUserProfile> result = readUserProfileRepository.findByEmail(email);
        if (result.isPresent()) {
            log.info("📧 CACHE STORE - Loaded user profile: email={}, name={}", email, result.get().getName());
        } else {
            log.warn("📧 CACHE STORE - User profile not found: email={}", email);
        }
        return result;
    }

    /**
     * Get users with most posts
     */
    @Cacheable(value = "top_content_creators", key = "#page + '_' + #size")
    public Page<ReadUserProfile> getTopContentCreators(int page, int size) {
        log.info("🏆 CACHE MISS - Loading top content creators from DB: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadUserProfile> result = readUserProfileRepository.findAllByOrderByPostsCountDesc(pageable);
        log.info("🏆 CACHE STORE - Loaded {} top content creators", result.getNumberOfElements());
        return result;
    }

    /**
     * Get users with most likes received
     */
    @Cacheable(value = "top_liked_users", key = "#page + '_' + #size")
    public Page<ReadUserProfile> getTopLikedUsers(int page, int size) {
        log.info("❤️ CACHE MISS - Loading top liked users from DB: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadUserProfile> result = readUserProfileRepository.findAllByOrderByTotalLikesReceivedDesc(pageable);
        log.info("❤️ CACHE STORE - Loaded {} top liked users", result.getNumberOfElements());
        return result;
    }

    /**
     * Get most active users
     */
    @Cacheable(value = "most_active_users", key = "#limit")
    public List<ReadUserProfile> getMostActiveUsers(int limit) {
        log.info("🔥 CACHE MISS - Loading most active users from DB: limit={}", limit);
        Pageable pageable = PageRequest.of(0, limit);
        List<ReadUserProfile> result = readUserProfileRepository.findMostActiveUsers(pageable);
        log.info("🔥 CACHE STORE - Loaded {} most active users", result.size());
        return result;
    }

    /**
     * Search users by name
     */
    public Page<ReadUserProfile> searchUsers(String searchTerm, int page, int size) {
        log.info("🔍 Searching users: term={}, page={}, size={}", searchTerm, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadUserProfile> result = readUserProfileRepository.searchByName(searchTerm, pageable);
        log.info("🔍 Found {} users matching: {}", result.getNumberOfElements(), searchTerm);
        return result;
    }

    /**
     * Get users with high post count
     */
    @Cacheable(value = "prolific_users", key = "#threshold")
    public List<ReadUserProfile> getProlificUsers(Long threshold) {
        log.info("📝 CACHE MISS - Loading prolific users from DB: threshold={}", threshold);
        List<ReadUserProfile> result = readUserProfileRepository.findByPostsCountGreaterThanOrderByPostsCountDesc(threshold);
        log.info("📝 CACHE STORE - Loaded {} prolific users with >{}posts", result.size(), threshold);
        return result;
    }
}