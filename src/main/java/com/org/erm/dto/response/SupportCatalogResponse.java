package com.org.erm.dto.response;

import java.util.List;

public record SupportCatalogResponse(
        List<String> ticketTypes,
        List<String> impactLevels,
        List<String> urgencyLevels,
        List<SupportCategoryOptionResponse> categories,
        List<SupportQueueSummaryResponse> queues
) {
}
