package com.org.erm.controller;

import com.org.erm.dto.request.EmployeeProfileUpdateRequestCreateRequest;
import com.org.erm.dto.response.EmployeeProfileUpdateRequestResponse;
import com.org.erm.dto.request.OnboardingActionRequest;
import com.org.erm.dto.response.PagedResponse;
import com.org.erm.dto.request.RequestCancellationRequest;
import com.org.erm.dto.request.RequestCommentRequest;
import com.org.erm.service.EmployeeProfileUpdateRequestService;
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

@RestController
@RequestMapping("/api/employee-profile-update-requests")
@Tag(name = "Employee Profile Update Requests", description = "Approval workflow for employee edit requests")
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_CHRO','ROLE_HR_HEAD','ROLE_SENIOR_HR')")
public class EmployeeProfileUpdateRequestController {

    private final EmployeeProfileUpdateRequestService requestService;

    public EmployeeProfileUpdateRequestController(EmployeeProfileUpdateRequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping("/pending-check")
    public ResponseEntity<java.util.Map<String, Boolean>> pendingCheck(
            @RequestParam(name = "employeeUserId") Long employeeUserId) {
        boolean pending = requestService.hasPendingRequest(employeeUserId);
        return ResponseEntity.ok(java.util.Map.of("hasPendingRequest", pending));
    }

    @PostMapping
    public ResponseEntity<EmployeeProfileUpdateRequestResponse> create(
            @Valid @RequestBody EmployeeProfileUpdateRequestCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requestService.create(request, authentication));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<EmployeeProfileUpdateRequestResponse>> list(
            @RequestParam(name = "workflowStage", required = false) String workflowStage,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "25") int size) {
        return ResponseEntity.ok(requestService.list(workflowStage, page, size));
    }

    @PatchMapping("/{id}/actions")
    public ResponseEntity<EmployeeProfileUpdateRequestResponse> takeAction(
            @PathVariable Long id,
            @Valid @RequestBody OnboardingActionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(requestService.takeAction(id, request, authentication));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<EmployeeProfileUpdateRequestResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody RequestCancellationRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(requestService.cancelRequest(id, request, authentication));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<EmployeeProfileUpdateRequestResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody RequestCommentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(requestService.addComment(id, request, authentication));
    }
}
