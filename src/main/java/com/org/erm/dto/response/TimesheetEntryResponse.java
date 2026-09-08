package com.org.erm.dto.response;

import com.org.erm.model.TimesheetWorkType;

import java.math.BigDecimal;

public record TimesheetEntryResponse(
        Long id,
        Integer sortOrder,
        TimesheetWorkType workType,
        Long projectAllocationId,
        String projectName,
        String projectCode,
        String taskName,
        BigDecimal mondayHours,
        BigDecimal tuesdayHours,
        BigDecimal wednesdayHours,
        BigDecimal thursdayHours,
        BigDecimal fridayHours,
        BigDecimal saturdayHours,
        BigDecimal sundayHours,
        BigDecimal totalHours
) {
}
