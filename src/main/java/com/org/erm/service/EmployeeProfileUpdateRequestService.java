package com.org.erm.service;

import com.org.erm.dto.request.EmployeeProfileUpdateRequestCreateRequest;
import com.org.erm.dto.response.EmployeeProfileUpdateRequestResponse;
import com.org.erm.dto.request.OnboardingActionRequest;
import com.org.erm.dto.response.OnboardingApprovalTrailItem;
import com.org.erm.dto.response.PagedResponse;
import com.org.erm.dto.request.RequestCancellationRequest;
import com.org.erm.dto.request.RequestCommentRequest;
import com.org.erm.model.ErmDesignationHierarchy;
import com.org.erm.model.ErmEmployeeProfileUpdateRequest;
import com.org.erm.model.ErmEmployeeProfileUpdateRequestComment;
import com.org.erm.model.ErmRole;
import com.org.erm.model.ErmUser;
import com.org.erm.model.OnboardingActionDecision;
import com.org.erm.model.OnboardingWorkflowStage;
import com.org.erm.repository.ErmDesignationHierarchyRepository;
import com.org.erm.repository.ErmEmployeeProfileUpdateRequestCommentRepository;
import com.org.erm.repository.ErmEmployeeProfileUpdateRequestRepository;
import com.org.erm.repository.ErmRoleRepository;
import com.org.erm.repository.ErmUserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class EmployeeProfileUpdateRequestService {

    private final ErmEmployeeProfileUpdateRequestRepository requestRepository;
    private final ErmEmployeeProfileUpdateRequestCommentRepository requestCommentRepository;
    private final ErmUserRepository userRepository;
    private final ErmDesignationHierarchyRepository designationHierarchyRepository;
    private final ErmRoleRepository roleRepository;
    private final MentionNotificationService mentionNotificationService;
    private final EmployeeIdService employeeIdService;
    private final EmployeeRoleReferenceService employeeRoleReferenceService;

    public EmployeeProfileUpdateRequestService(ErmEmployeeProfileUpdateRequestRepository requestRepository,
                                               ErmEmployeeProfileUpdateRequestCommentRepository requestCommentRepository,
                                               ErmUserRepository userRepository,
                                               ErmDesignationHierarchyRepository designationHierarchyRepository,
                                               ErmRoleRepository roleRepository,
                                               MentionNotificationService mentionNotificationService,
                                               EmployeeIdService employeeIdService,
                                               EmployeeRoleReferenceService employeeRoleReferenceService) {
        this.requestRepository = requestRepository;
        this.requestCommentRepository = requestCommentRepository;
        this.userRepository = userRepository;
        this.designationHierarchyRepository = designationHierarchyRepository;
        this.roleRepository = roleRepository;
        this.mentionNotificationService = mentionNotificationService;
        this.employeeIdService = employeeIdService;
        this.employeeRoleReferenceService = employeeRoleReferenceService;
    }

    private static final Set<OnboardingWorkflowStage> TERMINAL_STAGES =
            EnumSet.of(OnboardingWorkflowStage.SUPER_ADMIN_APPROVED, OnboardingWorkflowStage.REJECTED, OnboardingWorkflowStage.CANCELLED);

    @Transactional(readOnly = true)
    public boolean hasPendingRequest(Long employeeUserId) {
        return requestRepository.existsByEmployeeUserIdAndWorkflowStageNotIn(employeeUserId, TERMINAL_STAGES);
    }

    @Transactional
    public EmployeeProfileUpdateRequestResponse create(EmployeeProfileUpdateRequestCreateRequest request,
                                                       Authentication authentication) {
        ensureSeniorHr(authentication);
        ErmUser user = userRepository.findById(request.employeeUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (hasPendingRequest(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A profile update request for this employee is already in progress. Please wait until it is approved or rejected before submitting a new one.");
        }

        String requestedDepartment = normalizeRequired(request.department(), "Department is required");
        String requestedStatus = normalizeEmploymentStatus(request.employmentStatus());
        String currentDesignation = resolveCurrentDesignation(user);
        ManagerAssignment managerAssignment = resolveManagerAssignment(
                request.designationRoleName(),
                request.reportingManagerUserId()
        );
        TeamLeadReassignment reassignment = resolveTeamLeadReassignment(
                user,
                currentDesignation,
                managerAssignment.designationRoleName(),
                request.replacementTeamLeadUserId()
        );
        if (user.getId().equals(managerAssignment.manager().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee and reporting manager cannot be the same");
        }
        ensureChanged(user, requestedDepartment, requestedStatus, managerAssignment);

        ErmEmployeeProfileUpdateRequest entity = new ErmEmployeeProfileUpdateRequest();
        entity.setEmployeeUserId(user.getId());
        entity.setEmployeeId(user.getEmployeeId());
        entity.setEmployeeUsername(user.getUsername());
        entity.setCurrentFullName(user.getFullName());
        entity.setCurrentEmail(user.getEmail());
        entity.setCurrentDepartment(user.getDepartment());
        entity.setCurrentEmploymentStatus(user.getEmploymentStatus());
        entity.setCurrentDesignationRoleName(currentDesignation);
        entity.setCurrentReportingManagerUserId(user.getReportingManagerUserId());
        entity.setCurrentReportingManagerEmployeeId(resolveEmployeeId(user.getReportingManagerUserId()));
        entity.setCurrentReportingManagerName(resolveCurrentManagerName(user.getReportingManagerUserId()));
        entity.setRequestedFullName(user.getFullName());
        entity.setRequestedEmail(user.getEmail());
        entity.setRequestedDepartment(requestedDepartment);
        entity.setRequestedEmploymentStatus(requestedStatus);
        entity.setRequestedDesignationRoleName(managerAssignment.designationRoleName());
        entity.setRequestedReportingManagerUserId(managerAssignment.manager().getId());
        entity.setRequestedReportingManagerEmployeeId(managerAssignment.manager().getEmployeeId());
        entity.setRequestedReportingManagerName(resolveUserDisplayName(managerAssignment.manager()));
        entity.setReplacementTeamLeadUserId(reassignment.replacementTeamLeadUserId());
        entity.setReplacementTeamLeadEmployeeId(resolveEmployeeId(reassignment.replacementTeamLeadUserId()));
        entity.setReplacementTeamLeadName(reassignment.replacementTeamLeadName());
        entity.setDirectReportsAffectedCount(reassignment.directReportsAffectedCount());
        entity.setWorkflowStage(OnboardingWorkflowStage.HR_SUBMITTED);
        entity.setCreatedByUsername(authentication.getName());
        entity.setHrActionBy(authentication.getName());
        entity.setHrActionAt(LocalDateTime.now());
        entity.setHrComment(normalizeOptional(request.comment()));
        entity = requestRepository.save(entity);
        mentionNotificationService.notifyMentions(
                authentication.getName(),
                entity.getHrComment(),
                "EMPLOYEE_PROFILE_UPDATE",
                entity.getId(),
                authentication.getName() + " mentioned you on employee profile request #" + entity.getId(),
                "/employee-data"
        );
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PagedResponse<EmployeeProfileUpdateRequestResponse> list(String workflowStage, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        var pageable = PageRequest.of(safePage, safeSize);
        var pageResult = StringUtils.hasText(workflowStage)
                ? requestRepository.findAllByWorkflowStageOrderByCreatedAtDesc(OnboardingWorkflowStage.fromValue(workflowStage), pageable)
                : requestRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PagedResponse.from(pageResult.map(this::toResponse));
    }

    @Transactional
    public EmployeeProfileUpdateRequestResponse takeAction(Long requestId, OnboardingActionRequest request, Authentication authentication) {
        ErmEmployeeProfileUpdateRequest entity = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee profile update request not found"));

        OnboardingWorkflowStage stage = entity.getWorkflowStage();
        if (TERMINAL_STAGES.contains(stage)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is already closed");
        }

        String actor = authentication.getName();
        String comment = request.comment().trim();
        LocalDateTime now = LocalDateTime.now();

        if (stage == OnboardingWorkflowStage.HR_SUBMITTED) {
            ensureHeadHr(authentication);
            entity.setHeadHrActionBy(actor);
            entity.setHeadHrActionAt(now);
            entity.setHeadHrComment(comment);
            entity.setWorkflowStage(request.decision() == OnboardingActionDecision.APPROVE
                    ? OnboardingWorkflowStage.HEAD_HR_APPROVED
                    : OnboardingWorkflowStage.REJECTED);
        } else if (stage == OnboardingWorkflowStage.HEAD_HR_APPROVED) {
            ensureChro(authentication);
            entity.setChroActionBy(actor);
            entity.setChroActionAt(now);
            entity.setChroComment(comment);
            entity.setWorkflowStage(request.decision() == OnboardingActionDecision.APPROVE
                    ? OnboardingWorkflowStage.CHRO_APPROVED
                    : OnboardingWorkflowStage.REJECTED);
        } else if (stage == OnboardingWorkflowStage.CHRO_APPROVED) {
            ensureSuperAdmin(authentication);
            entity.setSuperAdminActionBy(actor);
            entity.setSuperAdminActionAt(now);
            entity.setSuperAdminComment(comment);
            entity.setWorkflowStage(request.decision() == OnboardingActionDecision.APPROVE
                    ? OnboardingWorkflowStage.SUPER_ADMIN_APPROVED
                    : OnboardingWorkflowStage.REJECTED);
            if (request.decision() == OnboardingActionDecision.APPROVE) {
                applyProfileUpdate(entity);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow stage cannot be actioned");
        }

        entity = requestRepository.save(entity);
        mentionNotificationService.notifyMentions(
                actor,
                comment,
                "EMPLOYEE_PROFILE_UPDATE",
                entity.getId(),
                actor + " mentioned you on employee profile request #" + entity.getId(),
                "/employee-data"
        );
        return toResponse(entity);
    }

    @Transactional
    public EmployeeProfileUpdateRequestResponse cancelRequest(Long requestId,
                                                              RequestCancellationRequest request,
                                                              Authentication authentication) {
        ErmEmployeeProfileUpdateRequest entity = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee profile update request not found"));

        if (TERMINAL_STAGES.contains(entity.getWorkflowStage())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed, rejected, or cancelled requests cannot be cancelled");
        }

        String actor = authentication.getName();
        if (!actor.equalsIgnoreCase(entity.getCreatedByUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the requester can cancel this request");
        }

        entity.setCancelledBy(actor);
        entity.setCancelledAt(LocalDateTime.now());
        entity.setCancelledComment(normalizeOptional(request == null ? null : request.comment()));
        entity.setWorkflowStage(OnboardingWorkflowStage.CANCELLED);
        entity = requestRepository.save(entity);
        mentionNotificationService.notifyMentions(
                actor,
                entity.getCancelledComment(),
                "EMPLOYEE_PROFILE_UPDATE",
                entity.getId(),
                actor + " mentioned you on employee profile request #" + entity.getId(),
                "/employee-data"
        );
        return toResponse(entity);
    }

    @Transactional
    public EmployeeProfileUpdateRequestResponse addComment(Long requestId,
                                                           RequestCommentRequest request,
                                                           Authentication authentication) {
        ErmEmployeeProfileUpdateRequest entity = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee profile update request not found"));

        if (TERMINAL_STAGES.contains(entity.getWorkflowStage())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comments are disabled for closed requests");
        }

        String actor = authentication.getName();
        ErmEmployeeProfileUpdateRequestComment comment = new ErmEmployeeProfileUpdateRequestComment();
        comment.setEmployeeProfileUpdateRequestId(entity.getId());
        comment.setStep("Comment");
        comment.setActor(actor);
        comment.setDecision("Commented");
        comment.setCommentText(normalizeOptional(request.comment()));
        comment.setActionAt(LocalDateTime.now());
        requestCommentRepository.save(comment);
        mentionNotificationService.notifyMentions(
                actor,
                comment.getCommentText(),
                "EMPLOYEE_PROFILE_UPDATE",
                entity.getId(),
                actor + " mentioned you on employee profile request #" + entity.getId(),
                "/employee-data"
        );
        return toResponse(entity);
    }

    private void applyProfileUpdate(ErmEmployeeProfileUpdateRequest request) {
        ErmUser user = userRepository.findById(request.getEmployeeUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        ErmRole role = roleRepository.findByNameIgnoreCase(request.getRequestedDesignationRoleName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Designation role not found"));
        user.setDepartment(request.getRequestedDepartment());
        user.setEmploymentStatus(request.getRequestedEmploymentStatus());
        user.setRoles(new java.util.HashSet<>(java.util.Set.of(role)));
        user.setPrimaryRoleId(role.getId());
        employeeIdService.refreshForPrimaryRole(user);
        user.setReportingManagerUserId(request.getRequestedReportingManagerUserId());
        user.setReportingManagerEmployeeId(request.getRequestedReportingManagerEmployeeId());
        user.setReportingManagerRoleName(resolveManagerRoleName(request.getRequestedDesignationRoleName()));
        user.setActive(!"inactive".equalsIgnoreCase(request.getRequestedEmploymentStatus()));
        userRepository.save(user);
        employeeRoleReferenceService.syncEmployeeIds(user.getId());

        if (request.getReplacementTeamLeadUserId() != null) {
            reassignDirectReports(user.getId(), request.getCurrentDesignationRoleName(), request.getReplacementTeamLeadUserId());
        }
    }

    private void ensureChanged(ErmUser user, String department, String status, ManagerAssignment managerAssignment) {
        boolean changed = !safeEqualsIgnoreCase(user.getDepartment(), department)
                || !safeEqualsIgnoreCase(user.getEmploymentStatus(), status)
                || !safeEqualsIgnoreCase(resolveCurrentDesignation(user), managerAssignment.designationRoleName())
                || user.getReportingManagerUserId() == null
                || !user.getReportingManagerUserId().equals(managerAssignment.manager().getId());
        if (!changed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No changes found in the request");
        }
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeEmploymentStatus(String value) {
        String normalized = normalizeRequired(value, "Employment status is required");
        if ("active".equalsIgnoreCase(normalized)) {
            return "Active";
        }
        if ("inactive".equalsIgnoreCase(normalized)) {
            return "Inactive";
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employment status must be Active or Inactive");
    }

    private String resolveCurrentDesignation(ErmUser employee) {
        if (employee.getPrimaryRoleId() != null) {
            Optional<String> primaryRoleName = employee.getRoles().stream()
                    .filter(role -> role.getId().equals(employee.getPrimaryRoleId()))
                    .map(ErmRole::getName)
                    .findFirst();
            if (primaryRoleName.isPresent()) {
                return primaryRoleName.get();
            }
        }
        return employee.getRoles().stream()
                .map(ErmRole::getName)
                .sorted(Comparator.naturalOrder())
                .findFirst()
                .orElse("Employee");
    }

    private String resolveCurrentManagerName(Long managerUserId) {
        if (managerUserId == null) {
            return null;
        }
        return userRepository.findById(managerUserId)
                .map(this::resolveUserDisplayName)
                .orElse(null);
    }

    private String resolveUserDisplayName(ErmUser user) {
        if (StringUtils.hasText(user.getFullName())) {
            return user.getFullName().trim();
        }
        return user.getUsername();
    }

    private String resolveManagerRoleName(String designationRoleName) {
        ErmDesignationHierarchy hierarchy = designationHierarchyRepository
                .findByDesignationRoleNameIgnoreCaseAndActiveTrue(designationRoleName.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid designation selected"));
        return hierarchy.getReportsToRoleName();
    }

    private TeamLeadReassignment resolveTeamLeadReassignment(ErmUser employee,
                                                             String currentDesignationRoleName,
                                                             String requestedDesignationRoleName,
                                                             Long replacementTeamLeadUserId) {
        List<ErmUser> directReports = userRepository.findAllByReportingManagerUserIdOrderByFullNameAsc(employee.getId());
        boolean movingDesignation = !safeEqualsIgnoreCase(currentDesignationRoleName, requestedDesignationRoleName);

        if (!movingDesignation || directReports.isEmpty()) {
            if (replacementTeamLeadUserId != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Replacement manager is not required for this request");
            }
            return new TeamLeadReassignment(null, null, 0);
        }

        if (replacementTeamLeadUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Replacement manager is required because this employee has direct reports");
        }

        ErmUser replacementTeamLead = userRepository.findById(replacementTeamLeadUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replacement manager not found"));
        if (replacementTeamLead.getId().equals(employee.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Replacement manager cannot be the same employee being promoted");
        }
        if (!replacementTeamLead.isActive() || !"active".equalsIgnoreCase(replacementTeamLead.getEmploymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replacement manager is not active");
        }
        boolean matchesCurrentRole = replacementTeamLead.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(currentDesignationRoleName));
        if (!matchesCurrentRole) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Replacement manager must be assigned as " + currentDesignationRoleName);
        }

        return new TeamLeadReassignment(
                replacementTeamLead.getId(),
                resolveUserDisplayName(replacementTeamLead),
                directReports.size()
        );
    }

    private void reassignDirectReports(Long currentManagerUserId, String reportingRoleName, Long replacementTeamLeadUserId) {
        List<ErmUser> directReports = userRepository.findAllByReportingManagerUserIdOrderByFullNameAsc(currentManagerUserId);
        if (directReports.isEmpty()) {
            return;
        }
        for (ErmUser directReport : directReports) {
            directReport.setReportingManagerUserId(replacementTeamLeadUserId);
            directReport.setReportingManagerEmployeeId(resolveEmployeeId(replacementTeamLeadUserId));
            directReport.setReportingManagerRoleName(reportingRoleName);
        }
        userRepository.saveAll(directReports);
    }

    private String resolveEmployeeId(Long userId) {
        return userId == null ? null : userRepository.findById(userId).map(ErmUser::getEmployeeId).orElse(null);
    }

    private ManagerAssignment resolveManagerAssignment(String designationRoleName, Long reportingManagerUserId) {
        if (!StringUtils.hasText(designationRoleName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Designation is required");
        }
        if (reportingManagerUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reporting manager is required");
        }

        ErmDesignationHierarchy hierarchy = designationHierarchyRepository
                .findByDesignationRoleNameIgnoreCaseAndActiveTrue(designationRoleName.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid designation selected"));

        String managerRoleName = hierarchy.getReportsToRoleName();
        ErmUser manager = userRepository.findById(reportingManagerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reporting manager not found"));

        if (!manager.isActive() || !"active".equalsIgnoreCase(manager.getEmploymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reporting manager is not active");
        }

        Optional<ErmRole> matchingRole = manager.getRoles().stream()
                .filter(role -> role.getName().equalsIgnoreCase(managerRoleName))
                .findFirst();
        if (matchingRole.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reporting manager must be assigned as " + managerRoleName);
        }

        return new ManagerAssignment(hierarchy.getDesignationRoleName(), managerRoleName, manager);
    }

    private boolean safeEqualsIgnoreCase(String left, String right) {
        if (!StringUtils.hasText(left) && !StringUtils.hasText(right)) {
            return true;
        }
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private void ensureSeniorHr(Authentication authentication) {
        if (hasAnyAuthority(authentication, "ROLE_SENIOR_HR")) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Senior HR can create employee update requests");
    }

    private void ensureHeadHr(Authentication authentication) {
        if (hasAnyAuthority(authentication, "ROLE_HR_HEAD")) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Head HR can action this stage");
    }

    private void ensureChro(Authentication authentication) {
        if (hasAnyAuthority(authentication, "ROLE_CHRO")) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only CHRO can action this stage");
    }

    private void ensureSuperAdmin(Authentication authentication) {
        if (hasAnyAuthority(authentication, "ROLE_SUPER_ADMIN")) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Super Admin can action this stage");
    }

    private boolean hasAnyAuthority(Authentication authentication, String... authorities) {
        return authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .anyMatch(authority -> List.of(authorities).contains(authority));
    }

    private List<OnboardingApprovalTrailItem> buildTrail(ErmEmployeeProfileUpdateRequest request) {
        List<OnboardingApprovalTrailItem> trail = new ArrayList<>();
        List<ErmEmployeeProfileUpdateRequestComment> historyEntries =
                requestCommentRepository.findAllByEmployeeProfileUpdateRequestIdOrderByActionAtAscIdAsc(request.getId());
        trail.addAll(historyEntries.stream()
                .map(entry -> new OnboardingApprovalTrailItem(
                        entry.getStep(),
                        entry.getActor(),
                        entry.getDecision(),
                        entry.getCommentText(),
                        entry.getActionAt()
                ))
                .toList());
        if (StringUtils.hasText(request.getHrActionBy())) {
            trail.add(new OnboardingApprovalTrailItem("Senior HR Submission", request.getHrActionBy(), "Submitted", request.getHrComment(), request.getHrActionAt()));
        }
        if (StringUtils.hasText(request.getHeadHrActionBy())) {
            trail.add(new OnboardingApprovalTrailItem("Head HR Review", request.getHeadHrActionBy(),
                    request.getWorkflowStage() == OnboardingWorkflowStage.REJECTED ? "Rejected" : "Approved",
                    request.getHeadHrComment(), request.getHeadHrActionAt()));
        }
        if (StringUtils.hasText(request.getChroActionBy())) {
            trail.add(new OnboardingApprovalTrailItem("CHRO Review", request.getChroActionBy(),
                    request.getWorkflowStage() == OnboardingWorkflowStage.REJECTED ? "Rejected" : "Approved",
                    request.getChroComment(), request.getChroActionAt()));
        }
        if (StringUtils.hasText(request.getSuperAdminActionBy())) {
            trail.add(new OnboardingApprovalTrailItem("Super Admin Review", request.getSuperAdminActionBy(),
                    request.getWorkflowStage() == OnboardingWorkflowStage.REJECTED ? "Rejected" : "Approved",
                    request.getSuperAdminComment(), request.getSuperAdminActionAt()));
        }
        if (StringUtils.hasText(request.getCancelledBy())) {
            trail.add(new OnboardingApprovalTrailItem("Request Cancellation", request.getCancelledBy(),
                    "Cancelled", request.getCancelledComment(), request.getCancelledAt()));
        }
        trail.sort((left, right) -> {
            if (left.actionAt() == null && right.actionAt() == null) {
                return 0;
            }
            if (left.actionAt() == null) {
                return 1;
            }
            if (right.actionAt() == null) {
                return -1;
            }
            return left.actionAt().compareTo(right.actionAt());
        });
        return trail;
    }

    private EmployeeProfileUpdateRequestResponse toResponse(ErmEmployeeProfileUpdateRequest request) {
        return new EmployeeProfileUpdateRequestResponse(
                request.getId(),
                request.getEmployeeUserId(),
                request.getEmployeeUsername(),
                request.getCurrentFullName(),
                request.getCurrentEmail(),
                request.getCurrentDepartment(),
                request.getCurrentEmploymentStatus(),
                request.getCurrentDesignationRoleName(),
                request.getCurrentReportingManagerUserId(),
                request.getCurrentReportingManagerName(),
                request.getRequestedFullName(),
                request.getRequestedEmail(),
                request.getRequestedDepartment(),
                request.getRequestedEmploymentStatus(),
                request.getRequestedDesignationRoleName(),
                request.getRequestedReportingManagerUserId(),
                request.getRequestedReportingManagerName(),
                request.getReplacementTeamLeadUserId(),
                request.getReplacementTeamLeadName(),
                request.getDirectReportsAffectedCount(),
                request.getWorkflowStage(),
                request.getCreatedByUsername(),
                buildTrail(request),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private record ManagerAssignment(String designationRoleName, String managerRoleName, ErmUser manager) {
    }

    private record TeamLeadReassignment(Long replacementTeamLeadUserId,
                                        String replacementTeamLeadName,
                                        Integer directReportsAffectedCount) {
    }
}
