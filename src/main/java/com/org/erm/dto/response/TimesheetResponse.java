package com.org.erm.dto.response;

import com.org.erm.model.TimesheetStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TimesheetResponse(
        Long id,
        String timesheetCode,
        Long employeeUserId,
        String employeeUsername,
        String employeeFullName,
        Long reportingManagerUserId,
        String reportingManagerUsername,
        String reportingManagerFullName,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        TimesheetStatus status,
        String submissionComment,
        String managerActionByUsername,
        LocalDateTime managerActionAt,
        String managerComment,
        BigDecimal totalHours,
        BigDecimal billableHours,
        BigDecimal nonBillableHours,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TimesheetEntryResponse> rows
) {
}
