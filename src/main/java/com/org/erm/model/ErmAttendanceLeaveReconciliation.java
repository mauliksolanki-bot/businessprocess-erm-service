package com.org.erm.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ERM_ATTENDANCE_LEAVE_RECONCILIATIONS")
public class ErmAttendanceLeaveReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "LEAVE_REQUEST_ID", nullable = false)
    private Long leaveRequestId;

    @Column(name = "TIMESHEET_ID", nullable = false)
    private Long timesheetId;

    @Column(name = "EMPLOYEE_USER_ID", nullable = false)
    private Long employeeUserId;

    @Column(name = "WEEK_START_DATE", nullable = false)
    private LocalDate weekStartDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "PREVIOUS_TIMESHEET_STATUS", nullable = false, length = 30)
    private AttendanceTimesheetStatus previousTimesheetStatus;

    @Column(name = "PREVIOUS_APPROVAL_REQUIRED", nullable = false)
    private boolean previousApprovalRequired;

    @Column(name = "PREVIOUS_SUBMITTED_AT")
    private LocalDateTime previousSubmittedAt;

    @Column(name = "PREVIOUS_APPROVED_AT")
    private LocalDateTime previousApprovedAt;

    @Column(name = "PREVIOUS_REJECTED_AT")
    private LocalDateTime previousRejectedAt;

    @Column(name = "PREVIOUS_APPROVER_COMMENT", length = 500)
    private String previousApproverComment;

    @Column(name = "RESTORED_AT")
    private LocalDateTime restoredAt;

    @OneToMany(mappedBy = "reconciliation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ErmAttendanceLeaveReconciliationDay> days = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public Long getLeaveRequestId() {
        return leaveRequestId;
    }

    public void setLeaveRequestId(Long leaveRequestId) {
        this.leaveRequestId = leaveRequestId;
    }

    public Long getTimesheetId() {
        return timesheetId;
    }

    public void setTimesheetId(Long timesheetId) {
        this.timesheetId = timesheetId;
    }

    public Long getEmployeeUserId() {
        return employeeUserId;
    }

    public void setEmployeeUserId(Long employeeUserId) {
        this.employeeUserId = employeeUserId;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public AttendanceTimesheetStatus getPreviousTimesheetStatus() {
        return previousTimesheetStatus;
    }

    public void setPreviousTimesheetStatus(AttendanceTimesheetStatus previousTimesheetStatus) {
        this.previousTimesheetStatus = previousTimesheetStatus;
    }

    public boolean isPreviousApprovalRequired() {
        return previousApprovalRequired;
    }

    public void setPreviousApprovalRequired(boolean previousApprovalRequired) {
        this.previousApprovalRequired = previousApprovalRequired;
    }

    public LocalDateTime getPreviousSubmittedAt() {
        return previousSubmittedAt;
    }

    public void setPreviousSubmittedAt(LocalDateTime previousSubmittedAt) {
        this.previousSubmittedAt = previousSubmittedAt;
    }

    public LocalDateTime getPreviousApprovedAt() {
        return previousApprovedAt;
    }

    public void setPreviousApprovedAt(LocalDateTime previousApprovedAt) {
        this.previousApprovedAt = previousApprovedAt;
    }

    public LocalDateTime getPreviousRejectedAt() {
        return previousRejectedAt;
    }

    public void setPreviousRejectedAt(LocalDateTime previousRejectedAt) {
        this.previousRejectedAt = previousRejectedAt;
    }

    public String getPreviousApproverComment() {
        return previousApproverComment;
    }

    public void setPreviousApproverComment(String previousApproverComment) {
        this.previousApproverComment = previousApproverComment;
    }

    public LocalDateTime getRestoredAt() {
        return restoredAt;
    }

    public void setRestoredAt(LocalDateTime restoredAt) {
        this.restoredAt = restoredAt;
    }

    public List<ErmAttendanceLeaveReconciliationDay> getDays() {
        return days;
    }

    public void addDay(ErmAttendanceLeaveReconciliationDay day) {
        this.days.add(day);
        day.setReconciliation(this);
    }
}
