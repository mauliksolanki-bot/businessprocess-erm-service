package com.org.erm.service;

import com.org.erm.dto.response.DashboardSummaryResponse;
import com.org.erm.dto.response.SelfDashboardResponse;
import com.org.erm.dto.response.SelfProjectAssignmentResponse;
import com.org.erm.dto.response.TeamLeadDashboardResponse;
import com.org.erm.dto.response.TeamLeadLeaveItemResponse;
import com.org.erm.dto.response.TeamLeadProjectItemResponse;
import com.org.erm.dto.response.TeamLeadProjectMemberResponse;
import com.org.erm.model.ErmLeaveRequest;
import com.org.erm.model.ErmProjectAllocation;
import com.org.erm.model.ErmProjectRequest;
import com.org.erm.model.ErmRole;
import com.org.erm.model.ErmUser;
import com.org.erm.model.LeaveRequestStatus;
import com.org.erm.model.ProjectAllocationStatus;
import com.org.erm.model.OnboardingWorkflowStage;
import com.org.erm.repository.ErmLeaveRequestRepository;
import com.org.erm.repository.ErmProjectAllocationRepository;
import com.org.erm.repository.ErmProjectRequestRepository;
import com.org.erm.repository.ErmEmployeeProfileUpdateRequestRepository;
import com.org.erm.repository.ErmOnboardingRequestRepository;
import com.org.erm.repository.ErmUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Set<OnboardingWorkflowStage> OPEN_ONBOARDING_STAGES = EnumSet.of(
            OnboardingWorkflowStage.HR_SUBMITTED,
            OnboardingWorkflowStage.HEAD_HR_APPROVED,
            OnboardingWorkflowStage.CHRO_APPROVED
    );

    private final ErmUserRepository userRepository;
    private final ErmOnboardingRequestRepository onboardingRequestRepository;
    private final ErmEmployeeProfileUpdateRequestRepository employeeProfileUpdateRequestRepository;
    private final ErmLeaveRequestRepository leaveRequestRepository;
    private final ErmProjectAllocationRepository projectAllocationRepository;
    private final ErmProjectRequestRepository projectRequestRepository;

    public DashboardService(ErmUserRepository userRepository,
                            ErmOnboardingRequestRepository onboardingRequestRepository,
                            ErmEmployeeProfileUpdateRequestRepository employeeProfileUpdateRequestRepository,
                            ErmLeaveRequestRepository leaveRequestRepository,
                            ErmProjectAllocationRepository projectAllocationRepository,
                            ErmProjectRequestRepository projectRequestRepository) {
        this.userRepository = userRepository;
        this.onboardingRequestRepository = onboardingRequestRepository;
        this.employeeProfileUpdateRequestRepository = employeeProfileUpdateRequestRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.projectAllocationRepository = projectAllocationRepository;
        this.projectRequestRepository = projectRequestRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long totalEmployees = userRepository.count();
        long openOnboardingRequests = onboardingRequestRepository.countByWorkflowStageIn(OPEN_ONBOARDING_STAGES);
        long openEmployeeDataRequests = employeeProfileUpdateRequestRepository.countByWorkflowStageNotIn(Set.of(
                OnboardingWorkflowStage.SUPER_ADMIN_APPROVED,
                OnboardingWorkflowStage.CANCELLED,
                OnboardingWorkflowStage.REJECTED
        ));
        return new DashboardSummaryResponse(totalEmployees, openOnboardingRequests, openEmployeeDataRequests);
    }

    @Transactional(readOnly = true)
    public TeamLeadDashboardResponse getTeamLeadDashboard(String username) {
        ErmUser teamLead = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!hasTeamLeadAccess(teamLead)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view team lead dashboard");
        }

        List<ErmUser> directReports = userRepository.findAllByReportingManagerUserIdOrderByFullNameAsc(teamLead.getId());
        List<Long> directReportIds = directReports.stream()
                .map(ErmUser::getId)
                .toList();

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        List<ErmLeaveRequest> leaveRequests = directReportIds.isEmpty()
                ? List.of()
                : leaveRequestRepository.findAllByEmployeeUserIdInAndRequestStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(
                directReportIds,
                LeaveRequestStatus.APPROVED,
                monthEnd,
                monthStart
        );
        List<TeamLeadLeaveItemResponse> leaveItems = leaveRequests.stream()
                .map(request -> new TeamLeadLeaveItemResponse(
                        request.getId(),
                        request.getEmployeeUserId(),
                        request.getEmployeeFullName(),
                        request.getEmployeeUsername(),
                        request.getLeaveCategory().getLabel(),
                        request.getStartDate(),
                        request.getEndDate(),
                        request.getRequestedDays(),
                        request.getRequestStatus().name()
                ))
                .toList();

        List<ErmProjectAllocation> allocations = directReportIds.isEmpty()
                ? List.of()
                : projectAllocationRepository.findAllByEmployeeUserIdInAndStatus(directReportIds, ProjectAllocationStatus.ACTIVE);
        Map<Long, List<ErmProjectAllocation>> allocationsByProject = allocations.stream()
                .collect(Collectors.groupingBy(ErmProjectAllocation::getProjectRequestId, LinkedHashMap::new, Collectors.toList()));

        List<Long> projectIds = new ArrayList<>(allocationsByProject.keySet());
        List<ErmProjectRequest> projects = projectIds.isEmpty()
                ? List.of()
                : projectRequestRepository.findAllById(projectIds);
        Map<Long, ErmProjectRequest> projectById = projects.stream()
                .collect(Collectors.toMap(ErmProjectRequest::getId, project -> project, (left, right) -> left, LinkedHashMap::new));

        List<TeamLeadProjectItemResponse> teamProjects = projectIds.stream()
                .map(projectById::get)
                .filter(project -> project != null)
                .sorted(Comparator.comparing(ErmProjectRequest::getProjectName, String.CASE_INSENSITIVE_ORDER))
                .map(project -> {
                    List<ErmProjectAllocation> projectAllocations = allocationsByProject.getOrDefault(project.getId(), List.of());
                    Map<Long, BigDecimal> memberPercentages = new LinkedHashMap<>();
                    Map<Long, ErmUser> memberUsers = new LinkedHashMap<>();
                    for (ErmProjectAllocation allocation : projectAllocations) {
                        memberPercentages.merge(allocation.getEmployeeUserId(), allocation.getAllocationPercent(), BigDecimal::add);
                        userRepository.findById(allocation.getEmployeeUserId()).ifPresent(user -> memberUsers.putIfAbsent(user.getId(), user));
                    }

                    List<TeamLeadProjectMemberResponse> members = memberUsers.values().stream()
                            .sorted(Comparator.comparing(ErmUser::getFullName, String.CASE_INSENSITIVE_ORDER))
                            .map(user -> new TeamLeadProjectMemberResponse(
                                    user.getId(),
                                    user.getFullName(),
                                    user.getUsername(),
                                    memberPercentages.getOrDefault(user.getId(), BigDecimal.ZERO)
                            ))
                            .toList();

                    BigDecimal activeAllocationPercent = members.stream()
                            .map(TeamLeadProjectMemberResponse::allocationPercent)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new TeamLeadProjectItemResponse(
                            project.getId(),
                            project.getProjectName(),
                            project.getProjectCode(),
                            project.getClientName(),
                            project.getProjectType(),
                            project.getPriority(),
                            project.getPlannedStartDate(),
                            project.getPlannedEndDate(),
                            project.getWorkflowStage().getLabel(),
                            activeAllocationPercent,
                            members.size(),
                            members
                    );
                })
                .toList();

        long currentMonthLeaveCount = leaveItems.size();
        long activeTeamProjectCount = teamProjects.size();

        return new TeamLeadDashboardResponse(
                teamLead.getId(),
                teamLead.getUsername(),
                teamLead.getFullName(),
                resolveDesignation(teamLead),
                directReports.size(),
                currentMonthLeaveCount,
                activeTeamProjectCount,
                leaveItems,
                teamProjects
        );
    }

    @Transactional(readOnly = true)
    public SelfDashboardResponse getSelfDashboard(String username) {
        ErmUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String reportingManagerFullName = null;
        if (user.getReportingManagerUserId() != null) {
            reportingManagerFullName = userRepository.findById(user.getReportingManagerUserId())
                    .map(manager -> StringUtils.hasText(manager.getFullName()) ? manager.getFullName().trim() : manager.getUsername())
                    .orElse(null);
        }

        List<SelfProjectAssignmentResponse> currentProjects = projectAllocationRepository
                .findAllByEmployeeUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), ProjectAllocationStatus.ACTIVE)
                .stream()
                .map(item -> new SelfProjectAssignmentResponse(
                        item.getId(),
                        item.getAllocationCode(),
                        item.getProjectRequestId(),
                        item.getProjectName(),
                        item.getProjectCode(),
                        item.getAllocationType().getLabel(),
                        item.getAllocationPercent(),
                        item.getStartDate(),
                        item.getEndDate(),
                        item.getStatus().getLabel(),
                        item.getUpdatedAt()
                ))
                .toList();

        return new SelfDashboardResponse(
                user.getId(),
                user.getUsername(),
                StringUtils.hasText(user.getFullName()) ? user.getFullName().trim() : user.getUsername(),
                resolveDesignation(user),
                reportingManagerFullName,
                user.getReportingManagerRoleName(),
                currentProjects
        );
    }

    private boolean hasTeamLeadAccess(ErmUser user) {
        return user.getRoles().stream()
                .map(ErmRole::getName)
                .map(String::toLowerCase)
                .anyMatch(role -> role.equals("team lead")
                        || role.equals("role_team_lead")
                        || role.equals("it support lead")
                        || role.equals("role_it_support_lead")
                        || role.equals("super admin")
                        || role.equals("admin"));
    }

    private String resolveDesignation(ErmUser user) {
        return user.getRoles().stream()
                .map(ErmRole::getName)
                .filter(StringUtils::hasText)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .findFirst()
                .orElse("Employee");
    }
}
