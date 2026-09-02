package com.org.erm.controller;

import com.org.erm.dto.AssignRolesRequest;
import com.org.erm.dto.EmployeeResponse;
import com.org.erm.dto.PagedResponse;
import com.org.erm.dto.RoleSummaryResponse;
import com.org.erm.service.RoleAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/role-audit")
@Tag(name = "Role Audit", description = "Super admin role audit and assignment")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class RoleAuditController {

    private final RoleAuditService roleAuditService;

    public RoleAuditController(RoleAuditService roleAuditService) {
        this.roleAuditService = roleAuditService;
    }

    @GetMapping("/employees")
    @Operation(summary = "Search employees for role audit")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employees fetched"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<PagedResponse<EmployeeResponse>> searchEmployees(
            @RequestParam(name = "employeeName", required = false) String employeeName,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "department", required = false) String department,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "25") int size
    ) {
        return ResponseEntity.ok(roleAuditService.searchEmployees(employeeName, role, department, status, page, size));
    }

    @GetMapping("/employees/{employeeId}")
    @Operation(summary = "Get employee details for role audit")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee fetched"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(roleAuditService.getEmployeeById(employeeId));
    }

    @GetMapping("/roles")
    @Operation(summary = "List all roles")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles fetched"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<RoleSummaryResponse>> getRoles() {
        return ResponseEntity.ok(roleAuditService.getAllRoles());
    }

    @PostMapping("/employees/{employeeId}/roles")
    @Operation(summary = "Assign additional roles to employee")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles assigned"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<EmployeeResponse> assignRoles(@PathVariable Long employeeId,
                                                        @Valid @RequestBody AssignRolesRequest request) {
        return ResponseEntity.ok(roleAuditService.assignRoles(employeeId, request));
    }
}
