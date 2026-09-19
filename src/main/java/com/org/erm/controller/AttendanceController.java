package com.org.erm.controller;

import com.org.erm.dto.request.AttendanceTimesheetActionRequest;
import com.org.erm.dto.request.AttendanceTimesheetUpsertRequest;
import com.org.erm.dto.response.AttendanceApprovalItemResponse;
import com.org.erm.dto.response.AttendanceApproverVisibilityResponse;
import com.org.erm.dto.response.AttendanceWeekResponse;
import com.org.erm.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@Tag(name = "Attendance", description = "Weekly attendance and timesheet workflows")
@PreAuthorize("isAuthenticated()")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/weeks/current")
    @Operation(summary = "Load the current or selected weekly timesheet")
    public ResponseEntity<AttendanceWeekResponse> currentWeek(@RequestParam(value = "weekStartDate", required = false) LocalDate weekStartDate,
                                                              Authentication authentication) {
        return ResponseEntity.ok(attendanceService.getWeek(authentication.getName(), weekStartDate));
    }

    @PostMapping("/timesheets/save")
    @Operation(summary = "Save a weekly timesheet draft")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Timesheet saved"),
            @ApiResponse(responseCode = "400", description = "Invalid timesheet payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<AttendanceWeekResponse> save(@Valid @RequestBody AttendanceTimesheetUpsertRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(attendanceService.save(authentication.getName(), request));
    }

    @PostMapping("/timesheets/submit")
    @Operation(summary = "Submit a weekly timesheet")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Timesheet submitted"),
            @ApiResponse(responseCode = "400", description = "Invalid timesheet payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<AttendanceWeekResponse> submit(@Valid @RequestBody AttendanceTimesheetUpsertRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(attendanceService.submit(authentication.getName(), request));
    }

    @GetMapping("/approver-visibility")
    @Operation(summary = "Check whether attendance approvals should be shown")
    public ResponseEntity<AttendanceApproverVisibilityResponse> approverVisibility(Authentication authentication) {
        return ResponseEntity.ok(attendanceService.approverVisibility(authentication.getName()));
    }

    @GetMapping("/approvals")
    @Operation(summary = "List pending billable timesheet approvals")
    public ResponseEntity<List<AttendanceApprovalItemResponse>> approvals(Authentication authentication) {
        return ResponseEntity.ok(attendanceService.approvals(authentication.getName()));
    }

    @PatchMapping("/timesheets/{id}/actions")
    @Operation(summary = "Approve or reject a timesheet")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Timesheet actioned"),
            @ApiResponse(responseCode = "400", description = "Invalid timesheet action"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Timesheet not found")
    })
    public ResponseEntity<AttendanceWeekResponse> action(@PathVariable("id") Long id,
                                                         @Valid @RequestBody AttendanceTimesheetActionRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.status(HttpStatus.OK).body(attendanceService.action(id, request, authentication.getName()));
    }
}
