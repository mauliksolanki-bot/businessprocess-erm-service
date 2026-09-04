package com.org.erm.controller;

import com.org.erm.dto.request.EmployeeDesignationUpdateRequestCreateRequest;
import com.org.erm.dto.response.EmployeeDesignationUpdateRequestResponse;
import com.org.erm.dto.request.OnboardingActionRequest;
import com.org.erm.service.EmployeeDataRequestService;
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

import java.util.List;

@RestController
@RequestMapping("/api/employee-data/requests")
@Tag(name = "Employee Data Requests", description = "Designation update workflow for employees")
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_CHRO','ROLE_HR_HEAD','ROLE_SENIOR_HR')")
public class EmployeeDataRequestController {

    private final EmployeeDataRequestService employeeDataRequestService;

    public EmployeeDataRequestController(EmployeeDataRequestService employeeDataRequestService) {
        this.employeeDataRequestService = employeeDataRequestService;
    }

    @PostMapping
    @Operation(summary = "Create employee designation update request")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Request created"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<EmployeeDesignationUpdateRequestResponse> create(
            @Valid @RequestBody EmployeeDesignationUpdateRequestCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeDataRequestService.create(request, authentication));
    }

    @GetMapping
    @Operation(summary = "List employee data requests")
    public ResponseEntity<List<EmployeeDesignationUpdateRequestResponse>> list(
            @RequestParam(name = "workflowStage", required = false) String workflowStage) {
        return ResponseEntity.ok(employeeDataRequestService.list(workflowStage));
    }

    @PatchMapping("/{id}/actions")
    @Operation(summary = "Approve or reject employee data request for current workflow step")
    public ResponseEntity<EmployeeDesignationUpdateRequestResponse> takeAction(
            @PathVariable Long id,
            @Valid @RequestBody OnboardingActionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(employeeDataRequestService.takeAction(id, request, authentication));
    }
}
