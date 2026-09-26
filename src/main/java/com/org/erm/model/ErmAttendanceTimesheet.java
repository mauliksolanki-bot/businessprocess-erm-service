package com.org.erm.model;

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
@Table(name = "ERM_ATTENDANCE_TIMESHEETS")
public class ErmAttendanceTimesheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "EMPLOYEE_USER_ID", nullable = false)
    private Long employeeUserId;

    @Column(name = "EMPLOYEE_ID", nullable = false, length = 50)
    private String employeeId;

    @Column(name = "EMPLOYEE_USERNAME", nullable = false, length = 100)
    private String employeeUsername;

    @Column(name = "EMPLOYEE_FULL_NAME", nullable = false, length = 150)
    private String employeeFullName;

    @Column(name = "WEEK_START_DATE", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "WEEK_END_DATE", nullable = false)
    private LocalDate weekEndDate;

    @Column(name = "APPROVER_MANAGER_USER_ID")
    private Long approverManagerUserId;

    @Column(name = "APPROVER_MANAGER_EMPLOYEE_ID", length = 50)
    private String approverManagerEmployeeId;

    @Column(name = "APPROVER_MANAGER_USERNAME", length = 100)
    private String approverManagerUsername;

    @Column(name = "APPROVER_MANAGER_FULL_NAME", length = 150)
    private String approverManagerFullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIMESHEET_STATUS", nullable = false, length = 30)
    private AttendanceTimesheetStatus timesheetStatus = AttendanceTimesheetStatus.DRAFT;

    @Column(name = "APPROVAL_REQUIRED", nullable = false)
    private boolean approvalRequired;

    @Column(name = "SUBMITTED_AT")
    private LocalDateTime submittedAt;

    @Column(name = "APPROVED_AT")
    private LocalDateTime approvedAt;

    @Column(name = "REJECTED_AT")
    private LocalDateTime rejectedAt;

    @Column(name = "APPROVER_COMMENT", length = 500)
    private String approverComment;

    @OneToMany(mappedBy = "timesheet", orphanRemoval = true, cascade = jakarta.persistence.CascadeType.ALL)
    private List<ErmAttendanceTimesheetDay> days = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public Long getEmployeeUserId() {
        return employeeUserId;
    }

    public void setEmployeeUserId(Long employeeUserId) {
        this.employeeUserId = employeeUserId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeUsername() {
        return employeeUsername;
    }

    public void setEmployeeUsername(String employeeUsername) {
        this.employeeUsername = employeeUsername;
    }

    public String getEmployeeFullName() {
        return employeeFullName;
    }

    public void setEmployeeFullName(String employeeFullName) {
        this.employeeFullName = employeeFullName;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public LocalDate getWeekEndDate() {
        return weekEndDate;
    }

    public void setWeekEndDate(LocalDate weekEndDate) {
        this.weekEndDate = weekEndDate;
    }

    public Long getApproverManagerUserId() {
        return approverManagerUserId;
    }

    public void setApproverManagerUserId(Long approverManagerUserId) {
        this.approverManagerUserId = approverManagerUserId;
    }

    public String getApproverManagerEmployeeId() {
        return approverManagerEmployeeId;
    }

    public void setApproverManagerEmployeeId(String approverManagerEmployeeId) {
        this.approverManagerEmployeeId = approverManagerEmployeeId;
    }

    public String getApproverManagerUsername() {
        return approverManagerUsername;
    }

    public void setApproverManagerUsername(String approverManagerUsername) {
        this.approverManagerUsername = approverManagerUsername;
    }

    public String getApproverManagerFullName() {
        return approverManagerFullName;
    }

    public void setApproverManagerFullName(String approverManagerFullName) {
        this.approverManagerFullName = approverManagerFullName;
    }

    public AttendanceTimesheetStatus getTimesheetStatus() {
        return timesheetStatus;
    }

    public void setTimesheetStatus(AttendanceTimesheetStatus timesheetStatus) {
        this.timesheetStatus = timesheetStatus;
    }

    public boolean isApprovalRequired() {
        return approvalRequired;
    }

    public void setApprovalRequired(boolean approvalRequired) {
        this.approvalRequired = approvalRequired;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public String getApproverComment() {
        return approverComment;
    }

    public void setApproverComment(String approverComment) {
        this.approverComment = approverComment;
    }

    public List<ErmAttendanceTimesheetDay> getDays() {
        return days;
    }

    public void setDays(List<ErmAttendanceTimesheetDay> days) {
        this.days = days;
    }

    public void addDay(ErmAttendanceTimesheetDay day) {
        this.days.add(day);
        day.setTimesheet(this);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
