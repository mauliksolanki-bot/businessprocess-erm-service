package com.org.erm.controller;

import com.org.erm.dto.response.PagedResponse;
import com.org.erm.dto.request.ProjectAllocationActionRequest;
import com.org.erm.dto.request.ProjectAllocationBatchCreateRequest;
import com.org.erm.dto.request.ProjectAllocationCreateRequest;
import com.org.erm.dto.request.ProjectAllocationBulkActionRequest;
import com.org.erm.dto.response.ProjectAllocationEmployeeOptionResponse;
import com.org.erm.dto.request.ProjectAllocationManageRequest;
import com.org.erm.dto.response.ProjectAllocationProjectOptionResponse;
import com.org.erm.dto.response.ProjectAllocationResponse;
import com.org.erm.dto.request.RequestCommentRequest;
import com.org.erm.service.ProjectAllocationService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/project-allocations")
@Tag(name = "Project Allocations", description = "Project allocation workflow")
@PreAuthorize("hasAnyAuthority('ROLE_PROJECT_MANAGER','ROLE_TEAM_LEAD','ROLE_IT_SUPPORT_MANAGER','ROLE_IT_SUPPORT_LEAD','ROLE_DELIVERY_MANAGER','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
public class ProjectAllocationController {

    private final ProjectAllocationService allocationService;

    public ProjectAllocationController(ProjectAllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @PostMapping
    @Operation(summary = "Create project allocation request")
    public ResponseEntity<List<ProjectAllocationResponse>> create(@Valid @RequestBody ProjectAllocationBatchCreateRequest request,
                                                                  Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(allocationService.create(request, authentication));
    }

    @GetMapping
    @Operation(summary = "List project allocations")
    public ResponseEntity<PagedResponse<ProjectAllocationResponse>> list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "25") int size,
            Authentication authentication) {
        return ResponseEntity.ok(allocationService.list(status, query, page, size, authentication));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project allocation by id")
    public ResponseEntity<ProjectAllocationResponse> getById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(allocationService.getById(id, authentication));
    }

    @PatchMapping("/{id}/actions")
    @Operation(summary = "Approve/reject/refer-back allocation")
    public ResponseEntity<ProjectAllocationResponse> takeAction(@PathVariable Long id,
                                                                @Valid @RequestBody ProjectAllocationActionRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(allocationService.takeAction(id, request, authentication));
    }

    @PatchMapping("/bulk-actions")
    @Operation(summary = "Bulk approve/reject/refer-back allocations")
    public ResponseEntity<List<ProjectAllocationResponse>> takeBulkAction(@Valid @RequestBody ProjectAllocationBulkActionRequest request,
                                                                          Authentication authentication) {
        return ResponseEntity.ok(allocationService.takeBulkAction(request, authentication));
    }

    @PatchMapping("/{id}/resubmit")
    @Operation(summary = "Resubmit refer-back allocation")
    public ResponseEntity<ProjectAllocationResponse> resubmit(@PathVariable Long id,
                                                              @Valid @RequestBody ProjectAllocationCreateRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(allocationService.resubmit(id, request, authentication));
    }

    @PatchMapping("/{id}/manage")
    @Operation(summary = "Manage active allocation (extend/reduce/release)")
    public ResponseEntity<ProjectAllocationResponse> manage(@PathVariable Long id,
                                                            @Valid @RequestBody ProjectAllocationManageRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(allocationService.manage(id, request, authentication));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a comment to a project allocation")
    public ResponseEntity<ProjectAllocationResponse> addComment(@PathVariable Long id,
                                                                @Valid @RequestBody RequestCommentRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(allocationService.addComment(id, request, authentication));
    }

    @GetMapping("/pending-approvals")
    @Operation(summary = "List pending allocations for delivery manager")
    public ResponseEntity<List<ProjectAllocationResponse>> pendingApprovals(Authentication authentication) {
        return ResponseEntity.ok(allocationService.pendingApprovals(authentication));
    }

    @GetMapping("/project-options")
    @Operation(summary = "List approved project options for allocation")
    public ResponseEntity<List<ProjectAllocationProjectOptionResponse>> projectOptions(Authentication authentication) {
        return ResponseEntity.ok(allocationService.projectOptions(authentication));
    }

    @GetMapping("/employee-options")
    @Operation(summary = "List active employee options for allocation")
    public ResponseEntity<List<ProjectAllocationEmployeeOptionResponse>> employeeOptions(Authentication authentication) {
        return ResponseEntity.ok(allocationService.employeeOptions(authentication));
    }
}
