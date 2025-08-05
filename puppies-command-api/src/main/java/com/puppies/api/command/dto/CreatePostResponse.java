package com.puppies.api.command.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for post creation responses.
 * Returns basic post information after successful creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePostResponse {

    private Long id;
    private String authorName;
    private String imageUrl;
    private String textContent;
    private LocalDateTime createdAt;
    private String message;

    public static CreatePostResponse from(Long id, String authorName, String imageUrl, 
                                        String textContent, LocalDateTime createdAt) {
        return CreatePostResponse.builder()
                .id(id)
                .authorName(authorName)
                .imageUrl(imageUrl)
                .textContent(textContent)
                .createdAt(createdAt)
                .message("Post created successfully")
                .build();
    }
}