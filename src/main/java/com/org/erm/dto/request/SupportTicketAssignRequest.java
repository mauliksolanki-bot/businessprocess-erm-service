package com.org.erm.dto.request;

import jakarta.validation.constraints.Size;

public record SupportTicketAssignRequest(
        @Size(max = 100, message = "Queue code must be at most 100 characters")
        String queueCode,

        Long assigneeUserId
) {
}
