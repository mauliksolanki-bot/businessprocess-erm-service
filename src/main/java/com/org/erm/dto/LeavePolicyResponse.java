package com.org.erm.dto;

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
