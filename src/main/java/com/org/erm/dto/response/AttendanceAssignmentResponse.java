package com.org.erm.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AttendanceAssignmentResponse(
        Long allocationId,
        Long projectRequestId,
        String projectName,
        String projectCode,
        String allocationType,
        BigDecimal allocationPercent,
        LocalDate startDate,
        LocalDate endDate
) {
}
