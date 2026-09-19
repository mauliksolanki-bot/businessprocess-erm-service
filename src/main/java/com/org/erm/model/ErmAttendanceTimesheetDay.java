package com.org.erm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ERM_ATTENDANCE_TIMESHEET_DAYS")
public class ErmAttendanceTimesheetDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TIMESHEET_ID", nullable = false)
    private ErmAttendanceTimesheet timesheet;

    @Column(name = "WORK_DATE", nullable = false)
    private LocalDate workDate;

    @Column(name = "BILLABLE_HOURS", nullable = false, precision = 4, scale = 2)
    private BigDecimal billableHours = BigDecimal.ZERO;

    @Column(name = "NON_BILLABLE_HOURS", nullable = false, precision = 4, scale = 2)
    private BigDecimal nonBillableHours = BigDecimal.ZERO;

    @Column(name = "BILLABLE_PROJECT_ALLOCATION_ID")
    private Long billableProjectAllocationId;

    @Column(name = "BILLABLE_PROJECT_REQUEST_ID")
    private Long billableProjectRequestId;

    @Column(name = "BILLABLE_PROJECT_NAME", length = 150)
    private String billableProjectName;

    @Column(name = "BILLABLE_PROJECT_CODE", length = 30)
    private String billableProjectCode;

    @Column(name = "NON_BILLABLE_PROJECT_ALLOCATION_ID")
    private Long nonBillableProjectAllocationId;

    @Column(name = "NON_BILLABLE_PROJECT_REQUEST_ID")
    private Long nonBillableProjectRequestId;

    @Column(name = "NON_BILLABLE_PROJECT_NAME", length = 150)
    private String nonBillableProjectName;

    @Column(name = "NON_BILLABLE_PROJECT_CODE", length = 30)
    private String nonBillableProjectCode;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public ErmAttendanceTimesheet getTimesheet() {
        return timesheet;
    }

    public void setTimesheet(ErmAttendanceTimesheet timesheet) {
        this.timesheet = timesheet;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
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

    public Long getBillableProjectAllocationId() {
        return billableProjectAllocationId;
    }

    public void setBillableProjectAllocationId(Long billableProjectAllocationId) {
        this.billableProjectAllocationId = billableProjectAllocationId;
    }

    public Long getBillableProjectRequestId() {
        return billableProjectRequestId;
    }

    public void setBillableProjectRequestId(Long billableProjectRequestId) {
        this.billableProjectRequestId = billableProjectRequestId;
    }

    public String getBillableProjectName() {
        return billableProjectName;
    }

    public void setBillableProjectName(String billableProjectName) {
        this.billableProjectName = billableProjectName;
    }

    public String getBillableProjectCode() {
        return billableProjectCode;
    }

    public void setBillableProjectCode(String billableProjectCode) {
        this.billableProjectCode = billableProjectCode;
    }

    public Long getNonBillableProjectAllocationId() {
        return nonBillableProjectAllocationId;
    }

    public void setNonBillableProjectAllocationId(Long nonBillableProjectAllocationId) {
        this.nonBillableProjectAllocationId = nonBillableProjectAllocationId;
    }

    public Long getNonBillableProjectRequestId() {
        return nonBillableProjectRequestId;
    }

    public void setNonBillableProjectRequestId(Long nonBillableProjectRequestId) {
        this.nonBillableProjectRequestId = nonBillableProjectRequestId;
    }

    public String getNonBillableProjectName() {
        return nonBillableProjectName;
    }

    public void setNonBillableProjectName(String nonBillableProjectName) {
        this.nonBillableProjectName = nonBillableProjectName;
    }

    public String getNonBillableProjectCode() {
        return nonBillableProjectCode;
    }

    public void setNonBillableProjectCode(String nonBillableProjectCode) {
        this.nonBillableProjectCode = nonBillableProjectCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
