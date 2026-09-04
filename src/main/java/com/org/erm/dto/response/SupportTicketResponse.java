package com.org.erm.dto.response;

import com.org.erm.model.SupportPriority;
import com.org.erm.model.SupportTicketStatus;
import com.org.erm.model.SupportTicketType;

import java.time.LocalDateTime;
import java.util.List;

public record SupportTicketResponse(
        Long id,
        String ticketNumber,
        SupportTicketType ticketType,
        String categoryCode,
        String categoryTitle,
        String subcategoryCode,
        String subcategoryTitle,
        String impactLevel,
        String urgencyLevel,
        SupportPriority priorityCode,
        Long queueId,
        String queueCode,
        String queueTitle,
        Long assigneeUserId,
        String assigneeUsername,
        String assigneeFullName,
        SupportTicketStatus status,
        String source,
        String shortDescription,
        String description,
        boolean securityIncident,
        LocalDateTime responseDueAt,
        LocalDateTime resolutionDueAt,
        LocalDateTime firstResponseAt,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt,
        String createdByUsername,
        String updatedByUsername,
        List<SupportTicketCommentResponse> comments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
