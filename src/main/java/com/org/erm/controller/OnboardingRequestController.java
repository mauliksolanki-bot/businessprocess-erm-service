package com.org.erm.controller;

import com.org.erm.dto.request.OnboardingActionRequest;
import com.org.erm.dto.response.OnboardingDesignationOptionResponse;
import com.org.erm.dto.response.OnboardingManagerOptionsResponse;
import com.org.erm.dto.request.OnboardingRequestCreateRequest;
import com.org.erm.dto.response.OnboardingRequestResponse;
import com.org.erm.dto.response.PagedResponse;
import com.org.erm.dto.request.RequestCommentRequest;
import com.org.erm.service.OnboardingRequestService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding-requests")
@Tag(name = "Onboarding Requests", description = "HR onboarding request management")
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_CHRO','ROLE_ADMIN','ROLE_CEO','ROLE_CTO','ROLE_HR_HEAD','ROLE_SENIOR_HR','ROLE_JUNIOR_HR')")
public class OnboardingRequestController {

    private final OnboardingRequestService onboardingRequestService;

    public OnboardingRequestController(OnboardingRequestService onboardingRequestService) {
        this.onboardingRequestService = onboardingRequestService;
    }

    @PostMapping
    @Operation(summary = "Create an onboarding request")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Onboarding request created"),
            @ApiResponse(responseCode = "400", description = "Invalid onboarding request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<OnboardingRequestResponse> create(@Valid @RequestBody OnboardingRequestCreateRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onboardingRequestService.create(request, authentication));
    }

    @GetMapping
    @Operation(summary = "List onboarding requests")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Onboarding requests fetched"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<PagedResponse<OnboardingRequestResponse>> list(
            @RequestParam(name = "workflowStage", required = false) String workflowStage,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "25") int size) {
        return ResponseEntity.ok(onboardingRequestService.list(workflowStage, page, size));
    }

    @GetMapping("/designation-options")
    @Operation(summary = "List onboarding designation options and their reporting role")
    public ResponseEntity<List<OnboardingDesignationOptionResponse>> designationOptions() {
        return ResponseEntity.ok(onboardingRequestService.listDesignationOptions());
    }

    @GetMapping("/manager-options")
    @Operation(summary = "List reporting manager options for the selected designation")
    public ResponseEntity<OnboardingManagerOptionsResponse> managerOptions(
            @RequestParam(name = "designationRoleName") String designationRoleName) {
        return ResponseEntity.ok(onboardingRequestService.getManagerOptions(designationRoleName));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get onboarding request by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Onboarding request fetched"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Onboarding request not found")
    })
    public ResponseEntity<OnboardingRequestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(onboardingRequestService.getById(id));
    }

    @PatchMapping("/{id}/actions")
    @Operation(summary = "Approve or reject onboarding request for current workflow step")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Onboarding request updated"),
            @ApiResponse(responseCode = "400", description = "Invalid workflow action"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Onboarding request not found")
    })
    public ResponseEntity<OnboardingRequestResponse> takeAction(@PathVariable Long id,
                                                                @Valid @RequestBody OnboardingActionRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(onboardingRequestService.takeAction(id, request, authentication));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a comment to an onboarding request")
    public ResponseEntity<OnboardingRequestResponse> addComment(@PathVariable Long id,
                                                                @Valid @RequestBody RequestCommentRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(onboardingRequestService.addComment(id, request, authentication));
    }

    @PatchMapping("/{id}/resubmit")
    @Operation(summary = "Resubmit an onboarding request after refer back")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Onboarding request resubmitted"),
            @ApiResponse(responseCode = "400", description = "Invalid workflow state"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Onboarding request not found")
    })
    public ResponseEntity<OnboardingRequestResponse> resubmit(@PathVariable Long id,
                                                              @Valid @RequestBody OnboardingRequestCreateRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(onboardingRequestService.resubmitAfterReferBack(id, request, authentication));
    }

    @PatchMapping("/{id}/reinitiate")
    @Operation(summary = "Re-initiate a rejected onboarding request by creator")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Onboarding request re-initiated"),
            @ApiResponse(responseCode = "400", description = "Invalid workflow state"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Onboarding request not found")
    })
    public ResponseEntity<OnboardingRequestResponse> reInitiate(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(onboardingRequestService.reInitiate(id, authentication));
    }

    @PostMapping("/{id}/remind")
    @Operation(summary = "Send a reminder for a pending onboarding request (requester only, after 8h pending)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reminder sent"),
            @ApiResponse(responseCode = "400", description = "Reminder not allowed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Onboarding request not found"),
            @ApiResponse(responseCode = "429", description = "Too many reminders")
    })
    public ResponseEntity<OnboardingRequestResponse> sendReminder(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(onboardingRequestService.sendReminder(id, authentication));
    }

    @GetMapping("/pending-approvals")
    @Operation(summary = "Get onboarding requests pending the caller's approval")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending approval requests"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<java.util.List<OnboardingRequestResponse>> getPendingApprovals(Authentication authentication) {
        return ResponseEntity.ok(onboardingRequestService.getPendingRequestsForApprover(authentication));
    }
}
