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

import java.time.LocalDateTime;

@Entity
@Table(name = "ERM_EMPLOYEE_DESIGNATION_REQUESTS")
public class ErmEmployeeDesignationRequest {

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

    @Column(name = "CURRENT_DESIGNATION_ROLE_NAME", nullable = false, length = 100)
    private String currentDesignationRoleName;

    @Column(name = "REQUESTED_DESIGNATION_ROLE_NAME", nullable = false, length = 100)
    private String requestedDesignationRoleName;

    @Column(name = "REQUESTED_REPORTING_MANAGER_USER_ID", nullable = false)
    private Long requestedReportingManagerUserId;

    @Column(name = "REQUESTED_REPORTING_MANAGER_USERNAME", nullable = false, length = 100)
    private String requestedReportingManagerUsername;

    @Column(name = "REQUESTED_REPORTING_MANAGER_FULL_NAME", nullable = false, length = 150)
    private String requestedReportingManagerFullName;

    @Column(name = "REQUESTED_REPORTING_MANAGER_ROLE_NAME", nullable = false, length = 100)
    private String requestedReportingManagerRoleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "WORKFLOW_STAGE", nullable = false, length = 40)
    private OnboardingWorkflowStage workflowStage = OnboardingWorkflowStage.HR_SUBMITTED;

    @Column(name = "CREATED_BY_USERNAME", nullable = false, length = 100)
    private String createdByUsername;

    @Column(name = "HR_COMMENT", length = 500)
    private String hrComment;

    @Column(name = "HEAD_HR_COMMENT", length = 500)
    private String headHrComment;

    @Column(name = "CHRO_COMMENT", length = 500)
    private String chroComment;

    @Column(name = "SUPER_ADMIN_COMMENT", length = 500)
    private String superAdminComment;

    @Column(name = "HR_ACTION_BY", length = 100)
    private String hrActionBy;

    @Column(name = "HEAD_HR_ACTION_BY", length = 100)
    private String headHrActionBy;

    @Column(name = "CHRO_ACTION_BY", length = 100)
    private String chroActionBy;

    @Column(name = "SUPER_ADMIN_ACTION_BY", length = 100)
    private String superAdminActionBy;

    @Column(name = "HR_ACTION_AT")
    private LocalDateTime hrActionAt;

    @Column(name = "HEAD_HR_ACTION_AT")
    private LocalDateTime headHrActionAt;

    @Column(name = "CHRO_ACTION_AT")
    private LocalDateTime chroActionAt;

    @Column(name = "SUPER_ADMIN_ACTION_AT")
    private LocalDateTime superAdminActionAt;

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

    public String getCurrentDesignationRoleName() {
        return currentDesignationRoleName;
    }

    public void setCurrentDesignationRoleName(String currentDesignationRoleName) {
        this.currentDesignationRoleName = currentDesignationRoleName;
    }

    public String getRequestedDesignationRoleName() {
        return requestedDesignationRoleName;
    }

    public void setRequestedDesignationRoleName(String requestedDesignationRoleName) {
        this.requestedDesignationRoleName = requestedDesignationRoleName;
    }

    public Long getRequestedReportingManagerUserId() {
        return requestedReportingManagerUserId;
    }

    public void setRequestedReportingManagerUserId(Long requestedReportingManagerUserId) {
        this.requestedReportingManagerUserId = requestedReportingManagerUserId;
    }

    public String getRequestedReportingManagerUsername() {
        return requestedReportingManagerUsername;
    }

    public void setRequestedReportingManagerUsername(String requestedReportingManagerUsername) {
        this.requestedReportingManagerUsername = requestedReportingManagerUsername;
    }

    public String getRequestedReportingManagerFullName() {
        return requestedReportingManagerFullName;
    }

    public void setRequestedReportingManagerFullName(String requestedReportingManagerFullName) {
        this.requestedReportingManagerFullName = requestedReportingManagerFullName;
    }

    public String getRequestedReportingManagerRoleName() {
        return requestedReportingManagerRoleName;
    }

    public void setRequestedReportingManagerRoleName(String requestedReportingManagerRoleName) {
        this.requestedReportingManagerRoleName = requestedReportingManagerRoleName;
    }

    public OnboardingWorkflowStage getWorkflowStage() {
        return workflowStage;
    }

    public void setWorkflowStage(OnboardingWorkflowStage workflowStage) {
        this.workflowStage = workflowStage;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public String getHrComment() {
        return hrComment;
    }

    public void setHrComment(String hrComment) {
        this.hrComment = hrComment;
    }

    public String getHeadHrComment() {
        return headHrComment;
    }

    public void setHeadHrComment(String headHrComment) {
        this.headHrComment = headHrComment;
    }

    public String getChroComment() {
        return chroComment;
    }

    public void setChroComment(String chroComment) {
        this.chroComment = chroComment;
    }

    public String getSuperAdminComment() {
        return superAdminComment;
    }

    public void setSuperAdminComment(String superAdminComment) {
        this.superAdminComment = superAdminComment;
    }

    public String getHrActionBy() {
        return hrActionBy;
    }

    public void setHrActionBy(String hrActionBy) {
        this.hrActionBy = hrActionBy;
    }

    public String getHeadHrActionBy() {
        return headHrActionBy;
    }

    public void setHeadHrActionBy(String headHrActionBy) {
        this.headHrActionBy = headHrActionBy;
    }

    public String getChroActionBy() {
        return chroActionBy;
    }

    public void setChroActionBy(String chroActionBy) {
        this.chroActionBy = chroActionBy;
    }

    public String getSuperAdminActionBy() {
        return superAdminActionBy;
    }

    public void setSuperAdminActionBy(String superAdminActionBy) {
        this.superAdminActionBy = superAdminActionBy;
    }

    public LocalDateTime getHrActionAt() {
        return hrActionAt;
    }

    public void setHrActionAt(LocalDateTime hrActionAt) {
        this.hrActionAt = hrActionAt;
    }

    public LocalDateTime getHeadHrActionAt() {
        return headHrActionAt;
    }

    public void setHeadHrActionAt(LocalDateTime headHrActionAt) {
        this.headHrActionAt = headHrActionAt;
    }

    public LocalDateTime getChroActionAt() {
        return chroActionAt;
    }

    public void setChroActionAt(LocalDateTime chroActionAt) {
        this.chroActionAt = chroActionAt;
    }

    public LocalDateTime getSuperAdminActionAt() {
        return superAdminActionAt;
    }

    public void setSuperAdminActionAt(LocalDateTime superAdminActionAt) {
        this.superAdminActionAt = superAdminActionAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
