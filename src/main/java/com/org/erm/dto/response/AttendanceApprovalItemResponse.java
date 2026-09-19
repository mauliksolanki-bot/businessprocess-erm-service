package com.org.erm.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceApprovalItemResponse(
        Long id,
        Long employeeUserId,
        String employeeUsername,
        String employeeFullName,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        BigDecimal billableHours,
        BigDecimal nonBillableHours,
        String timesheetStatus,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt
) {
}
