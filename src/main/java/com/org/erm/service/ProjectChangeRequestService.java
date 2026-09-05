package com.org.erm.service;

import com.org.erm.dto.response.ManagedProjectResponse;
import com.org.erm.dto.request.OnboardingActionRequest;
import com.org.erm.dto.response.OnboardingApprovalTrailItem;
import com.org.erm.dto.request.ProjectChangeRequestCreateRequest;
import com.org.erm.dto.response.ProjectChangeRequestResponse;
import com.org.erm.dto.response.ProjectManagerOptionResponse;
import com.org.erm.dto.request.RequestCommentRequest;
import com.org.erm.model.ErmProjectChangeRequest;
import com.org.erm.model.ErmProjectChangeRequestComment;
import com.org.erm.model.ErmProjectRequest;
import com.org.erm.model.ErmUser;
import com.org.erm.model.OnboardingActionDecision;
import com.org.erm.model.ProjectChangeWorkflowStage;
import com.org.erm.model.ProjectStatus;
import com.org.erm.model.ProjectWorkflowStage;
import com.org.erm.repository.ErmProjectChangeRequestCommentRepository;
import com.org.erm.repository.ErmProjectChangeRequestRepository;
import com.org.erm.repository.ErmProjectRequestCommentRepository;
import com.org.erm.repository.ErmProjectRequestRepository;
import com.org.erm.repository.ErmUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Service
public class ProjectChangeRequestService {

    private static final List<String> PROJECT_TYPES = List.of("Internal", "Billable", "Fixed Bid", "T&M");
    private static final List<String> PRIORITIES = List.of("Low", "Medium", "High", "Critical");
    private static final Set<ProjectChangeWorkflowStage> OPEN_STAGES = EnumSet.of(
            ProjectChangeWorkflowStage.PENDING_DELIVERY_MANAGER_APPROVAL,
            ProjectChangeWorkflowStage.PENDING_PROJECT_OWNER_APPROVAL
    );

    private final ErmProjectRequestRepository projectRequestRepository;
    private final ErmProjectRequestCommentRepository projectRequestCommentRepository;
    private final ErmProjectChangeRequestRepository projectChangeRequestRepository;
    private final ErmProjectChangeRequestCommentRepository projectChangeRequestCommentRepository;
    private final ErmUserRepository userRepository;
    private final MentionNotificationService mentionNotificationService;

    public ProjectChangeRequestService(ErmProjectRequestRepository projectRequestRepository,
                                       ErmProjectRequestCommentRepository projectRequestCommentRepository,
                                       ErmProjectChangeRequestRepository projectChangeRequestRepository,
                                       ErmProjectChangeRequestCommentRepository projectChangeRequestCommentRepository,
                                       ErmUserRepository userRepository,
                                       MentionNotificationService mentionNotificationService) {
        this.projectRequestRepository = projectRequestRepository;
        this.projectRequestCommentRepository = projectRequestCommentRepository;
        this.projectChangeRequestRepository = projectChangeRequestRepository;
        this.projectChangeRequestCommentRepository = projectChangeRequestCommentRepository;
        this.userRepository = userRepository;
        this.mentionNotificationService = mentionNotificationService;
    }

    @Transactional(readOnly = true)
    public List<ManagedProjectResponse> listManagedProjects(Authentication authentication) {
        ErmUser actor = loadCurrentUser(authentication);
        ensureProjectOwner(authentication);
        return projectRequestRepository.findAllByWorkflowStageAndProjectOwnerUserIdOrderByProjectNameAsc(
                        ProjectWorkflowStage.SUPER_ADMIN_APPROVED,
                        actor.getId()
                ).stream()
                .map(this::toManagedProjectResponse)
                .toList();
    }

    @Transactional
    public ProjectChangeRequestResponse create(Long projectId, ProjectChangeRequestCreateRequest request, Authentication authentication) {
        ErmUser actor = loadCurrentUser(authentication);
        ensureProjectOwner(authentication);
        ErmProjectRequest project = loadManagedProject(projectId, actor.getId());
        if (projectChangeRequestRepository.existsByProjectRequestIdAndWorkflowStageIn(projectId, OPEN_STAGES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A project change request is already pending for this project");
        }

        ErmProjectChangeRequest entity = new ErmProjectChangeRequest();
        entity.setProjectRequestId(project.getId());
        entity.setCreatedByUsername(authentication.getName());
        applyEditableFields(entity, request, project.getId());
        entity.setWorkflowStage(ProjectChangeWorkflowStage.PENDING_DELIVERY_MANAGER_APPROVAL);
        entity = projectChangeRequestRepository.save(entity);

        appendTrail(entity, "Project Change Request", authentication.getName(), "Submitted", normalizeRequired(request.reason(), "Reason is required"), LocalDateTime.now());
        return toChangeResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ProjectChangeRequestResponse> list(Authentication authentication) {
        LinkedHashMap<Long, ErmProjectChangeRequest> items = new LinkedHashMap<>();
        ErmUser user = loadCurrentUser(authentication);

        if (hasAnyAuthority(authentication, "ROLE_PROJECT_OWNER")) {
            projectChangeRequestRepository.findAllByProjectOwnerUserIdOrderByCreatedAtDesc(user.getId())
                    .forEach(item -> items.putIfAbsent(item.getId(), item));
        }
        if (hasAnyAuthority(authentication, "ROLE_DIRECTOR")) {
            projectChangeRequestRepository.findAllByProjectDirectorUserIdOrderByCreatedAtDesc(user.getId())
                    .forEach(item -> items.putIfAbsent(item.getId(), item));
        }
        if (hasAnyAuthority(authentication, "ROLE_CTO", "ROLE_SUPER_ADMIN", "ROLE_ADMIN")) {
            projectChangeRequestRepository.findAllByWorkflowStageInOrderByCreatedAtDesc(EnumSet.allOf(ProjectChangeWorkflowStage.class))
                    .forEach(item -> items.putIfAbsent(item.getId(), item));
        }

        return items.values().stream()
                .sorted(Comparator.comparing(ErmProjectChangeRequest::getCreatedAt).reversed())
                .map(this::toChangeResponse)
                .toList();
    }

    @Transactional
    public ProjectChangeRequestResponse takeAction(Long changeRequestId, OnboardingActionRequest request, Authentication authentication) {
        if (request.decision() == OnboardingActionDecision.REFER_BACK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refer back is not supported for project change requests");
        }
        ErmProjectChangeRequest entity = projectChangeRequestRepository.findById(changeRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project change request not found"));
        ErmProjectRequest project = projectRequestRepository.findById(entity.getProjectRequestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        if (entity.getWorkflowStage() == ProjectChangeWorkflowStage.APPROVED
                || entity.getWorkflowStage() == ProjectChangeWorkflowStage.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project change request is already closed");
        }

        String actor = authentication.getName();
        String comment = normalizeRequired(request.comment(), "Comment is required");
        LocalDateTime now = LocalDateTime.now();

        if (entity.getWorkflowStage() == ProjectChangeWorkflowStage.PENDING_DELIVERY_MANAGER_APPROVAL) {
            ensureAssignedDirector(authentication, entity.getProjectDirectorUserId());
            entity.setDmActionBy(actor);
            entity.setDmActionAt(now);
            entity.setDmComment(comment);
            entity.setWorkflowStage(request.decision() == OnboardingActionDecision.APPROVE
                    ? ProjectChangeWorkflowStage.PENDING_PROJECT_OWNER_APPROVAL
                    : ProjectChangeWorkflowStage.REJECTED);
            entity = projectChangeRequestRepository.save(entity);
            appendTrail(entity, "Director Review", actor, decisionLabel(request.decision()), comment, now);
            return toChangeResponse(entity);
        }

        ensureCto(authentication);
        entity.setProjectOwnerActionBy(actor);
        entity.setProjectOwnerActionAt(now);
        entity.setProjectOwnerComment(comment);
        if (request.decision() == OnboardingActionDecision.APPROVE) {
            applyApprovedChanges(project, entity);
            projectRequestRepository.save(project);
            entity.setWorkflowStage(ProjectChangeWorkflowStage.APPROVED);
        } else {
            entity.setWorkflowStage(ProjectChangeWorkflowStage.REJECTED);
        }
        entity = projectChangeRequestRepository.save(entity);
        appendTrail(entity, "CTO Review", actor, decisionLabel(request.decision()), comment, now);
        return toChangeResponse(entity);
    }

    @Transactional
    public ProjectChangeRequestResponse addComment(Long changeRequestId, RequestCommentRequest request, Authentication authentication) {
        ErmProjectChangeRequest entity = projectChangeRequestRepository.findById(changeRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project change request not found"));
        if (entity.getWorkflowStage() == ProjectChangeWorkflowStage.APPROVED
                || entity.getWorkflowStage() == ProjectChangeWorkflowStage.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comments are disabled for closed change requests");
        }
        appendTrail(entity, "Comment", authentication.getName(), "Commented", normalizeRequired(request.comment(), "Comment is required"), LocalDateTime.now());
        return toChangeResponse(entity);
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

    private ManagedProjectResponse toManagedProjectResponse(ErmProjectRequest project) {
        ErmProjectChangeRequest openChange = projectChangeRequestRepository.findAllByProjectRequestIdOrderByCreatedAtDesc(project.getId()).stream()
                .filter(change -> OPEN_STAGES.contains(change.getWorkflowStage()))
                .findFirst()
                .orElse(null);
        return new ManagedProjectResponse(
                project.getId(),
                project.getProjectName(),
                project.getProjectCode(),
                project.getClientName(),
                project.getProjectType(),
                project.getPriority(),
                project.getPlannedStartDate(),
                project.getPlannedEndDate(),
                project.getBudgetAmount(),
                project.getCurrency(),
                project.getDeliveryManagerUserId(),
                project.getDeliveryManagerName(),
                project.getProjectOwnerUserId(),
                project.getProjectOwnerName(),
                project.getProjectDirectorUserId(),
                project.getProjectDirectorName(),
                project.getProjectStatus().getLabel(),
                project.getDescription(),
                project.getRiskNotes(),
                project.getCreatedByUsername(),
                openChange != null,
                openChange == null ? null : openChange.getWorkflowStage().getLabel(),
                buildProjectTrail(project.getId()),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getVersion()
        );
    }

    private ProjectChangeRequestResponse toChangeResponse(ErmProjectChangeRequest entity) {
        return new ProjectChangeRequestResponse(
                entity.getId(),
                entity.getProjectRequestId(),
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
                entity.getProjectStatus().getLabel(),
                entity.getDescription(),
                entity.getRiskNotes(),
                entity.getChangeReason(),
                entity.getWorkflowStage(),
                entity.getCreatedByUsername(),
                buildChangeTrail(entity.getId()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    private void applyEditableFields(ErmProjectChangeRequest entity, ProjectChangeRequestCreateRequest request, Long currentProjectId) {
        String projectName = normalizeRequired(request.projectName(), "Project name is required");
        String projectCode = normalizeRequired(request.projectCode(), "Project code is required").toUpperCase();
        String clientName = normalizeRequired(request.clientName(), "Client name is required");
        String projectType = normalizeRequired(request.projectType(), "Project type is required");
        String priority = normalizeRequired(request.priority(), "Priority is required");
        String currency = normalizeRequired(request.currency(), "Currency is required").toUpperCase();
        String description = normalizeRequired(request.description(), "Description is required");
        String riskNotes = normalizeOptional(request.riskNotes());
        String reason = normalizeRequired(request.reason(), "Reason is required");
        LocalDate startDate = request.plannedStartDate();
        LocalDate endDate = request.plannedEndDate();
        BigDecimal budget = request.budgetAmount();

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

        if (projectRequestRepository.existsByProjectCodeIgnoreCaseAndIdNot(projectCode, currentProjectId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project code already exists");
        }
        if (projectRequestRepository.existsByProjectNameIgnoreCaseAndIdNot(projectName, currentProjectId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project name already exists");
        }

        ErmUser deliveryManager = resolveRoleUser(request.deliveryManagerUserId(), "Delivery Manager", "Delivery manager");
        ErmUser projectOwner = resolveRoleUser(request.projectOwnerUserId(), "Project Owner", "Project owner");
        ErmUser projectDirector = resolveRoleUser(request.projectDirectorUserId(), "Director", "Project director");
        ProjectStatus projectStatus = parseProjectStatus(request.projectStatus());

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
        entity.setProjectStatus(projectStatus);
        entity.setDescription(description);
        entity.setRiskNotes(riskNotes);
        entity.setChangeReason(reason);
    }

    private void applyApprovedChanges(ErmProjectRequest project, ErmProjectChangeRequest changeRequest) {
        project.setProjectName(changeRequest.getProjectName());
        project.setProjectCode(changeRequest.getProjectCode());
        project.setClientName(changeRequest.getClientName());
        project.setProjectType(changeRequest.getProjectType());
        project.setPriority(changeRequest.getPriority());
        project.setPlannedStartDate(changeRequest.getPlannedStartDate());
        project.setPlannedEndDate(changeRequest.getPlannedEndDate());
        project.setBudgetAmount(changeRequest.getBudgetAmount());
        project.setCurrency(changeRequest.getCurrency());
        project.setDeliveryManagerUserId(changeRequest.getDeliveryManagerUserId());
        project.setDeliveryManagerName(changeRequest.getDeliveryManagerName());
        project.setProjectOwnerUserId(changeRequest.getProjectOwnerUserId());
        project.setProjectOwnerName(changeRequest.getProjectOwnerName());
        project.setProjectDirectorUserId(changeRequest.getProjectDirectorUserId());
        project.setProjectDirectorName(changeRequest.getProjectDirectorName());
        project.setProjectStatus(changeRequest.getProjectStatus());
        project.setDescription(changeRequest.getDescription());
        project.setRiskNotes(changeRequest.getRiskNotes());
    }

    private ErmProjectRequest loadManagedProject(Long projectId, Long ownerUserId) {
        ErmProjectRequest project = projectRequestRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        if (project.getWorkflowStage() != ProjectWorkflowStage.SUPER_ADMIN_APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only approved projects can be managed");
        }
        if (project.getProjectOwnerUserId() == null || !project.getProjectOwnerUserId().equals(ownerUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can manage only projects assigned to you as Project Owner");
        }
        return project;
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

    private ErmUser loadCurrentUser(Authentication authentication) {
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void ensureAssignedDirector(Authentication authentication, Long expectedUserId) {
        ErmUser actor = userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!hasAnyAuthority(authentication, "ROLE_DIRECTOR") || expectedUserId == null || !actor.getId().equals(expectedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned Project Director can action this change request");
        }
    }

    private void ensureCto(Authentication authentication) {
        if (!hasAnyAuthority(authentication, "ROLE_CTO")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only CTO can action this change request stage");
        }
    }

    private void ensureProjectOwner(Authentication authentication) {
        if (!hasAnyAuthority(authentication, "ROLE_PROJECT_OWNER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Project Owner can manage projects");
        }
    }

    private boolean hasAnyAuthority(Authentication authentication, String... authorities) {
        return authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .anyMatch(authority -> List.of(authorities).contains(authority));
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

    private String decisionLabel(OnboardingActionDecision decision) {
        return decision == OnboardingActionDecision.APPROVE ? "Approved" : "Rejected";
    }

    private void appendTrail(ErmProjectChangeRequest entity, String step, String actor, String decision, String comment, LocalDateTime actionAt) {
        ErmProjectChangeRequestComment history = new ErmProjectChangeRequestComment();
        history.setProjectChangeRequestId(entity.getId());
        history.setStep(step);
        history.setActor(actor);
        history.setDecision(decision);
        history.setCommentText(comment);
        history.setActionAt(actionAt == null ? LocalDateTime.now() : actionAt);
        projectChangeRequestCommentRepository.save(history);
        mentionNotificationService.notifyMentions(
                actor,
                comment,
                "PROJECT_CHANGE_REQUEST",
                entity.getId(),
                actor + " mentioned you on project change request #" + entity.getId(),
                "/projects"
        );
    }

    private List<OnboardingApprovalTrailItem> buildProjectTrail(Long projectRequestId) {
        return projectRequestCommentRepository.findAllByProjectRequestIdOrderByActionAtAscIdAsc(projectRequestId).stream()
                .map(entry -> new OnboardingApprovalTrailItem(
                        entry.getStep(),
                        entry.getActor(),
                        entry.getDecision(),
                        entry.getCommentText(),
                        entry.getActionAt()
                ))
                .toList();
    }

    private List<OnboardingApprovalTrailItem> buildChangeTrail(Long changeRequestId) {
        return projectChangeRequestCommentRepository.findAllByProjectChangeRequestIdOrderByActionAtAscIdAsc(changeRequestId).stream()
                .map(entry -> new OnboardingApprovalTrailItem(
                        entry.getStep(),
                        entry.getActor(),
                        entry.getDecision(),
                        entry.getCommentText(),
                        entry.getActionAt()
                ))
                .toList();
    }
}
