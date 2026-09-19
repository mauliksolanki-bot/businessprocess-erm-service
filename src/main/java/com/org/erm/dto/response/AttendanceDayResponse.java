package com.org.erm.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AttendanceDayResponse(
        LocalDate workDate,
        String dayLabel,
        boolean weekend,
        boolean leaveDay,
        String leaveLabel,
        BigDecimal billableHours,
        BigDecimal nonBillableHours,
        Long billableProjectAllocationId,
        String billableProjectName,
        String billableProjectCode,
        Long nonBillableProjectAllocationId,
        String nonBillableProjectName,
        String nonBillableProjectCode,
        boolean editable
) {
}
