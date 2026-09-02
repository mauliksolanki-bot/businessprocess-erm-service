package com.org.erm.controller;

import com.org.erm.dto.EmployeeDirectReportResponse;
import com.org.erm.dto.EmployeeResponse;
import com.org.erm.dto.EmployeeUpdateRequest;
import com.org.erm.dto.PagedResponse;
import com.org.erm.dto.OnboardingManagerOptionResponse;
import com.org.erm.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Employee directory and filtering")
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN','ROLE_CHRO','ROLE_HR_HEAD','ROLE_SENIOR_HR','ROLE_JUNIOR_HR')")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @Operation(summary = "Search employees")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employees fetched"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<PagedResponse<EmployeeResponse>> search(
            @RequestParam(name = "employeeName", required = false) String employeeName,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "department", required = false) String department,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "25") int size
    ) {
        return ResponseEntity.ok(employeeService.searchEmployees(employeeName, role, department, status, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee fetched"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @GetMapping("/{id}/direct-reports")
    @Operation(summary = "Get direct reports for an employee")
    public ResponseEntity<List<EmployeeDirectReportResponse>> getDirectReports(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getDirectReports(id));
    }

    @GetMapping("/{id}/replacement-options")
    @Operation(summary = "Get replacement manager options for a promoted employee")
    public ResponseEntity<List<OnboardingManagerOptionResponse>> getReplacementOptions(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getReplacementOptions(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee updated"),
            @ApiResponse(responseCode = "400", description = "Invalid employee payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<EmployeeResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody EmployeeUpdateRequest request,
                                                   Authentication authentication) {
        boolean seniorHr = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SENIOR_HR".equals(authority.getAuthority()));
        if (seniorHr) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Senior HR must submit employee update requests for approval"
            );
        }
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }
}