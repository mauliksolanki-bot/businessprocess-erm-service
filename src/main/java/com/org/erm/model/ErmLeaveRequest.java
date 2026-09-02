package com.org.erm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ERM_LEAVE_REQUESTS")
public class ErmLeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "EMPLOYEE_USER_ID", nullable = false)
    private Long employeeUserId;

    @Column(name = "EMPLOYEE_USERNAME", nullable = false, length = 100)
    private String employeeUsername;

    @Column(name = "EMPLOYEE_FULL_NAME", nullable = false, length = 150)
    private String employeeFullName;

    @Column(name = "APPROVER_MANAGER_USER_ID")
    private Long approverManagerUserId;

    @Column(name = "APPROVER_MANAGER_USERNAME", length = 100)
    private String approverManagerUsername;

    @Column(name = "APPROVER_MANAGER_FULL_NAME", length = 150)
    private String approverManagerFullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "LEAVE_CATEGORY", nullable = false, length = 30)
    private LeaveCategory leaveCategory;

    @Column(name = "START_DATE", nullable = false)
    private LocalDate startDate;

    @Column(name = "END_DATE", nullable = false)
    private LocalDate endDate;

    @Column(name = "REQUESTED_DAYS", nullable = false)
    private Integer requestedDays;

    @Column(name = "REASON", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "REQUEST_STATUS", nullable = false, length = 30)
    private LeaveRequestStatus requestStatus = LeaveRequestStatus.PENDING;

    @Column(name = "APPROVER_COMMENT", length = 500)
    private String approverComment;

    @Column(name = "APPROVER_ACTION_AT")
    private LocalDateTime approverActionAt;

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

    public Long getApproverManagerUserId() {
        return approverManagerUserId;
    }

    public void setApproverManagerUserId(Long approverManagerUserId) {
        this.approverManagerUserId = approverManagerUserId;
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

    public LeaveCategory getLeaveCategory() {
        return leaveCategory;
    }

    public void setLeaveCategory(LeaveCategory leaveCategory) {
        this.leaveCategory = leaveCategory;
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

    public Integer getRequestedDays() {
        return requestedDays;
    }

    public void setRequestedDays(Integer requestedDays) {
        this.requestedDays = requestedDays;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LeaveRequestStatus getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(LeaveRequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getApproverComment() {
        return approverComment;
    }

    public void setApproverComment(String approverComment) {
        this.approverComment = approverComment;
    }

    public LocalDateTime getApproverActionAt() {
        return approverActionAt;
    }

    public void setApproverActionAt(LocalDateTime approverActionAt) {
        this.approverActionAt = approverActionAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
