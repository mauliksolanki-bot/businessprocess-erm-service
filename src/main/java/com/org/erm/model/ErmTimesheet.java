package com.org.erm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ERM_TIMESHEETS")
public class ErmTimesheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TIMESHEET_CODE", nullable = false, unique = true, length = 40)
    private String timesheetCode;

    @Column(name = "EMPLOYEE_USER_ID", nullable = false)
    private Long employeeUserId;

    @Column(name = "EMPLOYEE_USERNAME", nullable = false, length = 100)
    private String employeeUsername;

    @Column(name = "EMPLOYEE_FULL_NAME", nullable = false, length = 150)
    private String employeeFullName;

    @Column(name = "REPORTING_MANAGER_USER_ID")
    private Long reportingManagerUserId;

    @Column(name = "REPORTING_MANAGER_USERNAME", length = 100)
    private String reportingManagerUsername;

    @Column(name = "REPORTING_MANAGER_FULL_NAME", length = 150)
    private String reportingManagerFullName;

    @Column(name = "WEEK_START_DATE", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "WEEK_END_DATE", nullable = false)
    private LocalDate weekEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 50)
    private TimesheetStatus status = TimesheetStatus.PENDING_MANAGER_APPROVAL;

    @Column(name = "SUBMISSION_COMMENT", length = 500)
    private String submissionComment;

    @Column(name = "MANAGER_ACTION_BY_USERNAME", length = 100)
    private String managerActionByUsername;

    @Column(name = "MANAGER_ACTION_AT")
    private LocalDateTime managerActionAt;

    @Column(name = "MANAGER_COMMENT", length = 500)
    private String managerComment;

    @Column(name = "TOTAL_HOURS", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalHours = BigDecimal.ZERO;

    @Column(name = "BILLABLE_HOURS", nullable = false, precision = 5, scale = 2)
    private BigDecimal billableHours = BigDecimal.ZERO;

    @Column(name = "NON_BILLABLE_HOURS", nullable = false, precision = 5, scale = 2)
    private BigDecimal nonBillableHours = BigDecimal.ZERO;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public String getTimesheetCode() {
        return timesheetCode;
    }

    public void setTimesheetCode(String timesheetCode) {
        this.timesheetCode = timesheetCode;
    }

    public Long getEmployeeUserId() {
        return employeeUserId;
    }

    public void setEmployeeUserId(Long employeeUserId) {
        this.employeeUserId = employeeUserId;
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

    public Long getReportingManagerUserId() {
        return reportingManagerUserId;
    }

    public void setReportingManagerUserId(Long reportingManagerUserId) {
        this.reportingManagerUserId = reportingManagerUserId;
    }

    public String getReportingManagerUsername() {
        return reportingManagerUsername;
    }

    public void setReportingManagerUsername(String reportingManagerUsername) {
        this.reportingManagerUsername = reportingManagerUsername;
    }

    public String getReportingManagerFullName() {
        return reportingManagerFullName;
    }

    public void setReportingManagerFullName(String reportingManagerFullName) {
        this.reportingManagerFullName = reportingManagerFullName;
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

    public TimesheetStatus getStatus() {
        return status;
    }

    public void setStatus(TimesheetStatus status) {
        this.status = status;
    }

    public String getSubmissionComment() {
        return submissionComment;
    }

    public void setSubmissionComment(String submissionComment) {
        this.submissionComment = submissionComment;
    }

    public String getManagerActionByUsername() {
        return managerActionByUsername;
    }

    public void setManagerActionByUsername(String managerActionByUsername) {
        this.managerActionByUsername = managerActionByUsername;
    }

    public LocalDateTime getManagerActionAt() {
        return managerActionAt;
    }

    public void setManagerActionAt(LocalDateTime managerActionAt) {
        this.managerActionAt = managerActionAt;
    }

    public String getManagerComment() {
        return managerComment;
    }

    public void setManagerComment(String managerComment) {
        this.managerComment = managerComment;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(BigDecimal totalHours) {
        this.totalHours = totalHours;
    }

    public BigDecimal getBillableHours() {
        return billableHours;
    }

    public void setBillableHours(BigDecimal billableHours) {
        this.billableHours = billableHours;
    }

    public BigDecimal getNonBillableHours() {
        return nonBillableHours;
    }

    public void setNonBillableHours(BigDecimal nonBillableHours) {
        this.nonBillableHours = nonBillableHours;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
