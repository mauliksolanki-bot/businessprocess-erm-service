package com.org.erm.dto.response;

public record SupportQueueSummaryResponse(
        Long queueId,
        String queueCode,
        String queueTitle,
        String queueType,
        long memberCount,
        long openTicketCount
) {
}
