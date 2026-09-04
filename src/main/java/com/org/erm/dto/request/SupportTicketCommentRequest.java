package com.org.erm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportTicketCommentRequest(
        @NotBlank(message = "Comment is required")
        @Size(max = 1000, message = "Comment must be at most 1000 characters")
        String comment
) {
}
