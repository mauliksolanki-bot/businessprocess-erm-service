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
@Table(name = "ERM_PROJECT_ALLOCATIONS")
public class ErmProjectAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ALLOCATION_CODE", nullable = false, unique = true, length = 40)
    private String allocationCode;

    @Column(name = "PROJECT_REQUEST_ID", nullable = false)
    private Long projectRequestId;

    @Column(name = "PROJECT_NAME", nullable = false, length = 150)
    private String projectName;

    @Column(name = "PROJECT_CODE", nullable = false, length = 30)
    private String projectCode;

    @Column(name = "EMPLOYEE_USER_ID", nullable = false)
    private Long employeeUserId;

    @Column(name = "EMPLOYEE_ID", nullable = false, length = 50)
    private String employeeId;

    @Column(name = "EMPLOYEE_NAME", nullable = false, length = 150)
    private String employeeName;

    @Column(name = "EMPLOYEE_ROLE_NAME", length = 100)
    private String employeeRoleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "ALLOCATION_TYPE", nullable = false, length = 30)
    private ProjectAllocationType allocationType;

    @Column(name = "ALLOCATION_PERCENT", nullable = false, precision = 5, scale = 2)
    private BigDecimal allocationPercent;

    @Column(name = "START_DATE", nullable = false)
    private LocalDate startDate;

    @Column(name = "END_DATE", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 40)
    private ProjectAllocationStatus status = ProjectAllocationStatus.PENDING_DM_APPROVAL;

    @Column(name = "CREATED_BY_USERNAME", nullable = false, length = 100)
    private String createdByUsername;

    @Column(name = "DM_ACTION_BY", length = 100)
    private String dmActionBy;

    @Column(name = "DM_ACTION_AT")
    private LocalDateTime dmActionAt;

    @Column(name = "DM_COMMENT", length = 500)
    private String dmComment;

    @Column(name = "REFER_BACK_BY", length = 100)
    private String referBackBy;

    @Column(name = "REFER_BACK_AT")
    private LocalDateTime referBackAt;

    @Column(name = "REFER_BACK_COMMENT", length = 500)
    private String referBackComment;

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

    public String getAllocationCode() {
        return allocationCode;
    }

    public void setAllocationCode(String allocationCode) {
        this.allocationCode = allocationCode;
    }

    public Long getProjectRequestId() {
        return projectRequestId;
    }

    public void setProjectRequestId(Long projectRequestId) {
        this.projectRequestId = projectRequestId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
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

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeRoleName() {
        return employeeRoleName;
    }

    public void setEmployeeRoleName(String employeeRoleName) {
        this.employeeRoleName = employeeRoleName;
    }

    public ProjectAllocationType getAllocationType() {
        return allocationType;
    }

    public void setAllocationType(ProjectAllocationType allocationType) {
        this.allocationType = allocationType;
    }

    public BigDecimal getAllocationPercent() {
        return allocationPercent;
    }

    public void setAllocationPercent(BigDecimal allocationPercent) {
        this.allocationPercent = allocationPercent;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public ProjectAllocationStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectAllocationStatus status) {
        this.status = status;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public String getDmActionBy() {
        return dmActionBy;
    }

    public void setDmActionBy(String dmActionBy) {
        this.dmActionBy = dmActionBy;
    }

    public LocalDateTime getDmActionAt() {
        return dmActionAt;
    }

    public void setDmActionAt(LocalDateTime dmActionAt) {
        this.dmActionAt = dmActionAt;
    }

    public String getDmComment() {
        return dmComment;
    }

    public void setDmComment(String dmComment) {
        this.dmComment = dmComment;
    }

    public String getReferBackBy() {
        return referBackBy;
    }

    public void setReferBackBy(String referBackBy) {
        this.referBackBy = referBackBy;
    }

    public LocalDateTime getReferBackAt() {
        return referBackAt;
    }

    public void setReferBackAt(LocalDateTime referBackAt) {
        this.referBackAt = referBackAt;
    }

    public String getReferBackComment() {
        return referBackComment;
    }

    public void setReferBackComment(String referBackComment) {
        this.referBackComment = referBackComment;
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
