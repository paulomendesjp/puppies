package com.puppies.api.command.controller;

import com.puppies.api.command.dto.CreatePostRequest;
import com.puppies.api.command.dto.CreatePostResponse;
import com.puppies.api.command.service.PostCommandService;
import com.puppies.api.common.constants.ApiConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for post command operations.
 * Handles post creation, likes, and other write operations.
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {ApiConstants.CorsOrigins.LOCALHOST_3000, ApiConstants.CorsOrigins.LOCALHOST_8000})
public class PostCommandController {

    private final PostCommandService postCommandService;

    /**
     * Create a new post with image upload.
     * 
     * POST /api/posts
     * 
     * @param image The image file (required)
     * @param textContent The text content (optional)
     * @param authentication The authenticated user
     * @return Created post information
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<CreatePostResponse> createPost(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "textContent", required = false) String textContent,
            Authentication authentication) {
        
        CreatePostRequest request = CreatePostRequest.builder()
                .textContent(textContent)
                .build();

        CreatePostResponse response = postCommandService.createPost(
                request, image, authentication.getName());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Like a post.
     * 
     * POST /api/posts/{postId}/like
     * 
     * @param postId The ID of the post to like
     * @param authentication The authenticated user
     * @return Success response
     */
    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> likePost(@PathVariable Long postId, Authentication authentication) {
        postCommandService.likePost(postId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * Unlike a post.
     * 
     * DELETE /api/posts/{postId}/like
     * 
     * @param postId The ID of the post to unlike
     * @param authentication The authenticated user
     * @return Success response
     */
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<Void> unlikePost(@PathVariable Long postId, Authentication authentication) {
        postCommandService.unlikePost(postId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}