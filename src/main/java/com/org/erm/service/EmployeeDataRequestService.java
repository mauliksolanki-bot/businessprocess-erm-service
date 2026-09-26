package com.org.erm.service;

import com.org.erm.dto.request.EmployeeDesignationUpdateRequestCreateRequest;
import com.org.erm.dto.response.EmployeeDesignationUpdateRequestResponse;
import com.org.erm.dto.request.OnboardingActionRequest;
import com.org.erm.dto.response.OnboardingApprovalTrailItem;
import com.org.erm.model.ErmDesignationHierarchy;
import com.org.erm.model.ErmEmployeeDesignationRequest;
import com.org.erm.model.ErmRole;
import com.org.erm.model.ErmUser;
import com.org.erm.model.OnboardingActionDecision;
import com.org.erm.model.OnboardingWorkflowStage;
import com.org.erm.repository.ErmDesignationHierarchyRepository;
import com.org.erm.repository.ErmEmployeeDesignationRequestRepository;
import com.org.erm.repository.ErmRoleRepository;
import com.org.erm.repository.ErmUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeDataRequestService {

    private final ErmEmployeeDesignationRequestRepository requestRepository;
    private final ErmUserRepository userRepository;
    private final ErmRoleRepository roleRepository;
    private final ErmDesignationHierarchyRepository designationHierarchyRepository;
    private final MentionNotificationService mentionNotificationService;
    private final EmployeeIdService employeeIdService;
    private final EmployeeRoleReferenceService employeeRoleReferenceService;

    public EmployeeDataRequestService(ErmEmployeeDesignationRequestRepository requestRepository,
                                      ErmUserRepository userRepository,
                                      ErmRoleRepository roleRepository,
                                      ErmDesignationHierarchyRepository designationHierarchyRepository,
                                      MentionNotificationService mentionNotificationService,
                                      EmployeeIdService employeeIdService,
                                      EmployeeRoleReferenceService employeeRoleReferenceService) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.designationHierarchyRepository = designationHierarchyRepository;
        this.mentionNotificationService = mentionNotificationService;
        this.employeeIdService = employeeIdService;
        this.employeeRoleReferenceService = employeeRoleReferenceService;
    }

    @Transactional
    public EmployeeDesignationUpdateRequestResponse create(EmployeeDesignationUpdateRequestCreateRequest request,
                                                           Authentication authentication) {
        ensureSeniorHr(authentication);
        String actor = authentication.getName();

        ErmUser employee = userRepository.findById(request.employeeUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        ManagerAssignment managerAssignment = resolveManagerAssignment(request.designationRoleName(), request.reportingManagerUserId());
        if (employee.getId().equals(managerAssignment.manager().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee and reporting manager cannot be the same");
        }
        String currentDesignation = resolveCurrentDesignation(employee);
        if (currentDesignation.equalsIgnoreCase(managerAssignment.designationRoleName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested designation must be different from current designation");
        }

        ErmEmployeeDesignationRequest entity = new ErmEmployeeDesignationRequest();
        entity.setEmployeeUserId(employee.getId());
        entity.setEmployeeId(employee.getEmployeeId());
        entity.setEmployeeUsername(employee.getUsername());
        entity.setEmployeeFullName(employee.getFullName());
        entity.setCurrentDesignationRoleName(currentDesignation);
        entity.setRequestedDesignationRoleName(managerAssignment.designationRoleName());
        entity.setRequestedReportingManagerUserId(managerAssignment.manager().getId());
        entity.setRequestedReportingManagerEmployeeId(managerAssignment.manager().getEmployeeId());
        entity.setRequestedReportingManagerUsername(managerAssignment.manager().getUsername());
        entity.setRequestedReportingManagerFullName(managerAssignment.manager().getFullName());
        entity.setRequestedReportingManagerRoleName(managerAssignment.managerRoleName());
        entity.setWorkflowStage(OnboardingWorkflowStage.HR_SUBMITTED);
        entity.setCreatedByUsername(actor);
        entity.setHrActionBy(actor);
        entity.setHrActionAt(LocalDateTime.now());
        entity.setHrComment(normalizeOptional(request.comment()));
        entity = requestRepository.save(entity);
        mentionNotificationService.notifyMentions(
                actor,
                entity.getHrComment(),
                "EMPLOYEE_DATA_REQUEST",
                entity.getId(),
                actor + " mentioned you on employee data request #" + entity.getId(),
                "/employee-data"
        );
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDesignationUpdateRequestResponse> list(String workflowStage) {
        List<ErmEmployeeDesignationRequest> requests;
        if (StringUtils.hasText(workflowStage)) {
            requests = requestRepository.findAllByWorkflowStageOrderByCreatedAtDesc(OnboardingWorkflowStage.fromValue(workflowStage));
        } else {
            requests = requestRepository.findAllByOrderByCreatedAtDesc();
        }
        return requests.stream().map(this::toResponse).toList();
    }

    @Transactional
    public EmployeeDesignationUpdateRequestResponse takeAction(Long requestId,
                                                               OnboardingActionRequest request,
                                                               Authentication authentication) {
        ErmEmployeeDesignationRequest entity = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee data request not found"));

        OnboardingWorkflowStage stage = entity.getWorkflowStage();
        if (stage == OnboardingWorkflowStage.SUPER_ADMIN_APPROVED || stage == OnboardingWorkflowStage.REJECTED) {
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
                applyDesignationUpdate(entity);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow stage cannot be actioned");
        }

        entity = requestRepository.save(entity);
        mentionNotificationService.notifyMentions(
                actor,
                comment,
                "EMPLOYEE_DATA_REQUEST",
                entity.getId(),
                actor + " mentioned you on employee data request #" + entity.getId(),
                "/employee-data"
        );
        return toResponse(entity);
    }

    private void applyDesignationUpdate(ErmEmployeeDesignationRequest request) {
        ErmUser employee = userRepository.findById(request.getEmployeeUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        ManagerAssignment managerAssignment = resolveManagerAssignment(
                request.getRequestedDesignationRoleName(),
                request.getRequestedReportingManagerUserId()
        );
        if (employee.getId().equals(managerAssignment.manager().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee and reporting manager cannot be the same");
        }
        ErmRole role = roleRepository.findByNameIgnoreCase(managerAssignment.designationRoleName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Designation role not found"));

        employee.setRoles(new HashSet<>(java.util.Set.of(role)));
        employee.setPrimaryRoleId(role.getId());
        employeeIdService.refreshForPrimaryRole(employee);
        employee.setReportingManagerUserId(managerAssignment.manager().getId());
        employee.setReportingManagerEmployeeId(managerAssignment.manager().getEmployeeId());
        employee.setReportingManagerRoleName(managerAssignment.managerRoleName());
        employee.setDepartment(resolveDepartmentFromManager(managerAssignment.manager()));
        userRepository.save(employee);
        employeeRoleReferenceService.syncEmployeeIds(employee.getId());
    }

    private String resolveDepartmentFromManager(ErmUser manager) {
        return StringUtils.hasText(manager.getDepartment()) ? manager.getDepartment() : "General";
    }

    private String resolveCurrentDesignation(ErmUser employee) {
        return employee.getRoles().stream()
                .map(ErmRole::getName)
                .sorted(Comparator.naturalOrder())
                .findFirst()
                .orElse("Employee");
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

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void ensureSeniorHr(Authentication authentication) {
        if (hasAnyAuthority(authentication, "ROLE_SENIOR_HR")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Senior HR can create employee data requests");
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
                .map(authority -> authority.getAuthority())
                .anyMatch(authority -> List.of(authorities).contains(authority));
    }

    private List<OnboardingApprovalTrailItem> buildTrail(ErmEmployeeDesignationRequest request) {
        List<OnboardingApprovalTrailItem> trail = new ArrayList<>();
        if (StringUtils.hasText(request.getHrActionBy())) {
            trail.add(new OnboardingApprovalTrailItem(
                    "Senior HR Submission",
                    request.getHrActionBy(),
                    "Submitted",
                    request.getHrComment(),
                    request.getHrActionAt()
            ));
        }
        if (StringUtils.hasText(request.getHeadHrActionBy())) {
            trail.add(new OnboardingApprovalTrailItem(
                    "Head HR Review",
                    request.getHeadHrActionBy(),
                    request.getWorkflowStage() == OnboardingWorkflowStage.REJECTED ? "Rejected" : "Approved",
                    request.getHeadHrComment(),
                    request.getHeadHrActionAt()
            ));
        }
        if (StringUtils.hasText(request.getChroActionBy())) {
            trail.add(new OnboardingApprovalTrailItem(
                    "CHRO Review",
                    request.getChroActionBy(),
                    request.getWorkflowStage() == OnboardingWorkflowStage.REJECTED ? "Rejected" : "Approved",
                    request.getChroComment(),
                    request.getChroActionAt()
            ));
        }
        if (StringUtils.hasText(request.getSuperAdminActionBy())) {
            trail.add(new OnboardingApprovalTrailItem(
                    "Super Admin Review",
                    request.getSuperAdminActionBy(),
                    request.getWorkflowStage() == OnboardingWorkflowStage.REJECTED ? "Rejected" : "Approved",
                    request.getSuperAdminComment(),
                    request.getSuperAdminActionAt()
            ));
        }
        return trail;
    }

    private EmployeeDesignationUpdateRequestResponse toResponse(ErmEmployeeDesignationRequest request) {
        return new EmployeeDesignationUpdateRequestResponse(
                request.getId(),
                request.getEmployeeUserId(),
                request.getEmployeeUsername(),
                request.getEmployeeFullName(),
                request.getCurrentDesignationRoleName(),
                request.getRequestedDesignationRoleName(),
                request.getRequestedReportingManagerUserId(),
                request.getRequestedReportingManagerUsername(),
                request.getRequestedReportingManagerFullName(),
                request.getRequestedReportingManagerRoleName(),
                request.getWorkflowStage(),
                request.getCreatedByUsername(),
                buildTrail(request),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private record ManagerAssignment(String designationRoleName, String managerRoleName, ErmUser manager) {
    }
}
