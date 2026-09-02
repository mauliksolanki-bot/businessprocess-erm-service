package com.org.erm.service;

import com.org.erm.dto.OnboardingActionRequest;
import com.org.erm.dto.OnboardingApprovalTrailItem;
import com.org.erm.dto.PagedResponse;
import com.org.erm.dto.ProjectManagerOptionResponse;
import com.org.erm.dto.ProjectRequestCreateRequest;
import com.org.erm.dto.ProjectRequestResponse;
import com.org.erm.dto.RequestCommentRequest;
import com.org.erm.model.ErmProjectRequest;
import com.org.erm.model.ErmProjectRequestComment;
import com.org.erm.model.ErmUser;
import com.org.erm.model.OnboardingActionDecision;
import com.org.erm.model.ProjectStatus;
import com.org.erm.model.ProjectWorkflowStage;
import com.org.erm.repository.ErmProjectRequestCommentRepository;
import com.org.erm.repository.ErmProjectRequestRepository;
import com.org.erm.repository.ErmUserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectRequestService {

    private static final List<String> PROJECT_TYPES = List.of("Internal", "Billable", "Fixed Bid", "T&M");
    private static final List<String> PRIORITIES = List.of("Low", "Medium", "High", "Critical");
    private static final List<String> GLOBAL_VIEW_ROLES = List.of("ROLE_DIRECTOR", "ROLE_CTO", "ROLE_SUPER_ADMIN", "ROLE_ADMIN");

    private final ErmProjectRequestRepository projectRequestRepository;
    private final ErmProjectRequestCommentRepository projectRequestCommentRepository;
    private final ErmUserRepository userRepository;

    public ProjectRequestService(ErmProjectRequestRepository projectRequestRepository,
                                 ErmProjectRequestCommentRepository projectRequestCommentRepository,
                                 ErmUserRepository userRepository) {
        this.projectRequestRepository = projectRequestRepository;
        this.projectRequestCommentRepository = projectRequestCommentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectRequestResponse create(ProjectRequestCreateRequest request, Authentication authentication) {
        ensureProjectOwnerCreator(authentication);
        String actor = authentication.getName();
        ErmUser actorUser = loadCurrentUser(authentication);
        if (!actorUser.getId().equals(request.projectOwnerUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can create project requests only for your own Project Owner assignment");
        }

        ErmProjectRequest entity = new ErmProjectRequest();
        applyEditableFields(entity, request, true);
        entity.setCreatedByUsername(actor);
        entity.setWorkflowStage(ProjectWorkflowStage.PM_SUBMITTED);
        entity = projectRequestRepository.save(entity);

        appendTrail(entity, "Project Submission", actor, "Submitted", normalizeOptional(request.comment()), LocalDateTime.now());
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProjectRequestResponse> list(String workflowStage, String query, int page, int size, Authentication authentication) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        var pageable = PageRequest.of(safePage, safeSize);

        ProjectWorkflowStage parsedStage = StringUtils.hasText(workflowStage) ? ProjectWorkflowStage.fromValue(workflowStage) : null;
        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;
        ErmUser actorUser = loadCurrentUser(authentication);
        boolean restrictScope = !isGlobalViewer(authentication);
        Long projectOwnerUserId = hasAnyAuthority(authentication, "ROLE_PROJECT_OWNER") ? actorUser.getId() : null;
        Long projectManagerUserId = hasAnyAuthority(authentication, "ROLE_PROJECT_MANAGER", "ROLE_TEAM_LEAD", "ROLE_IT_SUPPORT_MANAGER", "ROLE_IT_SUPPORT_LEAD") ? actorUser.getId() : null;

        return PagedResponse.from(projectRequestRepository.search(
                restrictScope,
                projectOwnerUserId,
                projectManagerUserId,
                parsedStage,
                normalizedQuery,
                pageable
        ).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ProjectRequestResponse getById(Long requestId, Authentication authentication) {
        ErmProjectRequest entity = projectRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project request not found"));
        if (!canViewProject(authentication, entity)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this project request");
        }
        return toResponse(entity);
    }

    @Transactional
    public ProjectRequestResponse takeAction(Long requestId, OnboardingActionRequest request, Authentication authentication) {
        ErmProjectRequest entity = projectRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project request not found"));

        ProjectWorkflowStage stage = entity.getWorkflowStage();
        if (stage == ProjectWorkflowStage.SUPER_ADMIN_APPROVED || stage == ProjectWorkflowStage.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is already closed");
        }
        if (stage == ProjectWorkflowStage.REFER_BACK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requester must resubmit this request before approval actions");
        }

        String actor = authentication.getName();
        String comment = normalizeRequired(request.comment(), "Action comment is required");
        LocalDateTime now = LocalDateTime.now();

        if (stage == ProjectWorkflowStage.PM_SUBMITTED
                || stage == ProjectWorkflowStage.DELIVERY_MANAGER_APPROVED
                || stage == ProjectWorkflowStage.PROJECT_OWNER_APPROVED) {
            ensureAssignedDirector(authentication, entity.getProjectDirectorUserId());
            entity.setDirectorActionBy(actor);
            entity.setDirectorActionAt(now);
            entity.setDirectorComment(comment);
            updateStageFromDecision(entity, request.decision(), ProjectWorkflowStage.DIRECTOR_APPROVED, "Director Review", actor, comment, now);
        } else if (stage == ProjectWorkflowStage.DIRECTOR_APPROVED) {
            ensureCto(authentication);
            entity.setCtoActionBy(actor);
            entity.setCtoActionAt(now);
            entity.setCtoComment(comment);
            updateStageFromDecision(entity, request.decision(), ProjectWorkflowStage.CTO_APPROVED, "CTO Review", actor, comment, now);
        } else if (stage == ProjectWorkflowStage.CTO_APPROVED) {
            ensureSuperAdmin(authentication);
            entity.setSuperAdminActionBy(actor);
            entity.setSuperAdminActionAt(now);
            entity.setSuperAdminComment(comment);
            updateStageFromDecision(entity, request.decision(), ProjectWorkflowStage.SUPER_ADMIN_APPROVED, "Super Admin Review", actor, comment, now);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow stage cannot be actioned");
        }

        entity = projectRequestRepository.save(entity);
        appendTrail(entity, stageLabel(stage), actor, decisionLabel(request.decision()), comment, now);
        return toResponse(entity);
    }

    @Transactional
    public ProjectRequestResponse resubmit(Long requestId, ProjectRequestCreateRequest request, Authentication authentication) {
        ErmProjectRequest entity = projectRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project request not found"));
        if (entity.getWorkflowStage() != ProjectWorkflowStage.REFER_BACK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only refer-back requests can be resubmitted");
        }
        ensureProjectOwnerCreator(authentication);
        ErmUser actorUser = loadCurrentUser(authentication);
        if (!actorUser.getId().equals(entity.getProjectOwnerUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned Project Owner can resubmit this request");
        }
        if (!request.projectOwnerUserId().equals(entity.getProjectOwnerUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project owner cannot be changed during resubmission");
        }
        String actor = authentication.getName();

        applyEditableFields(entity, request, false);
        entity.setWorkflowStage(ProjectWorkflowStage.PM_SUBMITTED);
        entity = projectRequestRepository.save(entity);
        appendTrail(entity, "Project Re-Submission", actor, "Resubmitted", normalizeOptional(request.comment()), LocalDateTime.now());
        return toResponse(entity);
    }

    @Transactional
    public ProjectRequestResponse addComment(Long requestId, RequestCommentRequest request, Authentication authentication) {
        ErmProjectRequest entity = projectRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project request not found"));
        if (entity.getWorkflowStage() == ProjectWorkflowStage.SUPER_ADMIN_APPROVED
                || entity.getWorkflowStage() == ProjectWorkflowStage.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comments are disabled for closed requests");
        }
        String actor = authentication.getName();
        appendTrail(entity, "Comment", actor, "Commented", normalizeOptional(request.comment()), LocalDateTime.now());
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ProjectRequestResponse> pendingApprovals(Authentication authentication) {
        var stages = new java.util.ArrayList<ProjectWorkflowStage>();
        if (hasAnyAuthority(authentication, "ROLE_DIRECTOR")) {
            stages.add(ProjectWorkflowStage.PM_SUBMITTED);
            stages.add(ProjectWorkflowStage.DELIVERY_MANAGER_APPROVED);
            stages.add(ProjectWorkflowStage.PROJECT_OWNER_APPROVED);
        }
        if (hasAnyAuthority(authentication, "ROLE_CTO")) {
            stages.add(ProjectWorkflowStage.DIRECTOR_APPROVED);
        }
        if (hasAnyAuthority(authentication, "ROLE_SUPER_ADMIN", "ROLE_ADMIN")) {
            stages.add(ProjectWorkflowStage.CTO_APPROVED);
        }
        if (stages.isEmpty()) {
            return List.of();
        }
        Long actorDirectorUserId = hasAnyAuthority(authentication, "ROLE_DIRECTOR")
                ? userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getId()
                : null;
        return projectRequestRepository.findAllByWorkflowStageInOrderByCreatedAtDesc(stages).stream()
                .filter(item -> (item.getWorkflowStage() != ProjectWorkflowStage.PM_SUBMITTED
                        && item.getWorkflowStage() != ProjectWorkflowStage.DELIVERY_MANAGER_APPROVED
                        && item.getWorkflowStage() != ProjectWorkflowStage.PROJECT_OWNER_APPROVED)
                        || actorDirectorUserId == null
                        || item.getProjectDirectorUserId() == null
                        || item.getProjectDirectorUserId().equals(actorDirectorUserId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectManagerOptionResponse> deliveryManagerOptions(Authentication authentication) {
        if (!hasAnyAuthority(authentication, "ROLE_PROJECT_OWNER", "ROLE_DIRECTOR", "ROLE_CTO", "ROLE_SUPER_ADMIN", "ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        return userRepository.findActiveUsersByRoleName("Delivery Manager").stream()
                .map(user -> new ProjectManagerOptionResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectManagerOptionResponse> projectOwnerOptions(Authentication authentication) {
        if (!hasAnyAuthority(authentication, "ROLE_PROJECT_OWNER", "ROLE_DIRECTOR", "ROLE_CTO", "ROLE_SUPER_ADMIN", "ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        return userRepository.findActiveUsersByRoleName("Project Owner").stream()
                .map(user -> new ProjectManagerOptionResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectManagerOptionResponse> projectDirectorOptions(Authentication authentication) {
        if (!hasAnyAuthority(authentication, "ROLE_PROJECT_OWNER", "ROLE_DIRECTOR", "ROLE_CTO", "ROLE_SUPER_ADMIN", "ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        return userRepository.findActiveUsersByRoleName("Director").stream()
                .map(user -> new ProjectManagerOptionResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectManagerOptionResponse> projectManagerOptions(Authentication authentication) {
        if (!hasAnyAuthority(authentication, "ROLE_PROJECT_OWNER", "ROLE_DIRECTOR", "ROLE_CTO", "ROLE_SUPER_ADMIN", "ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        return userRepository.findActiveUsersByRoleName("Project Manager").stream()
                .map(user -> new ProjectManagerOptionResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail()))
                .toList();
    }

    private void applyEditableFields(ErmProjectRequest entity, ProjectRequestCreateRequest request, boolean isCreate) {
        String projectName = normalizeRequired(request.projectName(), "Project name is required");
        String projectCode = normalizeRequired(request.projectCode(), "Project code is required").toUpperCase();
        String clientName = normalizeRequired(request.clientName(), "Client name is required");
        String projectType = normalizeRequired(request.projectType(), "Project type is required");
        String priority = normalizeRequired(request.priority(), "Priority is required");
        String currency = normalizeRequired(request.currency(), "Currency is required").toUpperCase();
        String description = normalizeRequired(request.description(), "Description is required");
        String riskNotes = normalizeOptional(request.riskNotes());
        LocalDate startDate = request.plannedStartDate();
        LocalDate endDate = request.plannedEndDate();
        BigDecimal budget = request.budgetAmount();
        ProjectStatus projectStatus = parseProjectStatus(request.projectStatus());

        if (!PROJECT_TYPES.contains(projectType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project type must be one of: " + String.join(", ", PROJECT_TYPES));
        }
        if (!PRIORITIES.contains(priority)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Priority must be one of: " + String.join(", ", PRIORITIES));
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Planned end date must be on or after planned start date");
        }
        if (budget == null || budget.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget amount must be greater than zero");
        }
        if (startDate.isBefore(LocalDate.now().minusYears(2))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Planned start date is too old");
        }

        if (isCreate) {
            if (projectRequestRepository.existsByProjectCodeIgnoreCase(projectCode)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Project code already exists");
            }
            if (projectRequestRepository.existsByProjectNameIgnoreCase(projectName)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Project name already exists");
            }
        } else {
            if (projectRequestRepository.existsByProjectCodeIgnoreCaseAndIdNot(projectCode, entity.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Project code already exists");
            }
            if (projectRequestRepository.existsByProjectNameIgnoreCaseAndIdNot(projectName, entity.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Project name already exists");
            }
        }

        ErmUser deliveryManager = resolveRoleUser(request.deliveryManagerUserId(), "Delivery Manager", "Delivery manager");
        ErmUser projectOwner = resolveRoleUser(request.projectOwnerUserId(), "Project Owner", "Project owner");
        ErmUser projectDirector = resolveRoleUser(request.projectDirectorUserId(), "Director", "Project director");
        ErmUser projectManager = resolveRoleUser(request.projectManagerUserId(), "Project Manager", "Project manager");

        entity.setProjectName(projectName);
        entity.setProjectCode(projectCode);
        entity.setClientName(clientName);
        entity.setProjectType(projectType);
        entity.setPriority(priority);
        entity.setPlannedStartDate(startDate);
        entity.setPlannedEndDate(endDate);
        entity.setBudgetAmount(budget.setScale(2, java.math.RoundingMode.HALF_UP));
        entity.setCurrency(currency);
        entity.setDeliveryManagerUserId(deliveryManager.getId());
        entity.setDeliveryManagerName(resolveDisplayName(deliveryManager));
        entity.setProjectOwnerUserId(projectOwner.getId());
        entity.setProjectOwnerName(resolveDisplayName(projectOwner));
        entity.setProjectDirectorUserId(projectDirector.getId());
        entity.setProjectDirectorName(resolveDisplayName(projectDirector));
        entity.setProjectManagerUserId(projectManager.getId());
        entity.setProjectManagerName(resolveDisplayName(projectManager));
        entity.setProjectStatus(projectStatus);
        entity.setDescription(description);
        entity.setRiskNotes(riskNotes);
    }

    private ErmUser resolveRoleUser(Long userId, String roleName, String label) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
        }
        ErmUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " not found"));
        if (!user.isActive() || !"active".equalsIgnoreCase(user.getEmploymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is not active");
        }
        boolean roleAssigned = user.getRoles().stream().anyMatch(role -> roleName.equalsIgnoreCase(role.getName()));
        if (!roleAssigned) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not assigned as " + roleName);
        }
        return user;
    }

    private ProjectStatus parseProjectStatus(String value) {
        try {
            ProjectStatus status = ProjectStatus.fromValue(value);
            if (status == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project status is required");
            }
            return status;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
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

    private String resolveDisplayName(ErmUser user) {
        return StringUtils.hasText(user.getFullName()) ? user.getFullName().trim() : user.getUsername();
    }

    private void updateStageFromDecision(ErmProjectRequest entity,
                                         OnboardingActionDecision decision,
                                         ProjectWorkflowStage nextApproveStage,
                                         String referBackStage,
                                         String actor,
                                         String comment,
                                         LocalDateTime actionAt) {
        if (decision == OnboardingActionDecision.REFER_BACK) {
            entity.setWorkflowStage(ProjectWorkflowStage.REFER_BACK);
            entity.setReferBackBy(actor);
            entity.setReferBackComment(comment);
            entity.setReferBackAt(actionAt);
            entity.setReferBackStage(referBackStage);
            return;
        }
        entity.setWorkflowStage(decision == OnboardingActionDecision.APPROVE ? nextApproveStage : ProjectWorkflowStage.REJECTED);
    }

    private void appendTrail(ErmProjectRequest entity, String step, String actor, String decision, String comment, LocalDateTime actionAt) {
        ErmProjectRequestComment history = new ErmProjectRequestComment();
        history.setProjectRequestId(entity.getId());
        history.setStep(step);
        history.setActor(actor);
        history.setDecision(decision);
        history.setCommentText(comment);
        history.setActionAt(actionAt == null ? LocalDateTime.now() : actionAt);
        projectRequestCommentRepository.save(history);
    }

    private String decisionLabel(OnboardingActionDecision decision) {
        if (decision == OnboardingActionDecision.APPROVE) {
            return "Approved";
        }
        if (decision == OnboardingActionDecision.REJECT) {
            return "Rejected";
        }
        return "Refer Back";
    }

    private String stageLabel(ProjectWorkflowStage stage) {
        if (stage == ProjectWorkflowStage.PM_SUBMITTED) {
            return "Director Review";
        }
        if (stage == ProjectWorkflowStage.DELIVERY_MANAGER_APPROVED) {
            return "Director Review";
        }
        if (stage == ProjectWorkflowStage.PROJECT_OWNER_APPROVED) {
            return "Director Review";
        }
        if (stage == ProjectWorkflowStage.DIRECTOR_APPROVED) {
            return "CTO Review";
        }
        if (stage == ProjectWorkflowStage.CTO_APPROVED) {
            return "Super Admin Review";
        }
        return "Review";
    }

    private boolean isGlobalViewer(Authentication authentication) {
        return hasAnyAuthority(authentication, GLOBAL_VIEW_ROLES.toArray(String[]::new));
    }

    private ErmUser loadCurrentUser(Authentication authentication) {
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private boolean canViewProject(Authentication authentication, ErmProjectRequest entity) {
        if (isGlobalViewer(authentication)) {
            return true;
        }
        ErmUser actor = loadCurrentUser(authentication);
        boolean ownerAccess = hasAnyAuthority(authentication, "ROLE_PROJECT_OWNER")
                && entity.getProjectOwnerUserId() != null
                && actor.getId().equals(entity.getProjectOwnerUserId());
        boolean managerAccess = hasAnyAuthority(authentication, "ROLE_PROJECT_MANAGER", "ROLE_TEAM_LEAD", "ROLE_IT_SUPPORT_MANAGER", "ROLE_IT_SUPPORT_LEAD")
                && entity.getProjectManagerUserId() != null
                && actor.getId().equals(entity.getProjectManagerUserId());
        return ownerAccess || managerAccess;
    }

    private void ensureProjectOwnerCreator(Authentication authentication) {
        if (!hasAnyAuthority(authentication, "ROLE_PROJECT_OWNER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Project Owner can create or resubmit project requests");
        }
    }

    private void ensureCto(Authentication authentication) {
        if (!hasAnyAuthority(authentication, "ROLE_CTO")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only CTO can action this stage");
        }
    }

    private void ensureDirector(Authentication authentication) {
        if (!hasAnyAuthority(authentication, "ROLE_DIRECTOR")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Director can action this stage");
        }
    }

    private void ensureAssignedDirector(Authentication authentication, Long expectedUserId) {
        ensureDirector(authentication);
        if (expectedUserId == null) {
            return;
        }
        ErmUser actor = userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!actor.getId().equals(expectedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned Project Director can action this stage");
        }
    }

    private void ensureSuperAdmin(Authentication authentication) {
        if (!hasAnyAuthority(authentication, "ROLE_SUPER_ADMIN", "ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Super Admin can action this stage");
        }
    }

    private boolean hasAnyAuthority(Authentication authentication, String... authorities) {
        return authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .anyMatch(authority -> List.of(authorities).contains(authority));
    }

    private List<OnboardingApprovalTrailItem> buildTrail(Long requestId) {
        return projectRequestCommentRepository.findAllByProjectRequestIdOrderByActionAtAscIdAsc(requestId).stream()
                .map(entry -> new OnboardingApprovalTrailItem(
                        entry.getStep(),
                        entry.getActor(),
                        entry.getDecision(),
                        entry.getCommentText(),
                        entry.getActionAt()
                ))
                .toList();
    }

    private ProjectRequestResponse toResponse(ErmProjectRequest entity) {
        return new ProjectRequestResponse(
                entity.getId(),
                entity.getProjectName(),
                entity.getProjectCode(),
                entity.getClientName(),
                entity.getProjectType(),
                entity.getPriority(),
                entity.getPlannedStartDate(),
                entity.getPlannedEndDate(),
                entity.getBudgetAmount(),
                entity.getCurrency(),
                entity.getDeliveryManagerUserId(),
                entity.getDeliveryManagerName(),
                entity.getProjectOwnerUserId(),
                entity.getProjectOwnerName(),
                entity.getProjectDirectorUserId(),
                entity.getProjectDirectorName(),
                entity.getProjectManagerUserId(),
                entity.getProjectManagerName(),
                entity.getProjectStatus().getLabel(),
                entity.getDescription(),
                entity.getRiskNotes(),
                entity.getWorkflowStage(),
                entity.getCreatedByUsername(),
                entity.getReferBackBy(),
                entity.getReferBackStage(),
                entity.getReferBackComment(),
                entity.getReferBackAt(),
                buildTrail(entity.getId()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
