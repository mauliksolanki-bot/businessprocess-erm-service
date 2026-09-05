package com.org.erm.dto.request;

import com.org.erm.model.SupportTicketStatus;
import jakarta.validation.constraints.Size;

public record SupportTicketDetailsUpdateRequest(
        @Size(max = 100, message = "Queue code must be at most 100 characters")
        String queueCode,

        Long assigneeUserId,

        @Size(max = 20, message = "Impact must be at most 20 characters")
        String impactLevel,

        @Size(max = 20, message = "Urgency must be at most 20 characters")
        String urgencyLevel,

        SupportTicketStatus status,

        @Size(max = 1000, message = "Closure details must be at most 1000 characters")
        String closureDetails
) {
}
