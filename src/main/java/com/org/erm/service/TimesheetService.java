package com.org.erm.service;

import com.org.erm.dto.request.TimesheetActionRequest;
import com.org.erm.dto.request.TimesheetEntryRequest;
import com.org.erm.dto.request.TimesheetSubmitRequest;
import com.org.erm.dto.response.TimesheetApproverVisibilityResponse;
import com.org.erm.dto.response.TimesheetEntryResponse;
import com.org.erm.dto.response.TimesheetResponse;
import com.org.erm.model.ErmProjectAllocation;
import com.org.erm.model.ErmTimesheet;
import com.org.erm.model.ErmTimesheetEntry;
import com.org.erm.model.ErmUser;
import com.org.erm.model.ProjectAllocationStatus;
import com.org.erm.model.ProjectAllocationType;
import com.org.erm.model.TimesheetActionDecision;
import com.org.erm.model.TimesheetStatus;
import com.org.erm.model.TimesheetWorkType;
import com.org.erm.repository.ErmProjectAllocationRepository;
import com.org.erm.repository.ErmTimesheetEntryRepository;
import com.org.erm.repository.ErmTimesheetRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class TimesheetService {

    private final ErmTimesheetRepository timesheetRepository;
    private final ErmTimesheetEntryRepository timesheetEntryRepository;
    private final ErmUserRepository userRepository;
    private final ErmProjectAllocationRepository projectAllocationRepository;

    public TimesheetService(ErmTimesheetRepository timesheetRepository,
                            ErmTimesheetEntryRepository timesheetEntryRepository,
                            ErmUserRepository userRepository,
                            ErmProjectAllocationRepository projectAllocationRepository) {
        this.timesheetRepository = timesheetRepository;
        this.timesheetEntryRepository = timesheetEntryRepository;
        this.userRepository = userRepository;
        this.projectAllocationRepository = projectAllocationRepository;
    }

    @Transactional
    public TimesheetResponse submit(TimesheetSubmitRequest request, String username) {
        ErmUser user = resolveUser(username);
        if (request.rows() == null || request.rows().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one timesheet row is required");
        }
        LocalDate weekStart = request.weekStartDate();
        validateMonday(weekStart);
        LocalDate weekEnd = weekStart.plusDays(6);
        ErmUser manager = resolveManagerIfRequired(user, request.rows());
        String submissionComment = normalizeOptional(request.submissionComment());

        List<ErmTimesheetEntry> entries = new ArrayList<>();
        Map<DayOfWeek, BigDecimal> dailyTotals = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            dailyTotals.put(dayOfWeek, BigDecimal.ZERO);
        }

        boolean hasBillableRows = false;
        int sortOrder = 1;
        BigDecimal totalHours = BigDecimal.ZERO;
        BigDecimal billableHours = BigDecimal.ZERO;
        BigDecimal nonBillableHours = BigDecimal.ZERO;

        for (TimesheetEntryRequest row : request.rows()) {
            TimesheetWorkType workType = row.workType();
            if (workType == TimesheetWorkType.BILLABLE) {
                hasBillableRows = true;
            }

            ErmProjectAllocation allocation = null;
            if (workType == TimesheetWorkType.BILLABLE) {
                if (row.projectAllocationId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Billable rows require a project allocation");
                }
                allocation = projectAllocationRepository.findById(row.projectAllocationId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected project allocation was not found"));
                if (!allocation.getEmployeeUserId().equals(user.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Billable rows must use one of your active allocations");
                }
                if (allocation.getStatus() != ProjectAllocationStatus.ACTIVE) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected allocation is not active");
                }
                if (allocation.getAllocationType() != ProjectAllocationType.BILLABLE) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected allocation is not billable");
                }
            } else if (row.projectAllocationId() != null) {
                allocation = projectAllocationRepository.findById(row.projectAllocationId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected project allocation was not found"));
                if (!allocation.getEmployeeUserId().equals(user.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected allocation does not belong to you");
                }
                if (allocation.getStatus() != ProjectAllocationStatus.ACTIVE) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected allocation is not active");
                }
                if (allocation.getAllocationType() == ProjectAllocationType.BILLABLE) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-billable rows must not use billable allocations");
                }
            }

            BigDecimal monday = normalizeHours(row.mondayHours(), "Monday");
            BigDecimal tuesday = normalizeHours(row.tuesdayHours(), "Tuesday");
            BigDecimal wednesday = normalizeHours(row.wednesdayHours(), "Wednesday");
            BigDecimal thursday = normalizeHours(row.thursdayHours(), "Thursday");
            BigDecimal friday = normalizeHours(row.fridayHours(), "Friday");
            BigDecimal saturday = normalizeHours(row.saturdayHours(), "Saturday");
            BigDecimal sunday = normalizeHours(row.sundayHours(), "Sunday");

            BigDecimal rowTotal = monday.add(tuesday).add(wednesday).add(thursday).add(friday).add(saturday).add(sunday);
            if (rowTotal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each timesheet row must contain at least one hour");
            }

            dailyTotals.put(DayOfWeek.MONDAY, dailyTotals.get(DayOfWeek.MONDAY).add(monday));
            dailyTotals.put(DayOfWeek.TUESDAY, dailyTotals.get(DayOfWeek.TUESDAY).add(tuesday));
            dailyTotals.put(DayOfWeek.WEDNESDAY, dailyTotals.get(DayOfWeek.WEDNESDAY).add(wednesday));
            dailyTotals.put(DayOfWeek.THURSDAY, dailyTotals.get(DayOfWeek.THURSDAY).add(thursday));
            dailyTotals.put(DayOfWeek.FRIDAY, dailyTotals.get(DayOfWeek.FRIDAY).add(friday));
            dailyTotals.put(DayOfWeek.SATURDAY, dailyTotals.get(DayOfWeek.SATURDAY).add(saturday));
            dailyTotals.put(DayOfWeek.SUNDAY, dailyTotals.get(DayOfWeek.SUNDAY).add(sunday));

            validateDailyCap(dailyTotals);

            totalHours = totalHours.add(rowTotal);
            if (workType == TimesheetWorkType.BILLABLE) {
                billableHours = billableHours.add(rowTotal);
            } else {
                nonBillableHours = nonBillableHours.add(rowTotal);
            }

            ErmTimesheetEntry entry = new ErmTimesheetEntry();
            entry.setSortOrder(sortOrder++);
            entry.setWorkType(workType);
            entry.setProjectAllocationId(allocation == null ? null : allocation.getId());
            entry.setProjectName(allocation == null ? null : allocation.getProjectName());
            entry.setProjectCode(allocation == null ? null : allocation.getProjectCode());
            entry.setTaskName(normalizeRequired(row.taskName(), "Task name is required"));
            entry.setMondayHours(monday);
            entry.setTuesdayHours(tuesday);
            entry.setWednesdayHours(wednesday);
            entry.setThursdayHours(thursday);
            entry.setFridayHours(friday);
            entry.setSaturdayHours(saturday);
            entry.setSundayHours(sunday);
            entry.setTotalHours(rowTotal);
            entries.add(entry);
        }

        if (hasBillableRows && manager == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reporting manager is not configured for billable timesheets");
        }

        ErmTimesheet timesheet = new ErmTimesheet();
        timesheet.setTimesheetCode(buildTimesheetCode(user.getId(), weekStart));
        timesheet.setEmployeeUserId(user.getId());
        timesheet.setEmployeeUsername(user.getUsername());
        timesheet.setEmployeeFullName(resolveDisplayName(user));
        timesheet.setReportingManagerUserId(manager == null ? null : manager.getId());
        timesheet.setReportingManagerUsername(manager == null ? null : manager.getUsername());
        timesheet.setReportingManagerFullName(manager == null ? null : resolveDisplayName(manager));
        timesheet.setWeekStartDate(weekStart);
        timesheet.setWeekEndDate(weekEnd);
        timesheet.setSubmissionComment(submissionComment);
        timesheet.setTotalHours(scale(totalHours));
        timesheet.setBillableHours(scale(billableHours));
        timesheet.setNonBillableHours(scale(nonBillableHours));

        if (hasBillableRows) {
            timesheet.setStatus(TimesheetStatus.PENDING_MANAGER_APPROVAL);
        } else {
            timesheet.setStatus(TimesheetStatus.APPROVED);
            timesheet.setManagerActionByUsername("system");
            timesheet.setManagerActionAt(LocalDateTime.now());
            timesheet.setManagerComment("Auto-approved (non-billable)");
        }

        timesheet = timesheetRepository.save(timesheet);
        for (ErmTimesheetEntry entry : entries) {
            entry.setTimesheetId(timesheet.getId());
        }
        timesheetEntryRepository.saveAll(entries);

        return toResponse(timesheet, entries.stream().map(this::toEntryResponse).toList());
    }

    @Transactional(readOnly = true)
    public List<TimesheetResponse> myTimesheets(String username) {
        ErmUser user = resolveUser(username);
        return timesheetRepository.findAllByEmployeeUserIdOrderByWeekStartDateDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TimesheetResponse> approvals(String username) {
        ErmUser manager = resolveUser(username);
        return timesheetRepository.findAllByReportingManagerUserIdAndStatusOrderByWeekStartDateDesc(
                        manager.getId(),
                        TimesheetStatus.PENDING_MANAGER_APPROVAL
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TimesheetApproverVisibilityResponse approverVisibility(String username) {
        ErmUser manager = resolveUser(username);
        return new TimesheetApproverVisibilityResponse(!userRepository.findAllByReportingManagerUserIdOrderByFullNameAsc(manager.getId()).isEmpty());
    }

    @Transactional(readOnly = true)
    public TimesheetResponse getById(Long id, String username) {
        ErmUser actor = resolveUser(username);
        ErmTimesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Timesheet not found"));
        ensureVisible(actor, timesheet);
        return toResponse(timesheet);
    }

    @Transactional
    public TimesheetResponse action(Long id, TimesheetActionRequest request, String username) {
        ErmUser manager = resolveUser(username);
        ErmTimesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Timesheet not found"));
        if (timesheet.getReportingManagerUserId() == null || !timesheet.getReportingManagerUserId().equals(manager.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to action this timesheet");
        }
        if (timesheet.getStatus() != TimesheetStatus.PENDING_MANAGER_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timesheet is already closed");
        }

        timesheet.setStatus(request.decision() == TimesheetActionDecision.APPROVE ? TimesheetStatus.APPROVED : TimesheetStatus.REJECTED);
        timesheet.setManagerActionByUsername(manager.getUsername());
        timesheet.setManagerActionAt(LocalDateTime.now());
        timesheet.setManagerComment(normalizeRequired(request.comment(), "Comment is required"));
        timesheet = timesheetRepository.save(timesheet);
        return toResponse(timesheet);
    }

    private ErmUser resolveUser(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private ErmUser resolveManagerIfRequired(ErmUser user, List<TimesheetEntryRequest> rows) {
        boolean hasBillableRows = rows.stream().anyMatch(row -> row.workType() == TimesheetWorkType.BILLABLE);
        if (!hasBillableRows) {
            return null;
        }
        if (user.getReportingManagerUserId() == null) {
            return null;
        }
        return userRepository.findById(user.getReportingManagerUserId()).orElse(null);
    }

    private void validateMonday(LocalDate weekStart) {
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Week start date must be a Monday");
        }
    }

    private void validateDailyCap(Map<DayOfWeek, BigDecimal> totals) {
        for (Map.Entry<DayOfWeek, BigDecimal> entry : totals.entrySet()) {
            if (entry.getKey() == DayOfWeek.SATURDAY || entry.getKey() == DayOfWeek.SUNDAY) {
                continue;
            }
            if (entry.getValue().compareTo(new BigDecimal("8.00")) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Daily hours cannot exceed 8 for " + entry.getKey().name().toLowerCase());
            }
        }
    }

    private BigDecimal normalizeHours(BigDecimal value, String dayName) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, dayName + " hours are required");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, dayName + " hours cannot be negative");
        }
        return scale(value);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveDisplayName(ErmUser user) {
        return StringUtils.hasText(user.getFullName()) ? user.getFullName().trim() : user.getUsername();
    }

    private String buildTimesheetCode(Long userId, LocalDate weekStart) {
        return "TS-" + userId + "-" + weekStart.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private void ensureVisible(ErmUser actor, ErmTimesheet timesheet) {
        boolean isOwner = timesheet.getEmployeeUserId().equals(actor.getId());
        boolean isApprover = timesheet.getReportingManagerUserId() != null && timesheet.getReportingManagerUserId().equals(actor.getId());
        if (!isOwner && !isApprover) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this timesheet");
        }
    }

    private TimesheetResponse toResponse(ErmTimesheet timesheet) {
        List<TimesheetEntryResponse> rows = timesheetEntryRepository.findAllByTimesheetIdOrderBySortOrderAsc(timesheet.getId()).stream()
                .map(this::toEntryResponse)
                .toList();
        return toResponse(timesheet, rows);
    }

    private TimesheetResponse toResponse(ErmTimesheet timesheet, List<TimesheetEntryResponse> rows) {
        return new TimesheetResponse(
                timesheet.getId(),
                timesheet.getTimesheetCode(),
                timesheet.getEmployeeUserId(),
                timesheet.getEmployeeUsername(),
                timesheet.getEmployeeFullName(),
                timesheet.getReportingManagerUserId(),
                timesheet.getReportingManagerUsername(),
                timesheet.getReportingManagerFullName(),
                timesheet.getWeekStartDate(),
                timesheet.getWeekEndDate(),
                timesheet.getStatus(),
                timesheet.getSubmissionComment(),
                timesheet.getManagerActionByUsername(),
                timesheet.getManagerActionAt(),
                timesheet.getManagerComment(),
                timesheet.getTotalHours(),
                timesheet.getBillableHours(),
                timesheet.getNonBillableHours(),
                timesheet.getCreatedAt(),
                timesheet.getUpdatedAt(),
                rows
        );
    }

    private TimesheetEntryResponse toEntryResponse(ErmTimesheetEntry entry) {
        return new TimesheetEntryResponse(
                entry.getId(),
                entry.getSortOrder(),
                entry.getWorkType(),
                entry.getProjectAllocationId(),
                entry.getProjectName(),
                entry.getProjectCode(),
                entry.getTaskName(),
                entry.getMondayHours(),
                entry.getTuesdayHours(),
                entry.getWednesdayHours(),
                entry.getThursdayHours(),
                entry.getFridayHours(),
                entry.getSaturdayHours(),
                entry.getSundayHours(),
                entry.getTotalHours()
        );
    }
}
