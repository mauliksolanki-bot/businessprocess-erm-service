package com.org.erm.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UserAssignedProjectResponse(
        Long allocationId,
        String projectName,
        String projectCode,
        BigDecimal allocationPercent,
        LocalDate allocationEndDate
) {
}
