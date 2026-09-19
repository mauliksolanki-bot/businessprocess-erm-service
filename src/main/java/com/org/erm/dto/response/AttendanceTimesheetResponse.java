package com.org.erm.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AttendanceTimesheetResponse(
        Long id,
        Long employeeUserId,
        String employeeUsername,
        String employeeFullName,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        Long approverManagerUserId,
        String approverManagerUsername,
        String approverManagerFullName,
        String timesheetStatus,
        boolean approvalRequired,
        LocalDateTime submittedAt,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        String approverComment,
        boolean approvalReset,
        List<AttendanceDayResponse> days
) {
}
