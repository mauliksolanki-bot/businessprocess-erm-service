package com.org.erm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportTicketCreateRequest(
        @NotBlank(message = "Ticket type is required")
        @Size(max = 50, message = "Ticket type must be at most 50 characters")
        String ticketType,

        @NotBlank(message = "Category is required")
        @Size(max = 100, message = "Category must be at most 100 characters")
        String categoryCode,

        @Size(max = 100, message = "Subcategory must be at most 100 characters")
        String subcategoryCode,

        @NotBlank(message = "Impact is required")
        @Size(max = 20, message = "Impact must be at most 20 characters")
        String impactLevel,

        @NotBlank(message = "Urgency is required")
        @Size(max = 20, message = "Urgency must be at most 20 characters")
        String urgencyLevel,

        @NotBlank(message = "Short description is required")
        @Size(max = 255, message = "Short description must be at most 255 characters")
        String shortDescription,

        @NotBlank(message = "Description is required")
        String description,

        @Size(max = 50, message = "Source must be at most 50 characters")
        String source,

        // Optional assignee user id to assign the ticket at creation time
        Long assigneeUserId
) {
}
