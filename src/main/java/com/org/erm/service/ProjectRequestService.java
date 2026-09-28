package com.org.erm.service;

import com.org.erm.dto.request.OnboardingActionRequest;
import com.org.erm.dto.response.OnboardingApprovalTrailItem;
import com.org.erm.dto.response.PagedResponse;
import com.org.erm.dto.response.ProjectManagerOptionResponse;
import com.org.erm.dto.response.ProjectHrOptionResponse;
import com.org.erm.dto.request.ProjectRequestCreateRequest;
import com.org.erm.dto.request.ProjectBulkRowRequest;
import com.org.erm.dto.response.ProjectBulkSubmitResponse;
import com.org.erm.dto.response.ProjectBulkValidationError;
import com.org.erm.dto.response.ProjectBulkValidationResponse;
import com.org.erm.dto.response.ProjectRequestResponse;
import com.org.erm.dto.request.RequestCommentRequest;
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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ProjectRequestService {

    private static final List<String> PROJECT_TYPES = List.of("Internal", "Billable", "Fixed Bid", "T&M");
    private static final List<String> PRIORITIES = List.of("Low", "Medium", "High", "Critical");
    private static final List<String> ASSOCIATED_HR_ROLES = List.of("Junior HR", "Senior HR", "HR Head");
    private static final List<String> GLOBAL_VIEW_ROLES = List.of("ROLE_CTO", "ROLE_SUPER_ADMIN", "ROLE_ADMIN");

    private final ErmProjectRequestRepository projectRequestRepository;
    private final ErmProjectRequestCommentRepository projectRequestCommentRepository;
    private final ErmUserRepository userRepository;
    private final MentionNotificationService mentionNotificationService;
    private final Validator validator;

    public ProjectRequestService(ErmProjectRequestRepository projectRequestRepository,
                                 ErmProjectRequestCommentRepository projectRequestCommentRepository,
                                 ErmUserRepository userRepository,
                                 MentionNotificationService mentionNotificationService,
                                 Validator validator) {
        this.projectRequestRepository = projectRequestRepository;
        this.projectRequestCommentRepository = projectRequestCommentRepository;
        this.userRepository = userRepository;
        this.mentionNotificationService = mentionNotificationService;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public ProjectBulkValidationResponse validateBulk(List<ProjectBulkRowRequest> rows, Authentication authentication) {
        ensureProjectOwnerCreator(authentication);
        return validateBulkRows(rows, authentication);
    }

    @Transactional
    public ProjectBulkSubmitResponse submitBulk(List<ProjectBulkRowRequest> rows, Authentication authentication) {
        ensureProjectOwnerCreator(authentication);
        ProjectBulkValidationResponse validation = validateBulkRows(rows, authentication);
        if (!validation.valid()) return new ProjectBulkSubmitResponse(false, validation.errors(), List.of());

        List<Long> createdIds = new java.util.ArrayList<>();
        for (ProjectBulkRowRequest row : rows) {
            createdIds.add(create(row.project(), authentication).id());
        }
        return new ProjectBulkSubmitResponse(true, List.of(), createdIds);
    }

    private ProjectBulkValidationResponse validateBulkRows(List<ProjectBulkRowRequest> rows, Authentication authentication) {
        List<ProjectBulkValidationError> errors = new java.util.ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            errors.add(new ProjectBulkValidationError(null, "Rows", "Add at least one project row."));
            return new ProjectBulkValidationResponse(false, errors);
        }
        if (rows.size() > 20) {
            errors.add(new ProjectBulkValidationError(null, "Rows", "A maximum of 20 projects can be validated or submitted per batch."));
            return new ProjectBulkValidationResponse(false, errors);
        }

        Long actorId = loadCurrentUser(authentication).getId();
        Map<String, Integer> firstRowByCode = new LinkedHashMap<>();
        Map<String, Integer> firstRowByName = new LinkedHashMap<>();
        for (int index = 0; index < rows.size(); index++) {
            ProjectBulkRowRequest bulkRow = rows.get(index);
            int rowNumber = bulkRow == null || bulkRow.rowNumber() == null ? index + 2 : bulkRow.rowNumber();
            if (bulkRow == null || bulkRow.project() == null) {
                errors.add(new ProjectBulkValidationError(rowNumber, "Row", "This row is empty."));
                continue;
            }
            ProjectRequestCreateRequest project = bulkRow.project();
            for (ConstraintViolation<ProjectRequestCreateRequest> violation : validator.validate(project)) {
                errors.add(new ProjectBulkValidationError(rowNumber, excelFieldName(violation.getPropertyPath().toString()), violation.getMessage()));
            }
            if (project.projectOwnerUserId() != null && !actorId.equals(project.projectOwnerUserId())) {
                errors.add(new ProjectBulkValidationError(rowNumber, "Project Owner", "Each request must use your own Project Owner account."));
            }

            String code = project.projectCode() == null ? "" : project.projectCode().trim().toUpperCase(java.util.Locale.ROOT);
            String name = project.projectName() == null ? "" : project.projectName().trim().toLowerCase(java.util.Locale.ROOT);
            addBulkDuplicateError(errors, firstRowByCode, rowNumber, "Project Code", code);
            addBulkDuplicateError(errors, firstRowByName, rowNumber, "Project Name", name);

            try {
                applyEditableFields(new ErmProjectRequest(), project, true);
            } catch (ResponseStatusException exception) {
                errors.add(new ProjectBulkValidationError(rowNumber, "Project", exception.getReason() == null ? "Invalid project details." : exception.getReason()));
            } catch (RuntimeException exception) {
                errors.add(new ProjectBulkValidationError(rowNumber, "Project", "Project details could not be validated."));
            }
        }
        return new ProjectBulkValidationResponse(errors.isEmpty(), errors);
    }

    private void addBulkDuplicateError(List<ProjectBulkValidationError> errors, Map<String, Integer> seen,
                                       int rowNumber, String field, String value) {
        if (value.isBlank()) return;
        Integer firstRow = seen.putIfAbsent(value, rowNumber);
        if (firstRow != null) {
            errors.add(new ProjectBulkValidationError(rowNumber, field, "Duplicates the value in row " + firstRow + " of this file."));
        }
    }

    private String excelFieldName(String property) {
        return switch (property) {
            case "projectName" -> "Project Name";
            case "projectCode" -> "Project Code";
            case "clientName" -> "Client Name";
            case "projectType" -> "Project Type";
            case "priority" -> "Priority";
            case "plannedStartDate" -> "Planned Start Date";
            case "plannedEndDate" -> "Planned End Date";
            case "budgetAmount" -> "Budget Amount";
            case "currency" -> "Currency";
            case "deliveryManagerUserId" -> "Delivery Manager Username";
            case "projectOwnerUserId" -> "Project Owner Username";
            case "projectDirectorUserId" -> "Project Director Username";
            case "projectManagerUserId" -> "Project Manager Username";
            case "associatedHrUserId" -> "HRBP Username";
            case "projectStatus" -> "Project Status";
            case "description" -> "Description";
            case "riskNotes" -> "Risk Notes";
            case "comment" -> "Comment";
            default -> property;
        };
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
        Long projectDirectorUserId = hasAnyAuthority(authentication, "ROLE_DIRECTOR") ? actorUser.getId() : null;
        Long deliveryManagerUserId = hasAnyAuthority(authentication, "ROLE_DELIVERY_MANAGER") ? actorUser.getId() : null;

        return PagedResponse.from(projectRequestRepository.search(
                restrictScope,
                projectOwnerUserId,
                projectManagerUserId,
                projectDirectorUserId,
                deliveryManagerUserId,
                parsedStage,
                normalizedQuery,
                pageable
        ).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<ProjectRequestResponse> listProjectMaster(Authentication authentication) {
        ErmUser actorUser = loadCurrentUser(authentication);
        List<ErmProjectRequest> projects;
        if (isGlobalViewer(authentication)) {
            projects = projectRequestRepository.findAll();
        } else if (hasAnyAuthority(
                authentication,
                "ROLE_PROJECT_MANAGER",
                "ROLE_PROJECT_OWNER",
                "ROLE_DIRECTOR",
                "ROLE_DELIVERY_MANAGER"
        )) {
            projects = projectRequestRepository.findAssociatedProjects(actorUser.getId());
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view Project Master");
        }
        return projects.stream()
                .sorted(Comparator.comparing(
                        ErmProjectRequest::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(this::toResponse)
                .toList();
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
                        || Objects.equals(item.getProjectDirectorUserId(), actorDirectorUserId))
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

    @Transactional(readOnly = true)
    public List<ProjectHrOptionResponse> associatedHrOptions(Authentication authentication) {
        if (!hasAnyAuthority(authentication, "ROLE_PROJECT_OWNER", "ROLE_DIRECTOR", "ROLE_CTO", "ROLE_SUPER_ADMIN", "ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        Map<Long, ErmUser> users = new LinkedHashMap<>();
        ASSOCIATED_HR_ROLES.forEach(roleName -> userRepository.findActiveUsersByRoleName(roleName)
                .forEach(user -> users.putIfAbsent(user.getId(), user)));
        return users.values().stream()
                .map(user -> new ProjectHrOptionResponse(
                        user.getId(), user.getUsername(), resolveDisplayName(user), user.getEmail(),
                        resolveProjectHrRoleName(user)))
                .sorted(Comparator.comparing(ProjectHrOptionResponse::roleName)
                        .thenComparing(ProjectHrOptionResponse::fullName, String.CASE_INSENSITIVE_ORDER))
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
        ErmUser associatedHr = resolveProjectHrUser(request.associatedHrUserId());

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
        entity.setDeliveryManagerEmployeeId(deliveryManager.getEmployeeId());
        entity.setDeliveryManagerName(resolveDisplayName(deliveryManager));
        entity.setProjectOwnerUserId(projectOwner.getId());
        entity.setProjectOwnerEmployeeId(projectOwner.getEmployeeId());
        entity.setProjectOwnerName(resolveDisplayName(projectOwner));
        entity.setProjectDirectorUserId(projectDirector.getId());
        entity.setProjectDirectorEmployeeId(projectDirector.getEmployeeId());
        entity.setProjectDirectorName(resolveDisplayName(projectDirector));
        entity.setProjectManagerUserId(projectManager.getId());
        entity.setProjectManagerEmployeeId(projectManager.getEmployeeId());
        entity.setProjectManagerName(resolveDisplayName(projectManager));
        entity.setAssociatedHrUserId(associatedHr.getId());
        entity.setAssociatedHrName(resolveDisplayName(associatedHr));
        entity.setAssociatedHrRoleName(resolveProjectHrRoleName(associatedHr));
        entity.setProjectStatus(projectStatus);
        entity.setDescription(description);
        entity.setRiskNotes(riskNotes);
    }

    private ErmUser resolveProjectHrUser(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Associated HR is required");
        }
        ErmUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Associated HR user not found"));
        if (!user.isActive() || !"active".equalsIgnoreCase(user.getEmploymentStatus())
                || resolveProjectHrRoleNameOrNull(user) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Associated HR must be an active Junior HR, Senior HR, or HR Head");
        }
        return user;
    }

    private String resolveProjectHrRoleName(ErmUser user) {
        String roleName = resolveProjectHrRoleNameOrNull(user);
        if (roleName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Associated HR must be assigned a Junior HR, Senior HR, or HR Head role");
        }
        return roleName;
    }

    private String resolveProjectHrRoleNameOrNull(ErmUser user) {
        if (user.getPrimaryRoleId() != null) {
            String primaryRole = user.getRoles().stream()
                    .filter(role -> role.getId().equals(user.getPrimaryRoleId()))
                    .map(role -> role.getName())
                    .filter(roleName -> ASSOCIATED_HR_ROLES.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(roleName)))
                    .findFirst()
                    .orElse(null);
            if (primaryRole != null) return primaryRole;
        }
        return ASSOCIATED_HR_ROLES.stream()
                .filter(allowed -> user.getRoles().stream().anyMatch(role -> allowed.equalsIgnoreCase(role.getName())))
                .findFirst()
                .orElse(null);
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
        mentionNotificationService.notifyMentions(
                actor,
                comment,
                "PROJECT_REQUEST",
                entity.getId(),
                actor + " mentioned you on project request " + entity.getProjectCode(),
                "/projects"
        );
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
        boolean directorAccess = hasAnyAuthority(authentication, "ROLE_DIRECTOR")
                && entity.getProjectDirectorUserId() != null
                && actor.getId().equals(entity.getProjectDirectorUserId());
        boolean deliveryManagerAccess = hasAnyAuthority(authentication, "ROLE_DELIVERY_MANAGER")
                && entity.getDeliveryManagerUserId() != null
                && actor.getId().equals(entity.getDeliveryManagerUserId());
        return ownerAccess || managerAccess || directorAccess || deliveryManagerAccess;
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
                entity.getAssociatedHrUserId(),
                entity.getAssociatedHrName(),
                entity.getAssociatedHrRoleName(),
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
