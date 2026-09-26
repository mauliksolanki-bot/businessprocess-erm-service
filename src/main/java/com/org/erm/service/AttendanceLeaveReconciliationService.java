package com.org.erm.service;

import com.org.erm.model.AttendanceTimesheetStatus;
import com.org.erm.model.ErmAttendanceLeaveReconciliation;
import com.org.erm.model.ErmAttendanceLeaveReconciliationDay;
import com.org.erm.model.ErmAttendanceTimesheet;
import com.org.erm.model.ErmAttendanceTimesheetDay;
import com.org.erm.model.ErmLeaveRequest;
import com.org.erm.repository.ErmAttendanceLeaveReconciliationRepository;
import com.org.erm.repository.ErmAttendanceTimesheetDayRepository;
import com.org.erm.repository.ErmAttendanceTimesheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class AttendanceLeaveReconciliationService {

    private static final DateTimeFormatter WEEK_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ErmAttendanceTimesheetRepository timesheetRepository;
    private final ErmAttendanceTimesheetDayRepository timesheetDayRepository;
    private final ErmAttendanceLeaveReconciliationRepository reconciliationRepository;
    private final MentionNotificationService mentionNotificationService;

    public AttendanceLeaveReconciliationService(ErmAttendanceTimesheetRepository timesheetRepository,
                                                ErmAttendanceTimesheetDayRepository timesheetDayRepository,
                                                ErmAttendanceLeaveReconciliationRepository reconciliationRepository,
                                                MentionNotificationService mentionNotificationService) {
        this.timesheetRepository = timesheetRepository;
        this.timesheetDayRepository = timesheetDayRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.mentionNotificationService = mentionNotificationService;
    }

    @Transactional
    public void handleLeaveApplied(ErmLeaveRequest leaveRequest) {
        for (LocalDate weekStart : weekStartsBetween(leaveRequest.getStartDate(), leaveRequest.getEndDate())) {
            Optional<ErmAttendanceTimesheet> timesheetOptional = timesheetRepository.findByEmployeeUserIdAndWeekStartDate(
                    leaveRequest.getEmployeeUserId(),
                    weekStart
            );
            if (timesheetOptional.isEmpty()) {
                continue;
            }

            ErmAttendanceTimesheet timesheet = timesheetOptional.get();
            if (reconciliationRepository.findByLeaveRequestIdAndTimesheetId(leaveRequest.getId(), timesheet.getId()).isPresent()) {
                continue;
            }

            List<ErmAttendanceTimesheetDay> timesheetDays = timesheetDayRepository.findAllByTimesheetIdOrderByWorkDateAsc(timesheet.getId());
            List<ErmAttendanceTimesheetDay> affectedDays = timesheetDays.stream()
                    .filter(day -> overlaps(day.getWorkDate(), leaveRequest.getStartDate(), leaveRequest.getEndDate()))
                    .filter(this::hasStoredValue)
                    .toList();
            if (affectedDays.isEmpty()) {
                continue;
            }

            ErmAttendanceLeaveReconciliation reconciliation = createSnapshot(leaveRequest, timesheet, affectedDays);
            reconciliationRepository.save(reconciliation);

            affectedDays.forEach(this::clearDayValues);
            reopenTimesheetAsDraft(timesheet, timesheetDays);
            timesheetRepository.save(timesheet);

            notifyReset(leaveRequest, timesheet);
        }
    }

    @Transactional
    public void handleLeaveRejected(ErmLeaveRequest leaveRequest) {
        List<ErmAttendanceLeaveReconciliation> reconciliations = reconciliationRepository
                .findAllByLeaveRequestIdAndRestoredAtIsNullOrderByWeekStartDateAsc(leaveRequest.getId());
        for (ErmAttendanceLeaveReconciliation reconciliation : reconciliations) {
            Optional<ErmAttendanceTimesheet> timesheetOptional = timesheetRepository.findById(reconciliation.getTimesheetId());
            if (timesheetOptional.isEmpty()) {
                reconciliation.setRestoredAt(LocalDateTime.now());
                continue;
            }

            ErmAttendanceTimesheet timesheet = timesheetOptional.get();
            List<ErmAttendanceTimesheetDay> timesheetDays = timesheetDayRepository.findAllByTimesheetIdOrderByWorkDateAsc(timesheet.getId());
            Map<LocalDate, ErmAttendanceTimesheetDay> dayByDate = new LinkedHashMap<>();
            for (ErmAttendanceTimesheetDay day : timesheetDays) {
                dayByDate.put(day.getWorkDate(), day);
            }

            for (ErmAttendanceLeaveReconciliationDay snapshotDay : reconciliation.getDays()) {
                ErmAttendanceTimesheetDay day = dayByDate.get(snapshotDay.getWorkDate());
                if (day == null) {
                    day = new ErmAttendanceTimesheetDay();
                    day.setWorkDate(snapshotDay.getWorkDate());
                    timesheet.addDay(day);
                    dayByDate.put(snapshotDay.getWorkDate(), day);
                }
                restoreDayValues(day, snapshotDay);
            }

            timesheet.setTimesheetStatus(reconciliation.getPreviousTimesheetStatus());
            timesheet.setApprovalRequired(reconciliation.isPreviousApprovalRequired());
            timesheet.setSubmittedAt(reconciliation.getPreviousSubmittedAt());
            timesheet.setApprovedAt(reconciliation.getPreviousApprovedAt());
            timesheet.setRejectedAt(reconciliation.getPreviousRejectedAt());
            timesheet.setApproverComment(reconciliation.getPreviousApproverComment());
            timesheetRepository.save(timesheet);

            reconciliation.setRestoredAt(LocalDateTime.now());
            notifyRestore(leaveRequest, timesheet, reconciliation.getPreviousTimesheetStatus());
        }
    }

    private ErmAttendanceLeaveReconciliation createSnapshot(ErmLeaveRequest leaveRequest,
                                                            ErmAttendanceTimesheet timesheet,
                                                            List<ErmAttendanceTimesheetDay> affectedDays) {
        ErmAttendanceLeaveReconciliation reconciliation = new ErmAttendanceLeaveReconciliation();
        reconciliation.setLeaveRequestId(leaveRequest.getId());
        reconciliation.setTimesheetId(timesheet.getId());
        reconciliation.setEmployeeUserId(timesheet.getEmployeeUserId());
        reconciliation.setEmployeeId(timesheet.getEmployeeId());
        reconciliation.setWeekStartDate(timesheet.getWeekStartDate());
        reconciliation.setPreviousTimesheetStatus(timesheet.getTimesheetStatus());
        reconciliation.setPreviousApprovalRequired(timesheet.isApprovalRequired());
        reconciliation.setPreviousSubmittedAt(timesheet.getSubmittedAt());
        reconciliation.setPreviousApprovedAt(timesheet.getApprovedAt());
        reconciliation.setPreviousRejectedAt(timesheet.getRejectedAt());
        reconciliation.setPreviousApproverComment(timesheet.getApproverComment());

        for (ErmAttendanceTimesheetDay affectedDay : affectedDays) {
            ErmAttendanceLeaveReconciliationDay snapshotDay = new ErmAttendanceLeaveReconciliationDay();
            snapshotDay.setWorkDate(affectedDay.getWorkDate());
            snapshotDay.setBillableHours(affectedDay.getBillableHours());
            snapshotDay.setNonBillableHours(affectedDay.getNonBillableHours());
            snapshotDay.setBillableProjectAllocationId(affectedDay.getBillableProjectAllocationId());
            snapshotDay.setBillableProjectRequestId(affectedDay.getBillableProjectRequestId());
            snapshotDay.setBillableProjectName(affectedDay.getBillableProjectName());
            snapshotDay.setBillableProjectCode(affectedDay.getBillableProjectCode());
            snapshotDay.setNonBillableProjectAllocationId(affectedDay.getNonBillableProjectAllocationId());
            snapshotDay.setNonBillableProjectRequestId(affectedDay.getNonBillableProjectRequestId());
            snapshotDay.setNonBillableProjectName(affectedDay.getNonBillableProjectName());
            snapshotDay.setNonBillableProjectCode(affectedDay.getNonBillableProjectCode());
            reconciliation.addDay(snapshotDay);
        }
        return reconciliation;
    }

    private void reopenTimesheetAsDraft(ErmAttendanceTimesheet timesheet, List<ErmAttendanceTimesheetDay> timesheetDays) {
        timesheet.setTimesheetStatus(AttendanceTimesheetStatus.DRAFT);
        timesheet.setSubmittedAt(null);
        timesheet.setApprovedAt(null);
        timesheet.setRejectedAt(null);
        timesheet.setApproverComment(null);
        timesheet.setApprovalRequired(timesheetDays.stream()
                .map(ErmAttendanceTimesheetDay::getBillableHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .compareTo(BigDecimal.ZERO) > 0);
    }

    private void clearDayValues(ErmAttendanceTimesheetDay day) {
        day.setBillableHours(BigDecimal.ZERO);
        day.setNonBillableHours(BigDecimal.ZERO);
        day.setBillableProjectAllocationId(null);
        day.setBillableProjectRequestId(null);
        day.setBillableProjectName(null);
        day.setBillableProjectCode(null);
        day.setNonBillableProjectAllocationId(null);
        day.setNonBillableProjectRequestId(null);
        day.setNonBillableProjectName(null);
        day.setNonBillableProjectCode(null);
    }

    private void restoreDayValues(ErmAttendanceTimesheetDay day, ErmAttendanceLeaveReconciliationDay snapshotDay) {
        day.setBillableHours(snapshotDay.getBillableHours());
        day.setNonBillableHours(snapshotDay.getNonBillableHours());
        day.setBillableProjectAllocationId(snapshotDay.getBillableProjectAllocationId());
        day.setBillableProjectRequestId(snapshotDay.getBillableProjectRequestId());
        day.setBillableProjectName(snapshotDay.getBillableProjectName());
        day.setBillableProjectCode(snapshotDay.getBillableProjectCode());
        day.setNonBillableProjectAllocationId(snapshotDay.getNonBillableProjectAllocationId());
        day.setNonBillableProjectRequestId(snapshotDay.getNonBillableProjectRequestId());
        day.setNonBillableProjectName(snapshotDay.getNonBillableProjectName());
        day.setNonBillableProjectCode(snapshotDay.getNonBillableProjectCode());
    }

    private boolean hasStoredValue(ErmAttendanceTimesheetDay day) {
        return day.getBillableHours().compareTo(BigDecimal.ZERO) > 0
                || day.getNonBillableHours().compareTo(BigDecimal.ZERO) > 0
                || day.getBillableProjectAllocationId() != null
                || day.getNonBillableProjectAllocationId() != null;
    }

    private boolean overlaps(LocalDate workDate, LocalDate leaveStart, LocalDate leaveEnd) {
        return !workDate.isBefore(leaveStart) && !workDate.isAfter(leaveEnd);
    }

    private List<LocalDate> weekStartsBetween(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> weekStarts = new java.util.ArrayList<>();
        LocalDate cursor = normalizeWeekStart(startDate);
        LocalDate finalWeek = normalizeWeekStart(endDate);
        while (!cursor.isAfter(finalWeek)) {
            weekStarts.add(cursor);
            cursor = cursor.plusWeeks(1);
        }
        return weekStarts;
    }

    private LocalDate normalizeWeekStart(LocalDate date) {
        LocalDate cursor = date;
        while (cursor.getDayOfWeek() != DayOfWeek.MONDAY) {
            cursor = cursor.minusDays(1);
        }
        return cursor;
    }

    private void notifyReset(ErmLeaveRequest leaveRequest, ErmAttendanceTimesheet timesheet) {
        Set<String> recipients = notificationRecipients(leaveRequest);
        if (recipients.isEmpty()) {
            return;
        }
        mentionNotificationService.notifyUsers(
                recipients,
                leaveRequest.getEmployeeUsername(),
                "LEAVE_REQUEST",
                leaveRequest.getId(),
                "Leave request #" + leaveRequest.getId() + " reset the timesheet for "
                        + formatWeekRange(timesheet.getWeekStartDate(), timesheet.getWeekEndDate()) + " to Draft.",
                "/attendance"
        );
    }

    private void notifyRestore(ErmLeaveRequest leaveRequest,
                               ErmAttendanceTimesheet timesheet,
                               AttendanceTimesheetStatus restoredStatus) {
        Set<String> recipients = notificationRecipients(leaveRequest);
        if (recipients.isEmpty()) {
            return;
        }
        String actorUsername = StringUtils.hasText(leaveRequest.getApproverManagerUsername())
                ? leaveRequest.getApproverManagerUsername()
                : leaveRequest.getEmployeeUsername();
        mentionNotificationService.notifyUsers(
                recipients,
                actorUsername,
                "LEAVE_REQUEST",
                leaveRequest.getId(),
                "Leave request #" + leaveRequest.getId() + " restored the timesheet for "
                        + formatWeekRange(timesheet.getWeekStartDate(), timesheet.getWeekEndDate())
                        + " and returned it to " + restoredStatus.getLabel() + ".",
                "/attendance"
        );
    }

    private Set<String> notificationRecipients(ErmLeaveRequest leaveRequest) {
        Set<String> recipients = new LinkedHashSet<>();
        if (StringUtils.hasText(leaveRequest.getEmployeeUsername())) {
            recipients.add(leaveRequest.getEmployeeUsername().trim());
        }
        if (StringUtils.hasText(leaveRequest.getApproverManagerUsername())) {
            recipients.add(leaveRequest.getApproverManagerUsername().trim());
        }
        return recipients;
    }

    private String formatWeekRange(LocalDate weekStart, LocalDate weekEnd) {
        return WEEK_DATE_FORMATTER.format(weekStart) + " - " + WEEK_DATE_FORMATTER.format(weekEnd);
    }
}
