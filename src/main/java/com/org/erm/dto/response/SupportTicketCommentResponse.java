package com.org.erm.dto.response;

import java.time.LocalDateTime;

public record SupportTicketCommentResponse(
        Long id,
        String actorUsername,
        String actionType,
        String commentText,
        LocalDateTime createdAt
) {
}
