package com.org.erm.dto;

import java.time.LocalDate;

public record TeamLeadLeaveItemResponse(
        Long leaveRequestId,
        Long employeeUserId,
        String employeeFullName,
        String employeeUsername,
        String leaveCategory,
        LocalDate startDate,
        LocalDate endDate,
        Integer requestedDays,
        String requestStatus
) {
}
