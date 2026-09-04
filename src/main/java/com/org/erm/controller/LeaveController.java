package com.org.erm.controller;

import com.org.erm.dto.request.LeaveApplyRequest;
import com.org.erm.dto.request.LeaveActionRequest;
import com.org.erm.dto.response.LeaveApproverVisibilityResponse;
import com.org.erm.dto.response.LeavePolicyResponse;
import com.org.erm.dto.response.LeaveRequestResponse;
import com.org.erm.service.LeaveService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@Tag(name = "Leaves", description = "Leave applications for all authenticated users")
@PreAuthorize("isAuthenticated()")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/policies")
    @Operation(summary = "List enabled leave policies")
    public ResponseEntity<List<LeavePolicyResponse>> policies() {
        return ResponseEntity.ok(leaveService.listEnabledPolicies());
    }

    @PostMapping("/apply")
    @Operation(summary = "Apply leave")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Leave request created"),
            @ApiResponse(responseCode = "400", description = "Invalid leave payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<LeaveRequestResponse> apply(@Valid @RequestBody LeaveApplyRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.applyLeave(request, authentication.getName()));
    }

    @GetMapping("/my-requests")
    @Operation(summary = "List my leave requests")
    public ResponseEntity<List<LeaveRequestResponse>> myRequests(Authentication authentication) {
        return ResponseEntity.ok(leaveService.myRequests(authentication.getName()));
    }

    @GetMapping("/approvals")
    @Operation(summary = "List leave requests assigned to me for approval")
    public ResponseEntity<List<LeaveRequestResponse>> approvals(Authentication authentication) {
        return ResponseEntity.ok(leaveService.approverRequests(authentication.getName()));
    }

    @GetMapping("/approver-visibility")
    @Operation(summary = "Check whether approver requests tab should be shown for caller")
    public ResponseEntity<LeaveApproverVisibilityResponse> approverVisibility(Authentication authentication) {
        return ResponseEntity.ok(new LeaveApproverVisibilityResponse(leaveService.hasReportees(authentication.getName())));
    }

    @PatchMapping("/{id}/actions")
    @Operation(summary = "Approve or reject leave request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leave request actioned"),
            @ApiResponse(responseCode = "400", description = "Invalid leave action"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Leave request not found")
    })
    public ResponseEntity<LeaveRequestResponse> action(@PathVariable("id") Long id,
                                                       @Valid @RequestBody LeaveActionRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(leaveService.takeAction(id, request, authentication.getName()));
    }
}
