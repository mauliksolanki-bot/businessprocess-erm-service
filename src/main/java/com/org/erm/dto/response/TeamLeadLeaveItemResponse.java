package com.org.erm.dto.response;

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
