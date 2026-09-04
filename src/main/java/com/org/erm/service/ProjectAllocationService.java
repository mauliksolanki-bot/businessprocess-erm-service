package com.org.erm.service;

import com.org.erm.dto.response.OnboardingApprovalTrailItem;
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
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
    public List<ProjectAllocationResponse> create(ProjectAllocationBatchCreateRequest request, Authentication authentication) {
        ensureProjectManager(authentication);
        String actor = authentication.getName();
        ErmUser actorUser = userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ErmProjectRequest projectRequest = resolveProjectRequest(request.projectRequestId(), actorUser, authentication);
        validateDates(request.startDate(), request.endDate());
        BigDecimal percent = normalizePercent(request.allocationPercent());
        List<Long> employeeIds = normalizeEmployeeIds(request.employeeUserIds());
        List<ProjectAllocationResponse> responses = new ArrayList<>();

        for (Long employeeId : employeeIds) {
            ErmUser employee = resolveActiveEmployee(employeeId);
            ensureEmployeeCapacity(employee.getId(), request.startDate(), request.endDate(), percent, null);
            responses.add(createAllocation(projectRequest, employee, request.allocationType(), percent, request.startDate(), request.endDate(), normalizeOptional(request.comment()), actor));
        }

        return responses;
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
        String comment = normalizeRequired(request.comment());
        String actor = authentication.getName();
        return applyAction(entity, request.decision(), comment, actor, List.of());
    }

    @Transactional
    public List<ProjectAllocationResponse> takeBulkAction(ProjectAllocationBulkActionRequest request, Authentication authentication) {
        ensureDeliveryManager(authentication);
        String comment = normalizeRequired(request.comment());
        String actor = authentication.getName();
        List<Long> allocationIds = normalizeAllocationIds(request.allocationIds());
        List<ErmProjectAllocation> allocations = new ArrayList<>();
        for (ErmProjectAllocation allocation : allocationRepository.findAllById(allocationIds)) {
            allocations.add(allocation);
        }
        if (allocations.size() != allocationIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more allocations were not found");
        }

        Map<Long, ErmProjectAllocation> allocationsById = new LinkedHashMap<>();
        for (ErmProjectAllocation allocation : allocations) {
            allocationsById.put(allocation.getId(), allocation);
        }

        List<ErmProjectAllocation> projectedApprovals = new ArrayList<>();
        List<ProjectAllocationResponse> responses = new ArrayList<>();
        for (Long allocationId : allocationIds) {
            ErmProjectAllocation entity = allocationsById.get(allocationId);
            if (entity == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more allocations were not found");
            }
            responses.add(applyAction(entity, request.decision(), comment, actor, projectedApprovals));
            if (request.decision() == OnboardingActionDecision.APPROVE) {
                projectedApprovals.add(entity);
            }
        }

        return responses;
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

        ErmUser actorUser = userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        ErmProjectRequest projectRequest = resolveProjectRequest(request.projectRequestId(), actorUser, authentication);
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
        String comment = normalizeRequired(request.comment());
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
        appendTrail(entity, "Comment", actor, "Commented", normalizeRequired(request.comment()), LocalDateTime.now());
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
        ErmUser actorUser = userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        List<ErmProjectRequest> projects;
        if (hasAnyAuthority(authentication, ROLE_DM, "ROLE_SUPER_ADMIN", "ROLE_ADMIN")) {
            projects = projectRequestRepository.findAllByWorkflowStageOrderByProjectNameAsc(ProjectWorkflowStage.SUPER_ADMIN_APPROVED);
        } else if (hasAnyAuthority(authentication, ROLE_PM, ROLE_TEAM_LEAD, ROLE_IT_SUPPORT_MANAGER, ROLE_IT_SUPPORT_LEAD, "ROLE_PROJECT_OWNER", "ROLE_DIRECTOR", "ROLE_DELIVERY_MANAGER")) {
            projects = projectRequestRepository.findAssociatedProjects(actorUser.getId());
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
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
                        user.getRoles().stream().findFirst().map(com.org.erm.model.ErmRole::getName).orElse("NA")
                ))
                .toList();
    }

    private ErmProjectRequest resolveProjectRequest(Long requestId, ErmUser actorUser, Authentication authentication) {
        ErmProjectRequest projectRequest = projectRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project request not found"));
        if (projectRequest.getWorkflowStage() != ProjectWorkflowStage.SUPER_ADMIN_APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allocations are allowed only for fully approved projects");
        }
        if (!canAccessProjectAllocation(authentication, actorUser, projectRequest)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can allocate only projects associated with you");
        }
        return projectRequest;
    }

    private boolean canAccessProjectAllocation(Authentication authentication, ErmUser actorUser, ErmProjectRequest projectRequest) {
        if (hasAnyAuthority(authentication, ROLE_DM, "ROLE_SUPER_ADMIN", "ROLE_ADMIN")) {
            return true;
        }
        if (hasAnyAuthority(authentication, ROLE_PM, ROLE_TEAM_LEAD, ROLE_IT_SUPPORT_MANAGER, ROLE_IT_SUPPORT_LEAD)) {
            return Objects.equals(projectRequest.getProjectManagerUserId(), actorUser.getId())
                    || authentication.getName().equalsIgnoreCase(projectRequest.getCreatedByUsername());
        }
        if (hasAnyAuthority(authentication, "ROLE_PROJECT_OWNER")) {
            return Objects.equals(projectRequest.getProjectOwnerUserId(), actorUser.getId());
        }
        if (hasAnyAuthority(authentication, "ROLE_DIRECTOR")) {
            return Objects.equals(projectRequest.getProjectDirectorUserId(), actorUser.getId());
        }
        return false;
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
        entity.setEmployeeRoleName(employee.getRoles().stream().findFirst().map(com.org.erm.model.ErmRole::getName).orElse(null));
        entity.setAllocationType(request.allocationType());
        entity.setAllocationPercent(percent);
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
    }

    private ProjectAllocationResponse createAllocation(ErmProjectRequest projectRequest,
                                                       ErmUser employee,
                                                       com.org.erm.model.ProjectAllocationType allocationType,
                                                       BigDecimal percent,
                                                       LocalDate startDate,
                                                       LocalDate endDate,
                                                       String comment,
                                                       String actor) {
        ErmProjectAllocation entity = new ErmProjectAllocation();
        entity.setProjectRequestId(projectRequest.getId());
        entity.setProjectName(projectRequest.getProjectName());
        entity.setProjectCode(projectRequest.getProjectCode());
        entity.setEmployeeUserId(employee.getId());
        entity.setEmployeeName(resolveDisplayName(employee));
        entity.setEmployeeRoleName(employee.getRoles().stream().findFirst().map(com.org.erm.model.ErmRole::getName).orElse(null));
        entity.setAllocationType(allocationType);
        entity.setAllocationPercent(percent);
        entity.setStartDate(startDate);
        entity.setEndDate(endDate);
        entity.setAllocationCode(buildAllocationCode(projectRequest.getId(), employee.getId(), LocalDateTime.now()));
        entity.setCreatedByUsername(actor);
        entity.setStatus(ProjectAllocationStatus.PENDING_DM_APPROVAL);
        entity = allocationRepository.save(entity);
        appendTrail(entity, "Allocation Submission", actor, "Submitted", comment, LocalDateTime.now());
        return toResponse(entity);
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

    private ProjectAllocationResponse applyAction(ErmProjectAllocation entity,
                                                  OnboardingActionDecision decision,
                                                  String comment,
                                                  String actor,
                                                  List<ErmProjectAllocation> projectedApprovals) {
        if (entity.getStatus() != ProjectAllocationStatus.PENDING_DM_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending allocations can be actioned");
        }
        LocalDateTime now = LocalDateTime.now();
        entity.setDmActionBy(actor);
        entity.setDmActionAt(now);
        entity.setDmComment(comment);

        if (decision == OnboardingActionDecision.APPROVE) {
            ensureEmployeeCapacity(entity.getEmployeeUserId(), entity.getStartDate(), entity.getEndDate(), entity.getAllocationPercent(), entity.getId(), projectedApprovals);
            entity.setStatus(ProjectAllocationStatus.ACTIVE);
        } else if (decision == OnboardingActionDecision.REJECT) {
            entity.setStatus(ProjectAllocationStatus.REJECTED);
        } else {
            entity.setStatus(ProjectAllocationStatus.REFER_BACK);
            entity.setReferBackBy(actor);
            entity.setReferBackAt(now);
            entity.setReferBackComment(comment);
        }

        entity = allocationRepository.save(entity);
        appendTrail(entity, "Delivery Manager Review", actor, decisionLabel(decision), comment, now);
        return toResponse(entity);
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

    private String normalizeRequired(String value) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment is required");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private List<Long> normalizeEmployeeIds(List<Long> employeeUserIds) {
        if (employeeUserIds == null || employeeUserIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one employee is required");
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long employeeUserId : employeeUserIds) {
            if (employeeUserId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee is required");
            }
            if (!uniqueIds.add(employeeUserId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee selection contains duplicates");
            }
        }
        return new ArrayList<>(uniqueIds);
    }

    private List<Long> normalizeAllocationIds(List<Long> allocationIds) {
        if (allocationIds == null || allocationIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one allocation is required");
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long allocationId : allocationIds) {
            if (allocationId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allocation is required");
            }
            if (!uniqueIds.add(allocationId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allocation selection contains duplicates");
            }
        }
        return new ArrayList<>(uniqueIds);
    }

    private void ensureEmployeeCapacity(Long employeeUserId,
                                        LocalDate startDate,
                                        LocalDate endDate,
                                        BigDecimal requestedPercent,
                                        Long excludeId,
                                        List<ErmProjectAllocation> projectedApprovals) {
        BigDecimal activePercent = allocationRepository.sumAllocationPercentForOverlap(
                employeeUserId,
                ProjectAllocationStatus.ACTIVE,
                startDate,
                endDate,
                excludeId
        );
        BigDecimal projectedPercent = projectedApprovals.stream()
                .filter(allocation -> Objects.equals(allocation.getEmployeeUserId(), employeeUserId))
                .filter(allocation -> datesOverlap(allocation.getStartDate(), allocation.getEndDate(), startDate, endDate))
                .map(ErmProjectAllocation::getAllocationPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = activePercent.add(projectedPercent).add(requestedPercent);
        if (total.compareTo(new BigDecimal("100.00")) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Allocation exceeds employee capacity. Overlapping active allocation would become " + total.stripTrailingZeros().toPlainString() + "%"
            );
        }
    }

    private boolean datesOverlap(LocalDate firstStart, LocalDate firstEnd, LocalDate secondStart, LocalDate secondEnd) {
        return !firstEnd.isBefore(secondStart) && !secondEnd.isBefore(firstStart);
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
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
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
