package com.org.erm.service;

import com.org.erm.dto.request.AttendanceTimesheetActionRequest;
import com.org.erm.dto.request.AttendanceTimesheetDayRequest;
import com.org.erm.dto.request.AttendanceTimesheetUpsertRequest;
import com.org.erm.dto.response.AttendanceApprovalItemResponse;
import com.org.erm.dto.response.AttendanceAssignmentResponse;
import com.org.erm.dto.response.AttendanceApproverVisibilityResponse;
import com.org.erm.dto.response.AttendanceDayResponse;
import com.org.erm.dto.response.AttendanceTimesheetResponse;
import com.org.erm.dto.response.AttendanceWeekResponse;
import com.org.erm.model.AttendanceDecision;
import com.org.erm.model.AttendanceTimesheetStatus;
import com.org.erm.model.ErmAttendanceTimesheet;
import com.org.erm.model.ErmAttendanceTimesheetDay;
import com.org.erm.model.ErmLeaveRequest;
import com.org.erm.model.ErmProjectAllocation;
import com.org.erm.model.ErmUser;
import com.org.erm.model.LeaveRequestStatus;
import com.org.erm.model.ProjectAllocationStatus;
import com.org.erm.model.ProjectAllocationType;
import com.org.erm.repository.ErmAttendanceTimesheetDayRepository;
import com.org.erm.repository.ErmAttendanceTimesheetRepository;
import com.org.erm.repository.ErmLeaveRequestRepository;
import com.org.erm.repository.ErmProjectAllocationRepository;
import com.org.erm.repository.ErmUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private static final BigDecimal MAX_BILLABLE_HOURS_PER_DAY = new BigDecimal("8.00");

    private final ErmUserRepository userRepository;
    private final ErmProjectAllocationRepository projectAllocationRepository;
    private final ErmLeaveRequestRepository leaveRequestRepository;
    private final ErmAttendanceTimesheetRepository timesheetRepository;
    private final ErmAttendanceTimesheetDayRepository timesheetDayRepository;

    public AttendanceService(ErmUserRepository userRepository,
                             ErmProjectAllocationRepository projectAllocationRepository,
                             ErmLeaveRequestRepository leaveRequestRepository,
                             ErmAttendanceTimesheetRepository timesheetRepository,
                             ErmAttendanceTimesheetDayRepository timesheetDayRepository) {
        this.userRepository = userRepository;
        this.projectAllocationRepository = projectAllocationRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.timesheetRepository = timesheetRepository;
        this.timesheetDayRepository = timesheetDayRepository;
    }

    @Transactional(readOnly = true)
    public AttendanceWeekResponse getWeek(String username, LocalDate requestedWeekStart) {
        ErmUser user = resolveUser(username);
        LocalDate weekStart = normalizeWeekStart(requestedWeekStart);
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate editableUntil = LocalDate.now().minusDays(30);
        boolean editable = !weekEnd.isBefore(editableUntil);

        List<ErmProjectAllocation> activeAllocations = projectAllocationRepository
                .findAllByEmployeeUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), ProjectAllocationStatus.ACTIVE)
                .stream()
                .filter(allocation -> overlaps(allocation.getStartDate(), allocation.getEndDate(), weekStart, weekEnd))
                .toList();

        List<AttendanceAssignmentResponse> billableAssignments = toAssignmentResponses(activeAllocations.stream()
                .filter(allocation -> allocation.getAllocationType() == ProjectAllocationType.BILLABLE)
                .sorted(Comparator.comparing(ErmProjectAllocation::getProjectName, String.CASE_INSENSITIVE_ORDER))
                .toList());
        List<AttendanceAssignmentResponse> nonBillableAssignments = toAssignmentResponses(activeAllocations.stream()
                .filter(allocation -> allocation.getAllocationType() != ProjectAllocationType.BILLABLE)
                .sorted(Comparator.comparing(ErmProjectAllocation::getProjectName, String.CASE_INSENSITIVE_ORDER))
                .toList());

        List<ErmLeaveRequest> approvedLeaves = leaveRequestRepository
                .findAllByEmployeeUserIdAndRequestStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(
                        user.getId(),
                        LeaveRequestStatus.APPROVED,
                        weekEnd,
                        weekStart
                );

        Map<LocalDate, ErmLeaveRequest> leaveByDate = new LinkedHashMap<>();
        for (ErmLeaveRequest leaveRequest : approvedLeaves) {
            LocalDate cursor = leaveRequest.getStartDate().isBefore(weekStart) ? weekStart : leaveRequest.getStartDate();
            LocalDate leaveEnd = leaveRequest.getEndDate().isAfter(weekEnd) ? weekEnd : leaveRequest.getEndDate();
            while (!cursor.isAfter(leaveEnd)) {
                leaveByDate.put(cursor, leaveRequest);
                cursor = cursor.plusDays(1);
            }
        }

        ErmAttendanceTimesheet timesheet = timesheetRepository.findByEmployeeUserIdAndWeekStartDate(user.getId(), weekStart)
                .orElse(null);
        List<ErmAttendanceTimesheetDay> existingDays = timesheet == null
                ? List.of()
                : timesheetDayRepository.findAllByTimesheetIdOrderByWorkDateAsc(timesheet.getId());

        Map<LocalDate, ErmAttendanceTimesheetDay> dayByDate = existingDays.stream()
                .collect(Collectors.toMap(ErmAttendanceTimesheetDay::getWorkDate, day -> day, (left, right) -> left, LinkedHashMap::new));

        List<AttendanceDayResponse> days = new ArrayList<>();
        for (int index = 0; index < 7; index++) {
            LocalDate workDate = weekStart.plusDays(index);
            boolean weekend = workDate.getDayOfWeek() == DayOfWeek.SATURDAY || workDate.getDayOfWeek() == DayOfWeek.SUNDAY;
            ErmLeaveRequest leaveRequest = leaveByDate.get(workDate);
            ErmAttendanceTimesheetDay entry = dayByDate.get(workDate);
            boolean dayEditable = editable && !weekend && leaveRequest == null;

            days.add(new AttendanceDayResponse(
                    workDate,
                    workDate.getDayOfWeek().name().substring(0, 3),
                    weekend,
                    leaveRequest != null,
                    leaveRequest == null ? null : leaveRequest.getLeaveCategory().getLabel(),
                    entry == null ? BigDecimal.ZERO : scaleHours(entry.getBillableHours()),
                    entry == null ? BigDecimal.ZERO : scaleHours(entry.getNonBillableHours()),
                    entry == null ? null : entry.getBillableProjectAllocationId(),
                    entry == null ? null : entry.getBillableProjectName(),
                    entry == null ? null : entry.getBillableProjectCode(),
                    entry == null ? null : entry.getNonBillableProjectAllocationId(),
                    entry == null ? null : entry.getNonBillableProjectName(),
                    entry == null ? null : entry.getNonBillableProjectCode(),
                    dayEditable
            ));
        }

        AttendanceTimesheetResponse timesheetResponse = timesheet == null ? null : toTimesheetResponse(timesheet, days, false);

        List<AttendanceApprovalItemResponse> pendingApprovals = user.getId() == null
                ? List.of()
                : timesheetRepository.findAllByApproverManagerUserIdAndTimesheetStatusOrderByUpdatedAtDesc(user.getId(), AttendanceTimesheetStatus.SUBMITTED)
                .stream()
                .map(this::toApprovalItem)
                .toList();

        boolean hasBillableAssignments = !billableAssignments.isEmpty();
        boolean hasReportees = !userRepository.findAllByReportingManagerUserIdOrderByFullNameAsc(user.getId()).isEmpty();

        return new AttendanceWeekResponse(
                weekStart,
                weekEnd,
                editableUntil,
                editable,
                hasBillableAssignments,
                hasReportees,
                timesheetResponse,
                days,
                billableAssignments,
                nonBillableAssignments,
                pendingApprovals
        );
    }

    @Transactional
    public AttendanceWeekResponse save(String username, AttendanceTimesheetUpsertRequest request) {
        return upsert(username, request, false);
    }

    @Transactional
    public AttendanceWeekResponse submit(String username, AttendanceTimesheetUpsertRequest request) {
        return upsert(username, request, true);
    }

    @Transactional(readOnly = true)
    public AttendanceApproverVisibilityResponse approverVisibility(String username) {
        ErmUser user = resolveUser(username);
        return new AttendanceApproverVisibilityResponse(!userRepository.findAllByReportingManagerUserIdOrderByFullNameAsc(user.getId()).isEmpty());
    }

    @Transactional(readOnly = true)
    public List<AttendanceApprovalItemResponse> approvals(String username) {
        ErmUser manager = resolveUser(username);
        return timesheetRepository.findAllByApproverManagerUserIdAndTimesheetStatusOrderByUpdatedAtDesc(manager.getId(), AttendanceTimesheetStatus.SUBMITTED)
                .stream()
                .map(this::toApprovalItem)
                .toList();
    }

    @Transactional
    public AttendanceWeekResponse action(Long timesheetId, AttendanceTimesheetActionRequest request, String username) {
        ErmUser manager = resolveUser(username);
        ErmAttendanceTimesheet timesheet = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Timesheet not found"));

        if (timesheet.getApproverManagerUserId() == null || !timesheet.getApproverManagerUserId().equals(manager.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to action this timesheet");
        }
        if (timesheet.getTimesheetStatus() != AttendanceTimesheetStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timesheet is not pending approval");
        }

        if (request.decision() == AttendanceDecision.APPROVE) {
            timesheet.setTimesheetStatus(AttendanceTimesheetStatus.APPROVED);
            timesheet.setApprovedAt(LocalDateTime.now());
            timesheet.setRejectedAt(null);
        } else {
            timesheet.setTimesheetStatus(AttendanceTimesheetStatus.REJECTED);
            timesheet.setRejectedAt(LocalDateTime.now());
            timesheet.setApprovedAt(null);
        }
        timesheet.setApproverComment(normalizeText(request.comment()));
        timesheet = timesheetRepository.save(timesheet);

        return getWeek(timesheet.getEmployeeUsername(), timesheet.getWeekStartDate());
    }

    private AttendanceWeekResponse upsert(String username, AttendanceTimesheetUpsertRequest request, boolean submit) {
        ErmUser user = resolveUser(username);
        LocalDate weekStart = normalizeWeekStart(request.weekStartDate());
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate editableUntil = LocalDate.now().minusDays(30);
        if (weekEnd.isBefore(editableUntil)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timesheets older than 30 days cannot be edited");
        }

        ErmUser manager = resolveReportingManagerIfAvailable(user);
        List<AttendanceTimesheetDayRequest> dayRequests = request.days().stream()
                .sorted(Comparator.comparing(AttendanceTimesheetDayRequest::workDate))
                .toList();
        validateWeeklyPayload(weekStart, weekEnd, dayRequests);

        ErmAttendanceTimesheet timesheet = timesheetRepository.findByEmployeeUserIdAndWeekStartDate(user.getId(), weekStart)
                .orElseGet(ErmAttendanceTimesheet::new);
        boolean existingApproved = timesheet.getId() != null && timesheet.getTimesheetStatus() == AttendanceTimesheetStatus.APPROVED;

        timesheet.setEmployeeUserId(user.getId());
        timesheet.setEmployeeUsername(user.getUsername());
        timesheet.setEmployeeFullName(user.getFullName());
        timesheet.setWeekStartDate(weekStart);
        timesheet.setWeekEndDate(weekEnd);
        timesheet.setApproverManagerUserId(manager == null ? null : manager.getId());
        timesheet.setApproverManagerUsername(manager == null ? null : manager.getUsername());
        timesheet.setApproverManagerFullName(manager == null ? null : manager.getFullName());

        Map<LocalDate, ErmLeaveRequest> leaveByDate = loadLeaves(user.getId(), weekStart, weekEnd);
        Map<Long, ErmProjectAllocation> allocationsById = loadAssignments(user.getId(), weekStart, weekEnd).stream()
                .collect(Collectors.toMap(ErmProjectAllocation::getId, allocation -> allocation, (left, right) -> left, LinkedHashMap::new));

        List<ErmAttendanceTimesheetDay> replacementDays = new ArrayList<>();
        BigDecimal billableTotal = BigDecimal.ZERO;

        for (AttendanceTimesheetDayRequest dayRequest : dayRequests) {
            LocalDate workDate = dayRequest.workDate();
            boolean weekend = workDate.getDayOfWeek() == DayOfWeek.SATURDAY || workDate.getDayOfWeek() == DayOfWeek.SUNDAY;
            ErmLeaveRequest leaveRequest = leaveByDate.get(workDate);

            if (weekend && hasAnyValue(dayRequest)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Weekend entries are not allowed");
            }
            if (leaveRequest != null && hasAnyValue(dayRequest)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leave days cannot be edited");
            }

            BigDecimal billableHours = scaleHours(dayRequest.billableHours());
            BigDecimal nonBillableHours = scaleHours(dayRequest.nonBillableHours());
            if (billableHours.compareTo(MAX_BILLABLE_HOURS_PER_DAY) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Billable hours cannot exceed 8 per day");
            }
            if (billableHours.add(nonBillableHours).compareTo(new BigDecimal("24.00")) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total hours cannot exceed 24 per day");
            }

            ErmProjectAllocation billableAllocation = resolveAllocation(allocationsById, dayRequest.billableProjectAllocationId(), workDate, true);
            ErmProjectAllocation nonBillableAllocation = resolveAllocation(allocationsById, dayRequest.nonBillableProjectAllocationId(), workDate, false);

            if (billableHours.compareTo(BigDecimal.ZERO) > 0 && billableAllocation == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Billable hours require an active billable project assignment");
            }
            if (nonBillableHours.compareTo(BigDecimal.ZERO) > 0 && nonBillableAllocation == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-billable hours require an active non-billable project assignment");
            }

            billableTotal = billableTotal.add(billableHours);

            ErmAttendanceTimesheetDay day = new ErmAttendanceTimesheetDay();
            day.setWorkDate(workDate);
            day.setBillableHours(billableHours);
            day.setNonBillableHours(nonBillableHours);
            if (billableAllocation != null) {
                day.setBillableProjectAllocationId(billableAllocation.getId());
                day.setBillableProjectRequestId(billableAllocation.getProjectRequestId());
                day.setBillableProjectName(billableAllocation.getProjectName());
                day.setBillableProjectCode(billableAllocation.getProjectCode());
            }
            if (nonBillableAllocation != null) {
                day.setNonBillableProjectAllocationId(nonBillableAllocation.getId());
                day.setNonBillableProjectRequestId(nonBillableAllocation.getProjectRequestId());
                day.setNonBillableProjectName(nonBillableAllocation.getProjectName());
                day.setNonBillableProjectCode(nonBillableAllocation.getProjectCode());
            }
            replacementDays.add(day);
        }

        if (submit && billableTotal.compareTo(BigDecimal.ZERO) > 0 && manager == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reporting manager is not configured for billable approval");
        }

        timesheet.setApprovalRequired(billableTotal.compareTo(BigDecimal.ZERO) > 0);
        timesheet.setApproverComment(null);
        timesheet.setApprovedAt(null);
        timesheet.setRejectedAt(null);

        if (submit) {
            timesheet.setSubmittedAt(LocalDateTime.now());
            if (billableTotal.compareTo(BigDecimal.ZERO) > 0) {
                timesheet.setTimesheetStatus(AttendanceTimesheetStatus.SUBMITTED);
            } else {
                timesheet.setTimesheetStatus(AttendanceTimesheetStatus.APPROVED);
                timesheet.setApprovedAt(LocalDateTime.now());
            }
        } else {
            timesheet.setTimesheetStatus(AttendanceTimesheetStatus.DRAFT);
            timesheet.setSubmittedAt(null);
        }

        if (existingApproved) {
            timesheet.setTimesheetStatus(submit && billableTotal.compareTo(BigDecimal.ZERO) > 0
                    ? AttendanceTimesheetStatus.SUBMITTED
                    : timesheet.getTimesheetStatus());
        }

        if (timesheet.getId() != null) {
            timesheetDayRepository.deleteAllByTimesheetId(timesheet.getId());
            timesheetDayRepository.flush();
            timesheet.getDays().clear();
        }
        replacementDays.forEach(timesheet::addDay);
        timesheet = timesheetRepository.save(timesheet);

        return getWeek(user.getUsername(), weekStart);
    }

    private void validateWeeklyPayload(LocalDate weekStart, LocalDate weekEnd, List<AttendanceTimesheetDayRequest> dayRequests) {
        if (dayRequests.size() != 7) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Weekly timesheet must include 7 days");
        }
        for (int index = 0; index < dayRequests.size(); index++) {
            AttendanceTimesheetDayRequest dayRequest = dayRequests.get(index);
            LocalDate expectedDate = weekStart.plusDays(index);
            if (!expectedDate.equals(dayRequest.workDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timesheet days must match the selected week");
            }
            if (dayRequest.workDate().isBefore(weekStart) || dayRequest.workDate().isAfter(weekEnd)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timesheet day is outside the selected week");
            }
        }
    }

    private Map<LocalDate, ErmLeaveRequest> loadLeaves(Long userId, LocalDate weekStart, LocalDate weekEnd) {
        List<ErmLeaveRequest> approvedLeaves = leaveRequestRepository.findAllByEmployeeUserIdAndRequestStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(
                userId,
                LeaveRequestStatus.APPROVED,
                weekEnd,
                weekStart
        );
        Map<LocalDate, ErmLeaveRequest> leaveByDate = new LinkedHashMap<>();
        for (ErmLeaveRequest leaveRequest : approvedLeaves) {
            LocalDate cursor = leaveRequest.getStartDate().isBefore(weekStart) ? weekStart : leaveRequest.getStartDate();
            LocalDate leaveEnd = leaveRequest.getEndDate().isAfter(weekEnd) ? weekEnd : leaveRequest.getEndDate();
            while (!cursor.isAfter(leaveEnd)) {
                leaveByDate.put(cursor, leaveRequest);
                cursor = cursor.plusDays(1);
            }
        }
        return leaveByDate;
    }

    private List<ErmProjectAllocation> loadAssignments(Long userId, LocalDate weekStart, LocalDate weekEnd) {
        return projectAllocationRepository.findAllByEmployeeUserIdAndStatusOrderByUpdatedAtDesc(userId, ProjectAllocationStatus.ACTIVE)
                .stream()
                .filter(allocation -> overlaps(allocation.getStartDate(), allocation.getEndDate(), weekStart, weekEnd))
                .toList();
    }

    private ErmProjectAllocation resolveAllocation(Map<Long, ErmProjectAllocation> allocationsById,
                                                   Long allocationId,
                                                   LocalDate workDate,
                                                   boolean billable) {
        if (allocationId == null) {
            return null;
        }
        ErmProjectAllocation allocation = allocationsById.get(allocationId);
        if (allocation == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected project assignment is not active for this week");
        }
        if (allocation.getStatus() != ProjectAllocationStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected project assignment is not active");
        }
        if (allocation.getStartDate().isAfter(workDate) || allocation.getEndDate().isBefore(workDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected project assignment does not cover this date");
        }
        if (billable && allocation.getAllocationType() != ProjectAllocationType.BILLABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Billable hours require a billable project assignment");
        }
        if (!billable && allocation.getAllocationType() == ProjectAllocationType.BILLABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-billable hours require a non-billable project assignment");
        }
        return allocation;
    }

    private AttendanceTimesheetResponse toTimesheetResponse(ErmAttendanceTimesheet timesheet, List<AttendanceDayResponse> days, boolean approvalReset) {
        return new AttendanceTimesheetResponse(
                timesheet.getId(),
                timesheet.getEmployeeUserId(),
                timesheet.getEmployeeUsername(),
                timesheet.getEmployeeFullName(),
                timesheet.getWeekStartDate(),
                timesheet.getWeekEndDate(),
                timesheet.getApproverManagerUserId(),
                timesheet.getApproverManagerUsername(),
                timesheet.getApproverManagerFullName(),
                timesheet.getTimesheetStatus().name(),
                timesheet.isApprovalRequired(),
                timesheet.getSubmittedAt(),
                timesheet.getApprovedAt(),
                timesheet.getRejectedAt(),
                timesheet.getApproverComment(),
                approvalReset,
                days
        );
    }

    private AttendanceApprovalItemResponse toApprovalItem(ErmAttendanceTimesheet timesheet) {
        BigDecimal billableHours = timesheet.getDays().stream()
                .map(ErmAttendanceTimesheetDay::getBillableHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal nonBillableHours = timesheet.getDays().stream()
                .map(ErmAttendanceTimesheetDay::getNonBillableHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AttendanceApprovalItemResponse(
                timesheet.getId(),
                timesheet.getEmployeeUserId(),
                timesheet.getEmployeeUsername(),
                timesheet.getEmployeeFullName(),
                timesheet.getWeekStartDate(),
                timesheet.getWeekEndDate(),
                scaleHours(billableHours),
                scaleHours(nonBillableHours),
                timesheet.getTimesheetStatus().name(),
                timesheet.getSubmittedAt(),
                timesheet.getUpdatedAt()
        );
    }

    private List<AttendanceAssignmentResponse> toAssignmentResponses(List<ErmProjectAllocation> allocations) {
        return allocations.stream()
                .map(allocation -> new AttendanceAssignmentResponse(
                        allocation.getId(),
                        allocation.getProjectRequestId(),
                        allocation.getProjectName(),
                        allocation.getProjectCode(),
                        allocation.getAllocationType().name(),
                        allocation.getAllocationPercent(),
                        allocation.getStartDate(),
                        allocation.getEndDate()
                ))
                .toList();
    }

    private ErmUser resolveUser(String username) {
        if (!StringUtils.hasText(username)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private ErmUser resolveReportingManagerIfAvailable(ErmUser user) {
        if (user.getReportingManagerUserId() == null) {
            return null;
        }
        return userRepository.findById(user.getReportingManagerUserId()).orElse(null);
    }

    private boolean overlaps(LocalDate start, LocalDate end, LocalDate weekStart, LocalDate weekEnd) {
        return !start.isAfter(weekEnd) && !end.isBefore(weekStart);
    }

    private LocalDate normalizeWeekStart(LocalDate date) {
        LocalDate source = date == null ? LocalDate.now() : date;
        return source.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private boolean hasAnyValue(AttendanceTimesheetDayRequest request) {
        return scaleHours(request.billableHours()).compareTo(BigDecimal.ZERO) > 0
                || scaleHours(request.nonBillableHours()).compareTo(BigDecimal.ZERO) > 0
                || request.billableProjectAllocationId() != null
                || request.nonBillableProjectAllocationId() != null;
    }

    private BigDecimal scaleHours(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment is required");
        }
        return value.trim();
    }
}
