package com.org.erm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestCommentRequest(
        @NotBlank(message = "Comment is required")
        @Size(max = 500, message = "Comment must be at most 500 characters")
        String comment
) {
}
