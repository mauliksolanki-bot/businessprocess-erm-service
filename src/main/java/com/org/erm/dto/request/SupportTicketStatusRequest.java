package com.org.erm.dto.request;

import com.org.erm.model.SupportTicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SupportTicketStatusRequest(
        @NotNull(message = "Status is required")
        SupportTicketStatus status,

        @Size(max = 1000, message = "Comment must be at most 1000 characters")
        String comment
) {
}
