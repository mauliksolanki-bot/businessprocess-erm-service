package com.org.erm.dto.response;

import java.time.LocalDateTime;

public record LeavePolicyResponse(
        Long id,
        String leaveCategory,
        String displayName,
        Integer maxDaysPerYear,
        Boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
