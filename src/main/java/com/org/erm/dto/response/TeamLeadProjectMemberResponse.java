package com.org.erm.dto.response;

import java.math.BigDecimal;

public record TeamLeadProjectMemberResponse(
        Long employeeUserId,
        String employeeFullName,
        String employeeUsername,
        BigDecimal allocationPercent
) {
}
