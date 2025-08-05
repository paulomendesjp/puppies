package com.puppies.api.command.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for post creation requests.
 * The image is handled as a MultipartFile in the controller.
 * For demo posts, images are downloaded and converted to MultipartFile.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePostRequest {

    @Size(max = 2000, message = "Text content must not exceed 2000 characters")
    private String textContent;
}