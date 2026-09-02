package com.org.erm.service;

import com.org.erm.dto.OnboardingApprovalTrailItem;
import com.org.erm.dto.PagedResponse;
import com.org.erm.dto.ProjectAllocationActionRequest;
import com.org.erm.dto.ProjectAllocationCreateRequest;
import com.org.erm.dto.ProjectAllocationEmployeeOptionResponse;
import com.org.erm.dto.ProjectAllocationManageRequest;
import com.org.erm.dto.ProjectAllocationProjectOptionResponse;
import com.org.erm.dto.ProjectAllocationResponse;
import com.org.erm.dto.RequestCommentRequest;
import com.org.erm.model.ErmProjectAllocation;
import com.org.erm.model.ErmProjectAllocationComment;
import com.org.erm.model.ErmProjectRequest;
import com.org.erm.model.ErmUser;
import com.org.erm.model.OnboardingActionDecision;
import com.org.erm.model.ProjectAllocationManageAction;
import com.org.erm.model.ProjectAllocationStatus;
import com.org.erm.model.ProjectWorkflowStage;
import com.org.erm.repository.ErmProjectAllocationCommentRepository;
import com.org.erm.repository.ErmProjectAllocationRepository;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ProjectAllocationService {

    private static final String ROLE_PM = "ROLE_PROJECT_MANAGER";
    private static final String ROLE_TEAM_LEAD = "ROLE_TEAM_LEAD";
    private static final String ROLE_IT_SUPPORT_MANAGER = "ROLE_IT_SUPPORT_MANAGER";
    private static final String ROLE_IT_SUPPORT_LEAD = "ROLE_IT_SUPPORT_LEAD";
    private static final String ROLE_DM = "ROLE_DELIVERY_MANAGER";
    private static final DateTimeFormatter CODE_TIME = DateTimeFormatter.ofPattern("yyMMddHHmm");

    private final ErmProjectAllocationRepository allocationRepository;
    private final ErmProjectAllocationCommentRepository allocationCommentRepository;
    private final ErmProjectRequestRepository projectRequestRepository;
    private final ErmUserRepository userRepository;

    public ProjectAllocationService(ErmProjectAllocationRepository allocationRepository,
                                    ErmProjectAllocationCommentRepository allocationCommentRepository,
                                    ErmProjectRequestRepository projectRequestRepository,
                                    ErmUserRepository userRepository) {
        this.allocationRepository = allocationRepository;
        this.allocationCommentRepository = allocationCommentRepository;
        this.projectRequestRepository = projectRequestRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectAllocationResponse create(ProjectAllocationCreateRequest request, Authentication authentication) {
        ensureProjectManager(authentication);
        String actor = authentication.getName();

        ErmProjectRequest projectRequest = resolveProjectRequest(request.projectRequestId(), actor);
        ErmUser employee = resolveActiveEmployee(request.employeeUserId());
        validateDates(request.startDate(), request.endDate());
        BigDecimal percent = normalizePercent(request.allocationPercent());
        ensureEmployeeCapacity(employee.getId(), request.startDate(), request.endDate(), percent, null);

        ErmProjectAllocation entity = new ErmProjectAllocation();
        applyEditableFields(entity, projectRequest, employee, request, percent);
        entity.setAllocationCode(buildAllocationCode(projectRequest.getId(), employee.getId(), LocalDateTime.now()));
        entity.setCreatedByUsername(actor);
        entity.setStatus(ProjectAllocationStatus.PENDING_DM_APPROVAL);
        entity = allocationRepository.save(entity);

        appendTrail(entity, "Allocation Submission", actor, "Submitted", normalizeOptional(request.comment()), LocalDateTime.now());
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProjectAllocationResponse> list(String status, String query, int page, int size, Authentication authentication) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        var pageable = PageRequest.of(safePage, safeSize);
        String createdBy = hasAnyAuthority(authentication, ROLE_DM, "ROLE_SUPER_ADMIN", "ROLE_ADMIN") ? null : authentication.getName();
        ProjectAllocationStatus parsedStatus = StringUtils.hasText(status) ? ProjectAllocationStatus.fromValue(status) : null;
        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;

        return PagedResponse.from(allocationRepository.search(createdBy, parsedStatus, normalizedQuery, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ProjectAllocationResponse getById(Long id, Authentication authentication) {
        ErmProjectAllocation entity = allocationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project allocation not found"));
        if (!hasAnyAuthority(authentication, ROLE_DM)
                && !authentication.getName().equalsIgnoreCase(entity.getCreatedByUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this allocation");
        }
        return toResponse(entity);
    }

    @Transactional
    public ProjectAllocationResponse takeAction(Long id, ProjectAllocationActionRequest request, Authentication authentication) {
        ensureDeliveryManager(authentication);
        ErmProjectAllocation entity = allocationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project allocation not found"));
        if (entity.getStatus() != ProjectAllocationStatus.PENDING_DM_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending allocations can be actioned");
        }
        String comment = normalizeRequired(request.comment(), "Comment is required");
        String actor = authentication.getName();
        LocalDateTime now = LocalDateTime.now();

        entity.setDmActionBy(actor);
        entity.setDmActionAt(now);
        entity.setDmComment(comment);
        if (request.decision() == OnboardingActionDecision.APPROVE) {
            ensureEmployeeCapacity(entity.getEmployeeUserId(), entity.getStartDate(), entity.getEndDate(), entity.getAllocationPercent(), entity.getId());
            entity.setStatus(ProjectAllocationStatus.ACTIVE);
        } else if (request.decision() == OnboardingActionDecision.REJECT) {
            entity.setStatus(ProjectAllocationStatus.REJECTED);
        } else {
            entity.setStatus(ProjectAllocationStatus.REFER_BACK);
            entity.setReferBackBy(actor);
            entity.setReferBackAt(now);
            entity.setReferBackComment(comment);
        }

        entity = allocationRepository.save(entity);
        appendTrail(entity, "Delivery Manager Review", actor, decisionLabel(request.decision()), comment, now);
        return toResponse(entity);
    }

    @Transactional
    public ProjectAllocationResponse resubmit(Long id, ProjectAllocationCreateRequest request, Authentication authentication) {
        ensureProjectManager(authentication);
        ErmProjectAllocation entity = allocationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project allocation not found"));
        if (entity.getStatus() != ProjectAllocationStatus.REFER_BACK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only refer-back allocations can be resubmitted");
        }
        String actor = authentication.getName();
        if (!actor.equalsIgnoreCase(entity.getCreatedByUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only requester can resubmit this allocation");
        }

        ErmProjectRequest projectRequest = resolveProjectRequest(request.projectRequestId(), actor);
        ErmUser employee = resolveActiveEmployee(request.employeeUserId());
        validateDates(request.startDate(), request.endDate());
        BigDecimal percent = normalizePercent(request.allocationPercent());
        ensureEmployeeCapacity(employee.getId(), request.startDate(), request.endDate(), percent, entity.getId());

        applyEditableFields(entity, projectRequest, employee, request, percent);
        entity.setStatus(ProjectAllocationStatus.PENDING_DM_APPROVAL);
        entity = allocationRepository.save(entity);
        appendTrail(entity, "Allocation Re-Submission", actor, "Resubmitted", normalizeOptional(request.comment()), LocalDateTime.now());
        return toResponse(entity);
    }

    @Transactional
    public ProjectAllocationResponse manage(Long id, ProjectAllocationManageRequest request, Authentication authentication) {
        ensureDeliveryManager(authentication);
        ErmProjectAllocation entity = allocationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project allocation not found"));
        if (entity.getStatus() != ProjectAllocationStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only active allocations can be managed");
        }

        String actor = authentication.getName();
        String comment = normalizeRequired(request.comment(), "Comment is required");
        LocalDateTime now = LocalDateTime.now();
        entity.setDmActionBy(actor);
        entity.setDmActionAt(now);
        entity.setDmComment(comment);

        ProjectAllocationManageAction action = request.action();
        if (action == ProjectAllocationManageAction.EXTEND) {
            if (request.endDate() == null || !request.endDate().isAfter(entity.getEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Extended end date must be after current end date");
            }
            ensureEmployeeCapacity(entity.getEmployeeUserId(), entity.getStartDate(), request.endDate(), entity.getAllocationPercent(), entity.getId());
            entity.setEndDate(request.endDate());
            appendTrail(entity, "Allocation Management", actor, "Extended", comment, now);
        } else if (action == ProjectAllocationManageAction.REDUCE) {
            if (request.allocationPercent() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reduced allocation percent is required");
            }
            BigDecimal newPercent = normalizePercent(request.allocationPercent());
            if (newPercent.compareTo(entity.getAllocationPercent()) >= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reduced allocation percent must be lower than current allocation");
            }
            LocalDate nextEndDate = request.endDate() == null ? entity.getEndDate() : request.endDate();
            if (nextEndDate.isBefore(entity.getStartDate()) || nextEndDate.isAfter(entity.getEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reduced end date must be between start date and current end date");
            }
            ensureEmployeeCapacity(entity.getEmployeeUserId(), entity.getStartDate(), nextEndDate, newPercent, entity.getId());
            entity.setAllocationPercent(newPercent);
            entity.setEndDate(nextEndDate);
            appendTrail(entity, "Allocation Management", actor, "Reduced", comment, now);
        } else if (action == ProjectAllocationManageAction.RELEASE) {
            entity.setStatus(ProjectAllocationStatus.RELEASED);
            LocalDate today = LocalDate.now();
            if (today.isBefore(entity.getEndDate()) && !today.isBefore(entity.getStartDate())) {
                entity.setEndDate(today);
            }
            appendTrail(entity, "Allocation Management", actor, "Released", comment, now);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported manage action");
        }

        entity = allocationRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public ProjectAllocationResponse addComment(Long id, RequestCommentRequest request, Authentication authentication) {
        ErmProjectAllocation entity = allocationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project allocation not found"));
        if (entity.getStatus() == ProjectAllocationStatus.RELEASED
                || entity.getStatus() == ProjectAllocationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comments are disabled for closed allocations");
        }
        String actor = authentication.getName();
        appendTrail(entity, "Comment", actor, "Commented", normalizeRequired(request.comment(), "Comment is required"), LocalDateTime.now());
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ProjectAllocationResponse> pendingApprovals(Authentication authentication) {
        ensureDeliveryManager(authentication);
        return allocationRepository.findAllByStatusOrderByCreatedAtDesc(ProjectAllocationStatus.PENDING_DM_APPROVAL)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectAllocationProjectOptionResponse> projectOptions(Authentication authentication) {
        String actor = authentication.getName();
        List<ErmProjectRequest> projects = hasAnyAuthority(authentication, ROLE_DM)
                ? projectRequestRepository.findAllByWorkflowStageOrderByProjectNameAsc(ProjectWorkflowStage.SUPER_ADMIN_APPROVED)
                : projectRequestRepository.findAllByWorkflowStageAndCreatedByUsernameIgnoreCaseOrderByProjectNameAsc(
                ProjectWorkflowStage.SUPER_ADMIN_APPROVED, actor
        );
        return projects.stream()
                .map(project -> new ProjectAllocationProjectOptionResponse(project.getId(), project.getProjectCode(), project.getProjectName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectAllocationEmployeeOptionResponse> employeeOptions(Authentication authentication) {
        if (!hasAnyAuthority(authentication, ROLE_PM, ROLE_TEAM_LEAD, ROLE_IT_SUPPORT_MANAGER, ROLE_IT_SUPPORT_LEAD, ROLE_DM)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        return userRepository.findAllByActiveTrueAndEmploymentStatusIgnoreCaseOrderByFullNameAsc("active").stream()
                .map(user -> new ProjectAllocationEmployeeOptionResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName(),
                        user.getRoles().stream().findFirst().map(role -> role.getName()).orElse("NA")
                ))
                .toList();
    }

    private ErmProjectRequest resolveProjectRequest(Long requestId, String actor) {
        ErmProjectRequest projectRequest = projectRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project request not found"));
        if (projectRequest.getWorkflowStage() != ProjectWorkflowStage.SUPER_ADMIN_APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allocations are allowed only for fully approved projects");
        }
        if (!actor.equalsIgnoreCase(projectRequest.getCreatedByUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can allocate only your own approved projects");
        }
        return projectRequest;
    }

    private ErmUser resolveActiveEmployee(Long employeeUserId) {
        ErmUser user = userRepository.findById(employeeUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee not found"));
        if (!user.isActive() || !"active".equalsIgnoreCase(user.getEmploymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only active employees can be allocated");
        }
        return user;
    }

    private void applyEditableFields(ErmProjectAllocation entity,
                                     ErmProjectRequest projectRequest,
                                     ErmUser employee,
                                     ProjectAllocationCreateRequest request,
                                     BigDecimal percent) {
        entity.setProjectRequestId(projectRequest.getId());
        entity.setProjectName(projectRequest.getProjectName());
        entity.setProjectCode(projectRequest.getProjectCode());
        entity.setEmployeeUserId(employee.getId());
        entity.setEmployeeName(resolveDisplayName(employee));
        entity.setEmployeeRoleName(employee.getRoles().stream().findFirst().map(role -> role.getName()).orElse(null));
        entity.setAllocationType(request.allocationType());
        entity.setAllocationPercent(percent);
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
    }

    private void ensureEmployeeCapacity(Long employeeUserId,
                                        LocalDate startDate,
                                        LocalDate endDate,
                                        BigDecimal requestedPercent,
                                        Long excludeId) {
        BigDecimal activePercent = allocationRepository.sumAllocationPercentForOverlap(
                employeeUserId,
                ProjectAllocationStatus.ACTIVE,
                startDate,
                endDate,
                excludeId
        );
        BigDecimal total = activePercent.add(requestedPercent);
        if (total.compareTo(new BigDecimal("100.00")) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Allocation exceeds employee capacity. Overlapping active allocation would become " + total.stripTrailingZeros().toPlainString() + "%"
            );
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be on or after start date");
        }
    }

    private BigDecimal normalizePercent(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allocation percent must be in range (0, 100]");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
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

    private String buildAllocationCode(Long projectRequestId, Long employeeUserId, LocalDateTime now) {
        return "ALLOC-" + now.format(CODE_TIME) + "-" + projectRequestId + "-" + employeeUserId;
    }

    private void appendTrail(ErmProjectAllocation entity, String step, String actor, String decision, String comment, LocalDateTime actionAt) {
        ErmProjectAllocationComment commentEntry = new ErmProjectAllocationComment();
        commentEntry.setProjectAllocationId(entity.getId());
        commentEntry.setStep(step);
        commentEntry.setActor(actor);
        commentEntry.setDecision(decision);
        commentEntry.setCommentText(comment);
        commentEntry.setActionAt(actionAt == null ? LocalDateTime.now() : actionAt);
        allocationCommentRepository.save(commentEntry);
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

    private boolean hasAnyAuthority(Authentication authentication, String... authorities) {
        return authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .anyMatch(authority -> List.of(authorities).contains(authority));
    }

    private void ensureProjectManager(Authentication authentication) {
        if (!hasAnyAuthority(authentication, ROLE_PM, ROLE_TEAM_LEAD, ROLE_IT_SUPPORT_MANAGER, ROLE_IT_SUPPORT_LEAD)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Project Manager can create or resubmit allocations");
        }
    }

    private void ensureDeliveryManager(Authentication authentication) {
        if (!hasAnyAuthority(authentication, ROLE_DM)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Delivery Manager can manage allocations");
        }
    }

    private List<OnboardingApprovalTrailItem> buildTrail(Long allocationId) {
        return allocationCommentRepository.findAllByProjectAllocationIdOrderByActionAtAscIdAsc(allocationId).stream()
                .map(entry -> new OnboardingApprovalTrailItem(
                        entry.getStep(),
                        entry.getActor(),
                        entry.getDecision(),
                        entry.getCommentText(),
                        entry.getActionAt()
                ))
                .toList();
    }

    private ProjectAllocationResponse toResponse(ErmProjectAllocation entity) {
        ErmUser employee = userRepository.findById(entity.getEmployeeUserId()).orElse(null);
        return new ProjectAllocationResponse(
                entity.getId(),
                entity.getAllocationCode(),
                entity.getProjectRequestId(),
                entity.getProjectName(),
                entity.getProjectCode(),
                entity.getEmployeeUserId(),
                entity.getEmployeeName(),
                employee == null ? null : employee.getUsername(),
                employee == null ? null : employee.getEmail(),
                entity.getEmployeeRoleName(),
                entity.getAllocationType(),
                entity.getAllocationPercent(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getStatus(),
                entity.getCreatedByUsername(),
                entity.getDmActionBy(),
                entity.getDmActionAt(),
                entity.getDmComment(),
                entity.getReferBackBy(),
                entity.getReferBackAt(),
                entity.getReferBackComment(),
                buildTrail(entity.getId()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
