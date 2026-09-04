package com.org.erm.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SelfProjectAssignmentResponse(
        Long allocationId,
        String allocationCode,
        Long projectRequestId,
        String projectName,
        String projectCode,
        String allocationType,
        BigDecimal allocationPercent,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        LocalDateTime updatedAt
) {
}
