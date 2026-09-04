package com.org.erm.dto.response;

public record SupportCategoryOptionResponse(
        String categoryCode,
        String categoryTitle,
        String ticketType,
        String parentCategoryCode
) {
}
