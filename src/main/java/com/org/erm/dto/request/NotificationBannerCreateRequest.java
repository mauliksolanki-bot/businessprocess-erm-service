package com.org.erm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record NotificationBannerCreateRequest(
        @NotBlank(message = "Notification title is required")
        @Size(max = 150, message = "Notification title must be 150 characters or fewer")
        String title,

        @NotBlank(message = "Notification message is required")
        @Size(max = 2000, message = "Notification message must be 2,000 characters or fewer")
        String message,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotBlank(message = "Notification type is required")
        @Pattern(regexp = "Informational|Urgent|Low Priority", message = "Select a valid notification type")
        String notificationType
) {
}
