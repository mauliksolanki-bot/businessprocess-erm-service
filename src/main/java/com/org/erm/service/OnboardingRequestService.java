package com.org.erm.service;

import com.org.erm.dto.request.OnboardingActionRequest;
import com.org.erm.dto.response.OnboardingApprovalTrailItem;
import com.org.erm.dto.response.OnboardingDesignationOptionResponse;
import com.org.erm.dto.response.OnboardingManagerOptionResponse;
import com.org.erm.dto.response.OnboardingManagerOptionsResponse;
import com.org.erm.dto.request.OnboardingRequestCreateRequest;
import com.org.erm.dto.response.OnboardingRequestResponse;
import com.org.erm.dto.response.PagedResponse;
import com.org.erm.dto.request.RequestCommentRequest;
import com.org.erm.model.ErmDesignationHierarchy;
import com.org.erm.model.ErmOnboardingRequest;
import com.org.erm.model.ErmOnboardingRequestComment;
import com.org.erm.model.ErmRole;
import com.org.erm.model.ErmUser;
import com.org.erm.model.OnboardingActionDecision;
import com.org.erm.model.OnboardingInterviewStage;
import com.org.erm.model.OnboardingWorkflowStage;
import com.org.erm.repository.ErmDesignationHierarchyRepository;
import com.org.erm.repository.ErmOnboardingRequestCommentRepository;
import com.org.erm.repository.ErmRoleRepository;
import com.org.erm.repository.ErmOnboardingRequestRepository;
import com.org.erm.repository.ErmUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class OnboardingRequestService {

    private static final String EMPLOYEE_EMAIL_DOMAIN = "erm.local";

    private final ErmOnboardingRequestRepository onboardingRequestRepository;
    private final ErmOnboardingRequestCommentRepository onboardingRequestCommentRepository;
    private final ErmDesignationHierarchyRepository designationHierarchyRepository;
    private final ErmRoleRepository roleRepository;
    private final ErmUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public OnboardingRequestService(ErmOnboardingRequestRepository onboardingRequestRepository,
                                    ErmOnboardingRequestCommentRepository onboardingRequestCommentRepository,
                                    ErmDesignationHierarchyRepository designationHierarchyRepository,
                                    ErmRoleRepository roleRepository,
                                    ErmUserRepository userRepository,
                                    PasswordEncoder passwordEncoder) {
        this.onboardingRequestRepository = onboardingRequestRepository;
        this.onboardingRequestCommentRepository = onboardingRequestCommentRepository;
        this.designationHierarchyRepository = designationHierarchyRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public OnboardingRequestResponse create(OnboardingRequestCreateRequest request, Authentication authentication) {
        ensureAnyHr(authentication);
        String actor = authentication.getName();

        ErmOnboardingRequest onboardingRequest = new ErmOnboardingRequest();
        applyCreateRequest(onboardingRequest, request, actor);
        onboardingRequest = onboardingRequestRepository.save(onboardingRequest);
        appendTrail(onboardingRequest, "HR Submission", actor, "Submitted", onboardingRequest.getHrComment(), onboardingRequest.getHrActionAt());

        return toResponse(onboardingRequest);
    }

    @Transactional
    public OnboardingRequestResponse addComment(Long onboardingRequestId, RequestCommentRequest request, Authentication authentication) {
        ErmOnboardingRequest onboardingRequest = onboardingRequestRepository.findById(onboardingRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Onboarding request not found"));

        if (onboardingRequest.getWorkflowStage() == OnboardingWorkflowStage.SUPER_ADMIN_APPROVED
                || onboardingRequest.getWorkflowStage() == OnboardingWorkflowStage.REJECTED
                || onboardingRequest.getWorkflowStage() == OnboardingWorkflowStage.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comments are disabled for closed requests");
        }

        String actor = authentication.getName();
        appendTrail(onboardingRequest, "Comment", actor, "Commented", normalizeOptional(request.comment()), LocalDateTime.now());
        return toResponse(onboardingRequest);
    }

    @Transactional
    public OnboardingRequestResponse takeAction(Long onboardingRequestId, OnboardingActionRequest request, Authentication authentication) {
        ErmOnboardingRequest onboardingRequest = onboardingRequestRepository.findById(onboardingRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Onboarding request not found"));

        OnboardingWorkflowStage stage = onboardingRequest.getWorkflowStage();
        if (stage == OnboardingWorkflowStage.SUPER_ADMIN_APPROVED || stage == OnboardingWorkflowStage.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is already closed");
        }

        String actor = authentication.getName();
        String comment = request.comment().trim();
        LocalDateTime now = LocalDateTime.now();

        if (stage == OnboardingWorkflowStage.HR_SUBMITTED) {
            ensureHeadHr(authentication);
            applyActionForStage(onboardingRequest, request.decision(), actor, comment, now,
                    "Head HR Review", OnboardingWorkflowStage.HEAD_HR_APPROVED,
                    OnboardingWorkflowStage.HR_SUBMITTED);
        } else if (stage == OnboardingWorkflowStage.HEAD_HR_APPROVED) {
            ensureChro(authentication);
            applyActionForStage(onboardingRequest, request.decision(), actor, comment, now,
                    "CHRO Review", OnboardingWorkflowStage.CHRO_APPROVED,
                    OnboardingWorkflowStage.HEAD_HR_APPROVED);
        } else if (stage == OnboardingWorkflowStage.CHRO_APPROVED) {
            ensureSuperAdmin(authentication);
            applyActionForStage(onboardingRequest, request.decision(), actor, comment, now,
                    "Super Admin Review", OnboardingWorkflowStage.SUPER_ADMIN_APPROVED,
                    OnboardingWorkflowStage.CHRO_APPROVED);

            if (request.decision() == OnboardingActionDecision.APPROVE && !StringUtils.hasText(onboardingRequest.getGeneratedEmployeeId())) {
                onboardingRequest.setInterviewStage(OnboardingInterviewStage.COMPLETED);
                assignGeneratedIdentity(onboardingRequest);
                createEmployeeProfile(onboardingRequest);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow stage cannot be actioned");
        }

        onboardingRequest = onboardingRequestRepository.save(onboardingRequest);
        appendTrail(onboardingRequest, stageLabel(stage), actor, decisionLabel(request.decision()), comment, now);
        return toResponse(onboardingRequest);
    }

    @Transactional
    public OnboardingRequestResponse resubmitAfterReferBack(Long onboardingRequestId, OnboardingRequestCreateRequest request, Authentication authentication) {
        ErmOnboardingRequest onboardingRequest = onboardingRequestRepository.findById(onboardingRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Onboarding request not found"));

        if (onboardingRequest.getWorkflowStage() != OnboardingWorkflowStage.REFER_BACK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only refer-back requests can be resubmitted");
        }

        ensureAnyHr(authentication);
        String actor = authentication.getName();
        if (!actor.equalsIgnoreCase(onboardingRequest.getCreatedByUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the requester can edit and resubmit the request");
        }

        applyCreateRequest(onboardingRequest, request, actor);
        onboardingRequest.setWorkflowStage(OnboardingWorkflowStage.HR_SUBMITTED);
        onboardingRequest.setInterviewStage(OnboardingInterviewStage.PENDING);
        onboardingRequest.setHrActionBy(actor);
        onboardingRequest.setHrActionAt(LocalDateTime.now());
        onboardingRequest.setHrComment(normalizeOptional(request.comment()));
        onboardingRequest.setGeneratedEmployeeId(null);
        onboardingRequest.setGeneratedEmailAddress(null);
        onboardingRequest.setLastReminderAt(null);
        onboardingRequest.setReminderCount(0);
        onboardingRequest = onboardingRequestRepository.save(onboardingRequest);
        appendTrail(onboardingRequest, "HR Re-Submission", actor, "Resubmitted", onboardingRequest.getHrComment(), onboardingRequest.getHrActionAt());
        return toResponse(onboardingRequest);
    }

    @Transactional
    public OnboardingRequestResponse reInitiate(Long onboardingRequestId, Authentication authentication) {
        ErmOnboardingRequest onboardingRequest = onboardingRequestRepository.findById(onboardingRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Onboarding request not found"));

        if (onboardingRequest.getWorkflowStage() != OnboardingWorkflowStage.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only rejected requests can be re-initiated");
        }

        ensureAnyHr(authentication);
        String actor = authentication.getName();
        if (!actor.equalsIgnoreCase(onboardingRequest.getCreatedByUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only submitted user can re-initiate the request");
        }

        if (!StringUtils.hasText(onboardingRequest.getDesignationRoleName()) || onboardingRequest.getReportingManagerUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Designation and reporting manager are required before re-initiation");
        }

        ManagerAssignment managerAssignment = resolveManagerAssignment(
                onboardingRequest.getDesignationRoleName(),
                onboardingRequest.getReportingManagerUserId()
        );

        onboardingRequest.setWorkflowStage(OnboardingWorkflowStage.HR_SUBMITTED);
        onboardingRequest.setInterviewStage(OnboardingInterviewStage.PENDING);
        onboardingRequest.setHrActionBy(actor);
        onboardingRequest.setHrActionAt(LocalDateTime.now());
        onboardingRequest.setHrComment("Request re-initiated by submitter");
        onboardingRequest.setReportingManagerUsername(managerAssignment.manager().getUsername());
        onboardingRequest.setReportingManagerFullName(managerAssignment.manager().getFullName());
        onboardingRequest.setReportingManagerRoleName(managerAssignment.managerRoleName());

        onboardingRequest.setHeadHrActionBy(null);
        onboardingRequest.setHeadHrActionAt(null);
        onboardingRequest.setHeadHrComment(null);
        onboardingRequest.setChroActionBy(null);
        onboardingRequest.setChroActionAt(null);
        onboardingRequest.setChroComment(null);
        onboardingRequest.setSuperAdminActionBy(null);
        onboardingRequest.setSuperAdminActionAt(null);
        onboardingRequest.setSuperAdminComment(null);
        onboardingRequest.setGeneratedEmployeeId(null);
        onboardingRequest.setGeneratedEmailAddress(null);

        onboardingRequest = onboardingRequestRepository.save(onboardingRequest);
        appendTrail(onboardingRequest, "HR Re-Initiation", actor, "Re-initiated", onboardingRequest.getHrComment(), onboardingRequest.getHrActionAt());
        return toResponse(onboardingRequest);
    }

    private void applyActionForStage(ErmOnboardingRequest onboardingRequest,
                                     OnboardingActionDecision decision,
                                     String actor,
                                     String comment,
                                     LocalDateTime now,
                                     String referBackStageLabel,
                                     OnboardingWorkflowStage nextApproveStage,
                                     OnboardingWorkflowStage currentStage) {
        if (decision == OnboardingActionDecision.REFER_BACK) {
            if (currentStage == OnboardingWorkflowStage.HR_SUBMITTED) {
                onboardingRequest.setHeadHrActionBy(actor);
                onboardingRequest.setHeadHrActionAt(now);
                onboardingRequest.setHeadHrComment(comment);
            } else if (currentStage == OnboardingWorkflowStage.HEAD_HR_APPROVED) {
                onboardingRequest.setChroActionBy(actor);
                onboardingRequest.setChroActionAt(now);
                onboardingRequest.setChroComment(comment);
            } else if (currentStage == OnboardingWorkflowStage.CHRO_APPROVED) {
                onboardingRequest.setSuperAdminActionBy(actor);
                onboardingRequest.setSuperAdminActionAt(now);
                onboardingRequest.setSuperAdminComment(comment);
            }
            onboardingRequest.setWorkflowStage(OnboardingWorkflowStage.REFER_BACK);
            onboardingRequest.setReferBackBy(actor);
            onboardingRequest.setReferBackAt(now);
            onboardingRequest.setReferBackComment(comment);
            onboardingRequest.setReferBackStage(referBackStageLabel);
            return;
        }

        if (currentStage == OnboardingWorkflowStage.HR_SUBMITTED) {
            onboardingRequest.setHeadHrActionBy(actor);
            onboardingRequest.setHeadHrActionAt(now);
            onboardingRequest.setHeadHrComment(comment);
        } else if (currentStage == OnboardingWorkflowStage.HEAD_HR_APPROVED) {
            onboardingRequest.setChroActionBy(actor);
            onboardingRequest.setChroActionAt(now);
            onboardingRequest.setChroComment(comment);
        } else if (currentStage == OnboardingWorkflowStage.CHRO_APPROVED) {
            onboardingRequest.setSuperAdminActionBy(actor);
            onboardingRequest.setSuperAdminActionAt(now);
            onboardingRequest.setSuperAdminComment(comment);
        }

        onboardingRequest.setWorkflowStage(decision == OnboardingActionDecision.APPROVE ? nextApproveStage : OnboardingWorkflowStage.REJECTED);
    }

    @Transactional
    public OnboardingRequestResponse sendReminder(Long onboardingRequestId, Authentication authentication) {
        ErmOnboardingRequest onboardingRequest = onboardingRequestRepository.findById(onboardingRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Onboarding request not found"));

        OnboardingWorkflowStage stage = onboardingRequest.getWorkflowStage();
        if (stage == OnboardingWorkflowStage.SUPER_ADMIN_APPROVED || stage == OnboardingWorkflowStage.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reminder can only be sent for pending requests");
        }

        String actor = authentication.getName();
        if (!actor.equalsIgnoreCase(onboardingRequest.getCreatedByUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the requester can send a reminder");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = onboardingRequest.getCreatedAt();
        if (createdAt != null && java.time.Duration.between(createdAt, now).toHours() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reminder can only be sent after the request has been pending for at least 8 hours");
        }

        LocalDateTime lastReminderAt = onboardingRequest.getLastReminderAt();
        if (lastReminderAt != null && java.time.Duration.between(lastReminderAt, now).toHours() < 4) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "You can only send a reminder once every 4 hours");
        }

        onboardingRequest.setLastReminderAt(now);
        onboardingRequest.setReminderCount(onboardingRequest.getReminderCount() + 1);
        return toResponse(onboardingRequestRepository.save(onboardingRequest));
    }

    @Transactional(readOnly = true)
    public List<OnboardingRequestResponse> getPendingRequestsForApprover(Authentication authentication) {
        List<OnboardingWorkflowStage> stages = new ArrayList<>();
        if (hasAnyAuthority(authentication, "ROLE_HR_HEAD")) {
            stages.add(OnboardingWorkflowStage.HR_SUBMITTED);
        }
        if (hasAnyAuthority(authentication, "ROLE_CHRO")) {
            stages.add(OnboardingWorkflowStage.HEAD_HR_APPROVED);
        }
        if (hasAnyAuthority(authentication, "ROLE_SUPER_ADMIN")) {
            stages.add(OnboardingWorkflowStage.CHRO_APPROVED);
        }
        if (stages.isEmpty()) {
            return List.of();
        }
        return onboardingRequestRepository.findAllByWorkflowStageInOrderByCreatedAtDesc(stages)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OnboardingDesignationOptionResponse> listDesignationOptions() {
        return designationHierarchyRepository.findAllByActiveTrueOrderBySortOrderAscDesignationRoleNameAsc().stream()
                .map(item -> new OnboardingDesignationOptionResponse(item.getDesignationRoleName(), item.getReportsToRoleName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OnboardingManagerOptionsResponse getManagerOptions(String designationRoleName) {
        if (!StringUtils.hasText(designationRoleName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Designation is required");
        }

        ErmDesignationHierarchy hierarchy = designationHierarchyRepository
                .findByDesignationRoleNameIgnoreCaseAndActiveTrue(designationRoleName.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid designation selected"));

        List<OnboardingManagerOptionResponse> managers = userRepository.findActiveUsersByRoleName(hierarchy.getReportsToRoleName()).stream()
                .map(user -> new OnboardingManagerOptionResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName(),
                        user.getEmail()
                ))
                .toList();

        return new OnboardingManagerOptionsResponse(
                hierarchy.getDesignationRoleName(),
                hierarchy.getReportsToRoleName(),
                managers
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<OnboardingRequestResponse> list(String workflowStage, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(safePage, safeSize);
        org.springframework.data.domain.Page<ErmOnboardingRequest> pageResult;
        if (StringUtils.hasText(workflowStage)) {
            pageResult = onboardingRequestRepository.findAllByWorkflowStageOrderByCreatedAtDesc(
                    OnboardingWorkflowStage.fromValue(workflowStage), pageable);
        } else {
            pageResult = onboardingRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return PagedResponse.from(pageResult.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public OnboardingRequestResponse getById(Long onboardingRequestId) {
        return onboardingRequestRepository.findById(onboardingRequestId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Onboarding request not found"));
    }

    private void applyCreateRequest(ErmOnboardingRequest onboardingRequest, OnboardingRequestCreateRequest request, String actor) {
        ManagerAssignment managerAssignment = resolveManagerAssignment(request.designationRoleName(), request.reportingManagerUserId());

        onboardingRequest.setFirstName(normalizeName(request.firstName()));
        onboardingRequest.setLastName(normalizeName(request.lastName()));
        onboardingRequest.setAadhaarCardNumber(request.aadhaarCardNumber().trim());
        onboardingRequest.setPanCardNumber(request.panCardNumber().trim().toUpperCase(Locale.ROOT));
        onboardingRequest.setPersonalEmailAddress(request.personalEmailAddress().trim().toLowerCase(Locale.ROOT));
        onboardingRequest.setPermanentAddress(request.permanentAddress().trim());
        onboardingRequest.setPhoneNumber(request.phoneNumber().trim());
        onboardingRequest.setDesignationRoleName(managerAssignment.designationRoleName());
        onboardingRequest.setReportingManagerUserId(managerAssignment.manager().getId());
        onboardingRequest.setReportingManagerUsername(managerAssignment.manager().getUsername());
        onboardingRequest.setReportingManagerFullName(managerAssignment.manager().getFullName());
        onboardingRequest.setReportingManagerRoleName(managerAssignment.managerRoleName());
        onboardingRequest.setEducationQualification(normalizeOptional(request.educationQualification()));
        onboardingRequest.setInterviewStage(OnboardingInterviewStage.PENDING);
        onboardingRequest.setWorkflowStage(OnboardingWorkflowStage.HR_SUBMITTED);
        onboardingRequest.setCreatedByUsername(actor);
        onboardingRequest.setHrActionBy(actor);
        onboardingRequest.setHrActionAt(LocalDateTime.now());
        onboardingRequest.setHrComment(normalizeOptional(request.comment()));
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
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reporting manager must be assigned as " + managerRoleName
            );
        }

        return new ManagerAssignment(hierarchy.getDesignationRoleName(), managerRoleName, manager);
    }

    private void assignGeneratedIdentity(ErmOnboardingRequest onboardingRequest) {
        String employeeId = generateEmployeeId(onboardingRequest.getId());
        onboardingRequest.setGeneratedEmployeeId(employeeId);
        onboardingRequest.setGeneratedEmailAddress(generateEmailAddress(employeeId));
    }

    private void createEmployeeProfile(ErmOnboardingRequest onboardingRequest) {
        if (!StringUtils.hasText(onboardingRequest.getGeneratedEmployeeId()) || !StringUtils.hasText(onboardingRequest.getGeneratedEmailAddress())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Generated employee identity is missing");
        }

        String username = onboardingRequest.getGeneratedEmployeeId().toLowerCase(Locale.ROOT);
        String email = onboardingRequest.getGeneratedEmailAddress().toLowerCase(Locale.ROOT);
        ErmUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseGet(ErmUser::new);

        user.setUsername(username);
        user.setEmail(email);
        user.setFullName((onboardingRequest.getFirstName() + " " + onboardingRequest.getLastName()).trim());
        user.setDepartment(resolveDepartment(onboardingRequest));
        user.setEmploymentStatus("Active");
        user.setActive(true);
        user.setReportingManagerUserId(onboardingRequest.getReportingManagerUserId());
        user.setReportingManagerRoleName(onboardingRequest.getReportingManagerRoleName());
        if (!StringUtils.hasText(user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(username));
        }

        ErmRole role = roleRepository.findByNameIgnoreCase(onboardingRequest.getDesignationRoleName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Designation role not found"));
        user.setRoles(new java.util.HashSet<>(java.util.Set.of(role)));
        user.setPrimaryRoleId(role.getId());
        userRepository.save(user);
    }

    private String resolveDepartment(ErmOnboardingRequest onboardingRequest) {
        if (onboardingRequest.getReportingManagerUserId() == null) {
            return "General";
        }
        return userRepository.findById(onboardingRequest.getReportingManagerUserId())
                .map(ErmUser::getDepartment)
                .filter(StringUtils::hasText)
                .orElse("General");
    }

    private String generateEmployeeId(Long onboardingRequestId) {
        if (onboardingRequestId == null) {
            throw new IllegalStateException("Onboarding request id must be available before generating employee id");
        }
        return String.format(Locale.ROOT, "EMP-%06d", onboardingRequestId);
    }

    private String generateEmailAddress(String employeeId) {
        return employeeId.toLowerCase(Locale.ROOT) + "@" + EMPLOYEE_EMAIL_DOMAIN;
    }

    private String normalizeName(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void ensureAnyHr(Authentication authentication) {
        if (hasAnyAuthority(authentication, "ROLE_SENIOR_HR", "ROLE_JUNIOR_HR")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only HR can create onboarding requests");
    }

    private void ensureHeadHr(Authentication authentication) {
        if (hasAnyAuthority(authentication, "ROLE_HR_HEAD")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Head HR can action this stage");
    }

    private void ensureChro(Authentication authentication) {
        if (hasAnyAuthority(authentication, "ROLE_CHRO")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only CHRO can action this stage");
    }

    private void ensureSuperAdmin(Authentication authentication) {
        if (hasAnyAuthority(authentication, "ROLE_SUPER_ADMIN")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Super Admin can action this stage");
    }

    private boolean hasAnyAuthority(Authentication authentication, String... authorities) {
        return authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .anyMatch(authority -> List.of(authorities).contains(authority));
    }

    private List<OnboardingApprovalTrailItem> buildTrail(ErmOnboardingRequest onboardingRequest) {
        if (onboardingRequestCommentRepository == null) {
            return List.of();
        }
        List<ErmOnboardingRequestComment> historyEntries =
                onboardingRequestCommentRepository.findAllByOnboardingRequestIdOrderByActionAtAscIdAsc(onboardingRequest.getId());
        if (!historyEntries.isEmpty()) {
            return historyEntries.stream()
                    .map(entry -> new OnboardingApprovalTrailItem(
                            entry.getStep(),
                            entry.getActor(),
                            entry.getDecision(),
                            entry.getCommentText(),
                            entry.getActionAt()
                    ))
                    .toList();
        }

        List<OnboardingApprovalTrailItem> trail = new ArrayList<>();
        if (StringUtils.hasText(onboardingRequest.getHrActionBy())) {
            addTrailItem(trail, "HR Submission", onboardingRequest.getHrActionBy(), "Submitted", onboardingRequest.getHrComment(), onboardingRequest.getHrActionAt());
        }
        if (StringUtils.hasText(onboardingRequest.getHeadHrActionBy())) {
            addTrailItem(trail, "Head HR Review", onboardingRequest.getHeadHrActionBy(), headHrDecision(onboardingRequest), onboardingRequest.getHeadHrComment(), onboardingRequest.getHeadHrActionAt());
        }
        if (StringUtils.hasText(onboardingRequest.getChroActionBy())) {
            addTrailItem(trail, "CHRO Review", onboardingRequest.getChroActionBy(), chroDecision(onboardingRequest), onboardingRequest.getChroComment(), onboardingRequest.getChroActionAt());
        }
        if (StringUtils.hasText(onboardingRequest.getSuperAdminActionBy())) {
            addTrailItem(trail, "Super Admin Review", onboardingRequest.getSuperAdminActionBy(), onboardingRequest.getWorkflowStage() == OnboardingWorkflowStage.REFER_BACK ? "Refer Back" : onboardingRequest.getWorkflowStage() == OnboardingWorkflowStage.REJECTED ? "Rejected" : "Approved", onboardingRequest.getSuperAdminComment(), onboardingRequest.getSuperAdminActionAt());
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

    private void addTrailItem(List<OnboardingApprovalTrailItem> trail, String step, String actor, String decision, String comment, LocalDateTime actionAt) {
        trail.add(new OnboardingApprovalTrailItem(step, actor, decision, comment, actionAt));
    }

    private void appendTrail(ErmOnboardingRequest onboardingRequest, String step, String actor, String decision, String comment, LocalDateTime actionAt) {
        if (onboardingRequest.getId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store onboarding comment history");
        }
        ErmOnboardingRequestComment history = new ErmOnboardingRequestComment();
        history.setOnboardingRequestId(onboardingRequest.getId());
        history.setStep(step);
        history.setActor(actor);
        history.setDecision(decision);
        history.setCommentText(comment);
        history.setActionAt(actionAt == null ? LocalDateTime.now() : actionAt);
        if (onboardingRequestCommentRepository != null) {
            onboardingRequestCommentRepository.save(history);
        }
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

    private String stageLabel(OnboardingWorkflowStage stage) {
        if (stage == OnboardingWorkflowStage.HR_SUBMITTED) {
            return "Head HR Review";
        }
        if (stage == OnboardingWorkflowStage.HEAD_HR_APPROVED) {
            return "CHRO Review";
        }
        if (stage == OnboardingWorkflowStage.CHRO_APPROVED) {
            return "Super Admin Review";
        }
        return "Review";
    }

    private String headHrDecision(ErmOnboardingRequest onboardingRequest) {
        if (onboardingRequest.getWorkflowStage() == OnboardingWorkflowStage.REFER_BACK && StringUtils.hasText(onboardingRequest.getHeadHrActionBy())) {
            return "Refer Back";
        }
        if (StringUtils.hasText(onboardingRequest.getChroActionBy())
                || StringUtils.hasText(onboardingRequest.getSuperAdminActionBy())
                || onboardingRequest.getWorkflowStage() == OnboardingWorkflowStage.CHRO_APPROVED
                || onboardingRequest.getWorkflowStage() == OnboardingWorkflowStage.SUPER_ADMIN_APPROVED) {
            return "Approved";
        }
        if (onboardingRequest.getWorkflowStage() == OnboardingWorkflowStage.REJECTED) {
            return "Rejected";
        }
        return "Approved";
    }

    private String chroDecision(ErmOnboardingRequest onboardingRequest) {
        if (onboardingRequest.getWorkflowStage() == OnboardingWorkflowStage.REFER_BACK && StringUtils.hasText(onboardingRequest.getChroActionBy())) {
            return "Refer Back";
        }
        if (StringUtils.hasText(onboardingRequest.getSuperAdminActionBy())
                || onboardingRequest.getWorkflowStage() == OnboardingWorkflowStage.SUPER_ADMIN_APPROVED) {
            return "Approved";
        }
        if (onboardingRequest.getWorkflowStage() == OnboardingWorkflowStage.REJECTED) {
            return "Rejected";
        }
        return "Approved";
    }

    private OnboardingRequestResponse toResponse(ErmOnboardingRequest onboardingRequest) {
        return new OnboardingRequestResponse(
                onboardingRequest.getId(),
                onboardingRequest.getFirstName(),
                onboardingRequest.getLastName(),
                onboardingRequest.getAadhaarCardNumber(),
                onboardingRequest.getPanCardNumber(),
                onboardingRequest.getPersonalEmailAddress(),
                onboardingRequest.getPermanentAddress(),
                onboardingRequest.getPhoneNumber(),
                onboardingRequest.getDesignationRoleName(),
                onboardingRequest.getReportingManagerUserId(),
                onboardingRequest.getReportingManagerUsername(),
                onboardingRequest.getReportingManagerFullName(),
                onboardingRequest.getReportingManagerRoleName(),
                onboardingRequest.getEducationQualification(),
                onboardingRequest.getInterviewStage(),
                onboardingRequest.getWorkflowStage(),
                onboardingRequest.getCreatedByUsername(),
                onboardingRequest.getGeneratedEmployeeId(),
                onboardingRequest.getGeneratedEmailAddress(),
                onboardingRequest.getReferBackBy(),
                onboardingRequest.getReferBackStage(),
                onboardingRequest.getReferBackComment(),
                onboardingRequest.getReferBackAt(),
                buildTrail(onboardingRequest),
                onboardingRequest.getCreatedAt(),
                onboardingRequest.getUpdatedAt(),
                onboardingRequest.getLastReminderAt(),
                onboardingRequest.getReminderCount()
        );
    }

    private record ManagerAssignment(String designationRoleName, String managerRoleName, ErmUser manager) {
    }
}
