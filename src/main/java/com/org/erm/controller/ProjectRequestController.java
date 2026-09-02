package com.org.erm.controller;

import com.org.erm.dto.OnboardingActionRequest;
import com.org.erm.dto.ManagedProjectResponse;
import com.org.erm.dto.PagedResponse;
import com.org.erm.dto.ProjectChangeRequestCreateRequest;
import com.org.erm.dto.ProjectChangeRequestResponse;
import com.org.erm.dto.ProjectManagerOptionResponse;
import com.org.erm.dto.ProjectRequestCreateRequest;
import com.org.erm.dto.ProjectRequestResponse;
import com.org.erm.dto.RequestCommentRequest;
import com.org.erm.service.ProjectChangeRequestService;
import com.org.erm.service.ProjectRequestService;
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
@RequestMapping("/api/project-requests")
@Tag(name = "Project Requests", description = "Project creation and approval workflow")
@PreAuthorize("hasAnyAuthority('ROLE_PROJECT_MANAGER','ROLE_TEAM_LEAD','ROLE_DELIVERY_MANAGER','ROLE_PROJECT_OWNER','ROLE_DIRECTOR','ROLE_CTO','ROLE_SUPER_ADMIN','ROLE_ADMIN','ROLE_IT_SUPPORT_LEAD','ROLE_IT_SUPPORT_MANAGER')")
public class ProjectRequestController {

    private final ProjectRequestService projectRequestService;
    private final ProjectChangeRequestService projectChangeRequestService;

    public ProjectRequestController(ProjectRequestService projectRequestService,
                                    ProjectChangeRequestService projectChangeRequestService) {
        this.projectRequestService = projectRequestService;
        this.projectChangeRequestService = projectChangeRequestService;
    }

    @PostMapping
    @Operation(summary = "Create project request")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Project request created"),
            @ApiResponse(responseCode = "400", description = "Invalid payload"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ProjectRequestResponse> create(@Valid @RequestBody ProjectRequestCreateRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectRequestService.create(request, authentication));
    }

    @GetMapping
    @Operation(summary = "List project requests")
    public ResponseEntity<PagedResponse<ProjectRequestResponse>> list(
            @RequestParam(name = "workflowStage", required = false) String workflowStage,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "25") int size,
            Authentication authentication) {
        return ResponseEntity.ok(projectRequestService.list(workflowStage, query, page, size, authentication));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project request by id")
    public ResponseEntity<ProjectRequestResponse> getById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(projectRequestService.getById(id, authentication));
    }

    @PatchMapping("/{id}/actions")
    @Operation(summary = "Approve, reject or refer-back project request")
    public ResponseEntity<ProjectRequestResponse> takeAction(@PathVariable Long id,
                                                             @Valid @RequestBody OnboardingActionRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(projectRequestService.takeAction(id, request, authentication));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a comment to a project request")
    public ResponseEntity<ProjectRequestResponse> addComment(@PathVariable Long id,
                                                             @Valid @RequestBody RequestCommentRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(projectRequestService.addComment(id, request, authentication));
    }

    @PatchMapping("/{id}/resubmit")
    @Operation(summary = "Resubmit project request after refer-back")
    public ResponseEntity<ProjectRequestResponse> resubmit(@PathVariable Long id,
                                                           @Valid @RequestBody ProjectRequestCreateRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(projectRequestService.resubmit(id, request, authentication));
    }

    @GetMapping("/pending-approvals")
    @Operation(summary = "Get project requests pending caller approval")
    public ResponseEntity<List<ProjectRequestResponse>> pendingApprovals(Authentication authentication) {
        return ResponseEntity.ok(projectRequestService.pendingApprovals(authentication));
    }

    @GetMapping("/delivery-manager-options")
    @Operation(summary = "List active delivery managers")
    public ResponseEntity<List<ProjectManagerOptionResponse>> deliveryManagerOptions(Authentication authentication) {
        return ResponseEntity.ok(projectRequestService.deliveryManagerOptions(authentication));
    }

    @GetMapping("/project-owner-options")
    @Operation(summary = "List active project owners")
    public ResponseEntity<List<ProjectManagerOptionResponse>> projectOwnerOptions(Authentication authentication) {
        return ResponseEntity.ok(projectRequestService.projectOwnerOptions(authentication));
    }

    @GetMapping("/project-director-options")
    @Operation(summary = "List active project directors")
    public ResponseEntity<List<ProjectManagerOptionResponse>> projectDirectorOptions(Authentication authentication) {
        return ResponseEntity.ok(projectRequestService.projectDirectorOptions(authentication));
    }

    @GetMapping("/project-manager-options")
    @Operation(summary = "List active project managers")
    public ResponseEntity<List<ProjectManagerOptionResponse>> projectManagerOptions(Authentication authentication) {
        return ResponseEntity.ok(projectRequestService.projectManagerOptions(authentication));
    }

    @GetMapping("/managed-projects")
    @Operation(summary = "List approved projects owned by caller")
    @PreAuthorize("hasAnyAuthority('ROLE_PROJECT_OWNER','ROLE_DIRECTOR','ROLE_CTO','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<List<ManagedProjectResponse>> managedProjects(Authentication authentication) {
        return ResponseEntity.ok(projectChangeRequestService.listManagedProjects(authentication));
    }

    @PostMapping("/{id}/change-requests")
    @Operation(summary = "Create project change request")
    @PreAuthorize("hasAnyAuthority('ROLE_PROJECT_OWNER','ROLE_DIRECTOR','ROLE_CTO','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<ProjectChangeRequestResponse> createChangeRequest(@PathVariable Long id,
                                                                            @Valid @RequestBody ProjectChangeRequestCreateRequest request,
                                                                            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectChangeRequestService.create(id, request, authentication));
    }

    @GetMapping("/change-requests")
    @Operation(summary = "List relevant project change requests")
    @PreAuthorize("hasAnyAuthority('ROLE_PROJECT_OWNER','ROLE_DIRECTOR','ROLE_CTO','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<List<ProjectChangeRequestResponse>> listChangeRequests(Authentication authentication) {
        return ResponseEntity.ok(projectChangeRequestService.list(authentication));
    }

    @PatchMapping("/change-requests/{id}/actions")
    @Operation(summary = "Approve or reject project change request")
    @PreAuthorize("hasAnyAuthority('ROLE_PROJECT_OWNER','ROLE_DIRECTOR','ROLE_CTO','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<ProjectChangeRequestResponse> actionChangeRequest(@PathVariable Long id,
                                                                            @Valid @RequestBody OnboardingActionRequest request,
                                                                            Authentication authentication) {
        return ResponseEntity.ok(projectChangeRequestService.takeAction(id, request, authentication));
    }

    @PostMapping("/change-requests/{id}/comments")
    @Operation(summary = "Add a comment to project change request")
    @PreAuthorize("hasAnyAuthority('ROLE_PROJECT_OWNER','ROLE_DIRECTOR','ROLE_CTO','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<ProjectChangeRequestResponse> addChangeRequestComment(@PathVariable Long id,
                                                                                @Valid @RequestBody RequestCommentRequest request,
                                                                                Authentication authentication) {
        return ResponseEntity.ok(projectChangeRequestService.addComment(id, request, authentication));
    }
}
