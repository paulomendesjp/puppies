package com.puppies.api.read.service;

import com.puppies.api.read.model.ReadUserProfile;
import com.puppies.api.read.repository.ReadUserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for QueryUserProfileService.
 * 
 * Tests user profile queries, caching behavior,
 * and read-optimized operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QueryUserProfileService Tests")
class QueryUserProfileServiceTest {

    @Mock
    private ReadUserProfileRepository readUserProfileRepository;

    @InjectMocks
    private QueryUserProfileService queryUserProfileService;

    private ReadUserProfile testUserProfile;
    private ReadUserProfile testUserProfile2;
    private List<ReadUserProfile> testUserProfiles;

    @BeforeEach
    void setUp() {
        testUserProfile = ReadUserProfile.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .postsCount(25L)
                .followersCount(150L)
                .followingCount(75L)
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();

        testUserProfile2 = ReadUserProfile.builder()
                .id(2L)
                .name("Jane Smith")
                .email("jane@example.com")
                .postsCount(50L)
                .followersCount(200L)
                .followingCount(100L)
                .createdAt(LocalDateTime.now().minusDays(20))
                .build();

        testUserProfiles = Arrays.asList(testUserProfile, testUserProfile2);
    }

    @Test
    @DisplayName("Should get user profile by ID when exists")
    void getUserProfile_WhenUserExists_ShouldReturnUserProfile() {
        // Given
        Long userId = 1L;
        when(readUserProfileRepository.findById(userId)).thenReturn(Optional.of(testUserProfile));

        // When
        Optional<ReadUserProfile> result = queryUserProfileService.getUserProfile(userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUserProfile);
        assertThat(result.get().getName()).isEqualTo("John Doe");
        assertThat(result.get().getEmail()).isEqualTo("john@example.com");

        verify(readUserProfileRepository).findById(userId);
    }

    @Test
    @DisplayName("Should return empty when user profile not found")
    void getUserProfile_WhenUserNotExists_ShouldReturnEmpty() {
        // Given
        Long userId = 999L;
        when(readUserProfileRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        Optional<ReadUserProfile> result = queryUserProfileService.getUserProfile(userId);

        // Then
        assertThat(result).isEmpty();

        verify(readUserProfileRepository).findById(userId);
    }

    @Test
    @DisplayName("Should get user profile by email when exists")
    void getUserProfileByEmail_WhenUserExists_ShouldReturnUserProfile() {
        // Given
        String email = "john@example.com";
        when(readUserProfileRepository.findByEmail(email)).thenReturn(Optional.of(testUserProfile));

        // When
        Optional<ReadUserProfile> result = queryUserProfileService.getUserProfileByEmail(email);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUserProfile);
        assertThat(result.get().getEmail()).isEqualTo(email);

        verify(readUserProfileRepository).findByEmail(email);
    }

    @Test
    @DisplayName("Should return empty when user profile not found by email")
    void getUserProfileByEmail_WhenUserNotExists_ShouldReturnEmpty() {
        // Given
        String email = "nonexistent@example.com";
        when(readUserProfileRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When
        Optional<ReadUserProfile> result = queryUserProfileService.getUserProfileByEmail(email);

        // Then
        assertThat(result).isEmpty();

        verify(readUserProfileRepository).findByEmail(email);
    }

    @Test
    @DisplayName("Should get most active users ordered by posts count")
    void getMostActiveUsers_ShouldReturnUsersOrderedByActivity() {
        // Given
        int limit = 10;
        Pageable pageable = PageRequest.of(0, limit);
        when(readUserProfileRepository.findMostActiveUsers(pageable)).thenReturn(testUserProfiles);

        // When
        List<ReadUserProfile> result = queryUserProfileService.getMostActiveUsers(limit);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(testUserProfiles);

        verify(readUserProfileRepository).findMostActiveUsers(pageable);
    }

    @Test
    @DisplayName("Should search users by name with pagination")
    void searchUsers_ShouldReturnMatchingUsers() {
        // Given
        String searchTerm = "John";
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadUserProfile> expectedPage = new PageImpl<>(List.of(testUserProfile), pageable, 1);
        
        when(readUserProfileRepository.searchByName(searchTerm, pageable)).thenReturn(expectedPage);

        // When
        Page<ReadUserProfile> result = queryUserProfileService.searchUsers(searchTerm, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).contains("John");
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(readUserProfileRepository).searchByName(searchTerm, pageable);
    }

    @Test
    @DisplayName("Should return empty results for search with no matches")
    void searchUsers_WithNoMatches_ShouldReturnEmptyPage() {
        // Given
        String searchTerm = "NonExistent";
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadUserProfile> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        
        when(readUserProfileRepository.searchByName(searchTerm, pageable)).thenReturn(emptyPage);

        // When
        Page<ReadUserProfile> result = queryUserProfileService.searchUsers(searchTerm, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);

        verify(readUserProfileRepository).searchByName(searchTerm, pageable);
    }

    @Test
    @DisplayName("Should get prolific users above threshold")
    void getProlificUsers_ShouldReturnUsersAboveThreshold() {
        // Given
        Long threshold = 20L;
        List<ReadUserProfile> prolificUsers = Arrays.asList(testUserProfile, testUserProfile2);
        
        when(readUserProfileRepository.findByPostsCountGreaterThanOrderByPostsCountDesc(threshold))
                .thenReturn(prolificUsers);

        // When
        List<ReadUserProfile> result = queryUserProfileService.getProlificUsers(threshold);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(prolificUsers);
        
        // Verify all users have posts count above threshold
        result.forEach(user -> assertThat(user.getPostsCount()).isGreaterThan(threshold));

        verify(readUserProfileRepository).findByPostsCountGreaterThanOrderByPostsCountDesc(threshold);
    }

    @Test
    @DisplayName("Should return empty list when no prolific users found")
    void getProlificUsers_WithHighThreshold_ShouldReturnEmptyList() {
        // Given
        Long highThreshold = 1000L;
        
        when(readUserProfileRepository.findByPostsCountGreaterThanOrderByPostsCountDesc(highThreshold))
                .thenReturn(List.of());

        // When
        List<ReadUserProfile> result = queryUserProfileService.getProlificUsers(highThreshold);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(readUserProfileRepository).findByPostsCountGreaterThanOrderByPostsCountDesc(highThreshold);
    }

    @Test
    @DisplayName("Should handle edge case with zero posts threshold")
    void getProlificUsers_WithZeroThreshold_ShouldReturnAllUsers() {
        // Given
        Long zeroThreshold = 0L;
        
        when(readUserProfileRepository.findByPostsCountGreaterThanOrderByPostsCountDesc(zeroThreshold))
                .thenReturn(testUserProfiles);

        // When
        List<ReadUserProfile> result = queryUserProfileService.getProlificUsers(zeroThreshold);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(testUserProfiles);

        verify(readUserProfileRepository).findByPostsCountGreaterThanOrderByPostsCountDesc(zeroThreshold);
    }

    @Test
    @DisplayName("Should handle negative limit by validating it")
    void getMostActiveUsers_WithNegativeLimit_ShouldValidateLimit() {
        // Given
        int negativeLimit = -1;
        
        // When & Then - Should throw IllegalArgumentException for invalid limit
        assertThrows(IllegalArgumentException.class, () -> {
            queryUserProfileService.getMostActiveUsers(negativeLimit);
        });
    }

    @Test
    @DisplayName("Should handle empty search term gracefully")
    void searchUsers_WithEmptySearchTerm_ShouldMakeRepositoryCall() {
        // Given
        String emptySearchTerm = "";
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadUserProfile> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        
        when(readUserProfileRepository.searchByName(emptySearchTerm, pageable)).thenReturn(emptyPage);

        // When
        Page<ReadUserProfile> result = queryUserProfileService.searchUsers(emptySearchTerm, page, size);

        // Then
        assertThat(result).isNotNull();
        verify(readUserProfileRepository).searchByName(emptySearchTerm, pageable);
    }

    @Test
    @DisplayName("Should verify user profile data integrity")
    void getUserProfile_ShouldReturnCompleteUserData() {
        // Given
        Long userId = 1L;
        when(readUserProfileRepository.findById(userId)).thenReturn(Optional.of(testUserProfile));

        // When
        Optional<ReadUserProfile> result = queryUserProfileService.getUserProfile(userId);

        // Then
        assertThat(result).isPresent();
        ReadUserProfile profile = result.get();
        
        assertThat(profile.getId()).isEqualTo(1L);
        assertThat(profile.getName()).isEqualTo("John Doe");
        assertThat(profile.getEmail()).isEqualTo("john@example.com");
        assertThat(profile.getPostsCount()).isEqualTo(25L);
        assertThat(profile.getFollowersCount()).isEqualTo(150L);
        assertThat(profile.getFollowingCount()).isEqualTo(75L);
        assertThat(profile.getCreatedAt()).isNotNull();
    }
}