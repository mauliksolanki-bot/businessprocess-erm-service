package com.org.erm.service;

import com.org.erm.dto.request.LeaveApplyRequest;
import com.org.erm.dto.request.LeaveActionRequest;
import com.org.erm.dto.response.LeavePolicyResponse;
import com.org.erm.dto.response.LeaveRequestResponse;
import com.org.erm.model.ErmLeavePolicy;
import com.org.erm.model.ErmLeaveRequest;
import com.org.erm.model.ErmUser;
import com.org.erm.model.LeaveRequestStatus;
import com.org.erm.repository.ErmLeavePolicyRepository;
import com.org.erm.repository.ErmLeaveRequestRepository;
import com.org.erm.repository.ErmUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class LeaveService {

    private static final Set<LeaveRequestStatus> APPLICABLE_STATUSES = EnumSet.of(LeaveRequestStatus.PENDING, LeaveRequestStatus.APPROVED);

    private final ErmLeavePolicyRepository leavePolicyRepository;
    private final ErmLeaveRequestRepository leaveRequestRepository;
    private final ErmUserRepository userRepository;

    public LeaveService(ErmLeavePolicyRepository leavePolicyRepository,
                        ErmLeaveRequestRepository leaveRequestRepository,
                        ErmUserRepository userRepository) {
        this.leavePolicyRepository = leavePolicyRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<LeavePolicyResponse> listEnabledPolicies() {
        return leavePolicyRepository.findAllByEnabledTrueOrderByLeaveCategoryAsc().stream()
                .sorted(Comparator.comparing(policy -> policy.getLeaveCategory().ordinal()))
                .map(this::toPolicyResponse)
                .toList();
    }

    @Transactional
    public LeaveRequestResponse applyLeave(LeaveApplyRequest request, String username) {
        ErmUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        ErmUser manager = resolveReportingManager(user);

        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be on or after start date");
        }

        ErmLeavePolicy policy = leavePolicyRepository.findByLeaveCategory(request.leaveCategory())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leave policy is not configured"));
        if (!policy.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected leave category is disabled");
        }

        long requestedDays = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        if (requestedDays > policy.getMaxDaysPerYear()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested days exceed configured yearly limit");
        }

        LocalDate yearStart = LocalDate.of(request.startDate().getYear(), 1, 1);
        LocalDate yearEnd = LocalDate.of(request.startDate().getYear(), 12, 31);
        long usedDays = leaveRequestRepository.sumRequestedDaysByYear(
                user.getId(),
                request.leaveCategory(),
                APPLICABLE_STATUSES,
                yearStart,
                yearEnd
        );
        if (usedDays + requestedDays > policy.getMaxDaysPerYear()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested days exceed remaining yearly balance for this category");
        }

        ErmLeaveRequest leaveRequest = new ErmLeaveRequest();
        leaveRequest.setEmployeeUserId(user.getId());
        leaveRequest.setEmployeeUsername(user.getUsername());
        leaveRequest.setEmployeeFullName(user.getFullName());
        leaveRequest.setApproverManagerUserId(manager.getId());
        leaveRequest.setApproverManagerUsername(manager.getUsername());
        leaveRequest.setApproverManagerFullName(manager.getFullName());
        leaveRequest.setLeaveCategory(request.leaveCategory());
        leaveRequest.setStartDate(request.startDate());
        leaveRequest.setEndDate(request.endDate());
        leaveRequest.setRequestedDays((int) requestedDays);
        leaveRequest.setReason(normalizeRequired(request.reason(), "Reason is required"));
        leaveRequest.setRequestStatus(LeaveRequestStatus.PENDING);

        return toLeaveResponse(leaveRequestRepository.save(leaveRequest));
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> myRequests(String username) {
        ErmUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return leaveRequestRepository.findAllByEmployeeUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toLeaveResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> approverRequests(String username) {
        ErmUser manager = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return leaveRequestRepository.findAllByApproverManagerUserIdOrderByCreatedAtDesc(manager.getId()).stream()
                .map(this::toLeaveResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasReportees(String username) {
        ErmUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return !userRepository.findAllByReportingManagerUserIdOrderByFullNameAsc(user.getId()).isEmpty();
    }

    @Transactional
    public LeaveRequestResponse takeAction(Long leaveRequestId, LeaveActionRequest request, String username) {
        ErmUser manager = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        ErmLeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));

        if (leaveRequest.getApproverManagerUserId() == null || !leaveRequest.getApproverManagerUserId().equals(manager.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to action this leave request");
        }
        if (leaveRequest.getRequestStatus() != LeaveRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leave request is already closed");
        }

        leaveRequest.setRequestStatus(request.decision().name().equalsIgnoreCase("APPROVE")
                ? LeaveRequestStatus.APPROVED
                : LeaveRequestStatus.REJECTED);
        leaveRequest.setApproverComment(normalizeRequired(request.comment(), "Comment is required"));
        leaveRequest.setApproverActionAt(LocalDateTime.now());

        return toLeaveResponse(leaveRequestRepository.save(leaveRequest));
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private ErmUser resolveReportingManager(ErmUser user) {
        if (user.getReportingManagerUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reporting manager is not configured for this user");
        }
        return userRepository.findById(user.getReportingManagerUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reporting manager not found"));
    }

    private LeavePolicyResponse toPolicyResponse(ErmLeavePolicy policy) {
        return new LeavePolicyResponse(
                policy.getId(),
                policy.getLeaveCategory().getLabel(),
                policy.getDisplayName(),
                policy.getMaxDaysPerYear(),
                policy.isEnabled(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }

    private LeaveRequestResponse toLeaveResponse(ErmLeaveRequest request) {
        return new LeaveRequestResponse(
                request.getId(),
                request.getEmployeeUserId(),
                request.getEmployeeUsername(),
                request.getEmployeeFullName(),
                request.getApproverManagerUserId(),
                request.getApproverManagerUsername(),
                request.getApproverManagerFullName(),
                request.getLeaveCategory().getLabel(),
                request.getStartDate(),
                request.getEndDate(),
                request.getRequestedDays(),
                request.getReason(),
                request.getRequestStatus().name(),
                request.getApproverComment(),
                request.getApproverActionAt(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
