package com.org.erm.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveRequestResponse(
        Long id,
        Long employeeUserId,
        String employeeUsername,
        String employeeFullName,
        Long approverManagerUserId,
        String approverManagerUsername,
        String approverManagerFullName,
        String leaveCategory,
        LocalDate startDate,
        LocalDate endDate,
        Integer requestedDays,
        String reason,
        String requestStatus,
        String approverComment,
        LocalDateTime approverActionAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
