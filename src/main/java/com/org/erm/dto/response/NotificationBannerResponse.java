package com.org.erm.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NotificationBannerResponse(
        Long id,
        String title,
        String message,
        LocalDate startDate,
        LocalDate endDate,
        String notificationType,
        String createdByUsername,
        boolean active,
        LocalDateTime createdAt
) {
}
