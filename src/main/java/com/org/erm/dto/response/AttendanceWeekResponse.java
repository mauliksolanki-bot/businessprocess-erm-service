package com.org.erm.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AttendanceWeekResponse(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        LocalDate editableUntil,
        boolean editable,
        boolean hasBillableAssignments,
        boolean hasReportees,
        AttendanceTimesheetResponse timesheet,
        List<AttendanceDayResponse> days,
        List<AttendanceAssignmentResponse> billableAssignments,
        List<AttendanceAssignmentResponse> nonBillableAssignments,
        List<AttendanceApprovalItemResponse> pendingApprovals
) {
}
