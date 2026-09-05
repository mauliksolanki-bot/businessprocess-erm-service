package com.org.erm.dto.response;

import java.time.LocalDateTime;

public record UserMentionNotificationResponse(
        Long id,
        String actorUsername,
        String contextType,
        Long contextId,
        String message,
        String href,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
