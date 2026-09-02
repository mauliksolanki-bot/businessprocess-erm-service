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
@Table(name = "ERM_EMPLOYEE_PROFILE_UPDATE_REQUESTS")
public class ErmEmployeeProfileUpdateRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "EMPLOYEE_USER_ID", nullable = false)
    private Long employeeUserId;

    @Column(name = "EMPLOYEE_USERNAME", nullable = false, length = 100)
    private String employeeUsername;

    @Column(name = "CURRENT_FULL_NAME", nullable = false, length = 150)
    private String currentFullName;

    @Column(name = "CURRENT_EMAIL", nullable = false, length = 150)
    private String currentEmail;

    @Column(name = "CURRENT_DEPARTMENT", nullable = false, length = 100)
    private String currentDepartment;

    @Column(name = "CURRENT_EMPLOYMENT_STATUS", nullable = false, length = 30)
    private String currentEmploymentStatus;

    @Column(name = "CURRENT_DESIGNATION_ROLE_NAME", nullable = false, length = 100)
    private String currentDesignationRoleName;

    @Column(name = "CURRENT_REPORTING_MANAGER_USER_ID")
    private Long currentReportingManagerUserId;

    @Column(name = "CURRENT_REPORTING_MANAGER_NAME", length = 150)
    private String currentReportingManagerName;

    @Column(name = "REQUESTED_FULL_NAME", nullable = false, length = 150)
    private String requestedFullName;

    @Column(name = "REQUESTED_EMAIL", nullable = false, length = 150)
    private String requestedEmail;

    @Column(name = "REQUESTED_DEPARTMENT", nullable = false, length = 100)
    private String requestedDepartment;

    @Column(name = "REQUESTED_EMPLOYMENT_STATUS", nullable = false, length = 30)
    private String requestedEmploymentStatus;

    @Column(name = "REQUESTED_DESIGNATION_ROLE_NAME", nullable = false, length = 100)
    private String requestedDesignationRoleName;

    @Column(name = "REQUESTED_REPORTING_MANAGER_USER_ID", nullable = false)
    private Long requestedReportingManagerUserId;

    @Column(name = "REQUESTED_REPORTING_MANAGER_NAME", nullable = false, length = 150)
    private String requestedReportingManagerName;

    @Column(name = "REPLACEMENT_TEAM_LEAD_USER_ID")
    private Long replacementTeamLeadUserId;

    @Column(name = "REPLACEMENT_TEAM_LEAD_NAME", length = 150)
    private String replacementTeamLeadName;

    @Column(name = "DIRECT_REPORTS_AFFECTED_COUNT", nullable = false)
    private Integer directReportsAffectedCount = 0;

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

    @Column(name = "CANCELLED_BY", length = 100)
    private String cancelledBy;

    @Column(name = "CANCELLED_AT")
    private LocalDateTime cancelledAt;

    @Column(name = "CANCELLED_COMMENT", length = 500)
    private String cancelledComment;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public Long getEmployeeUserId() { return employeeUserId; }
    public void setEmployeeUserId(Long employeeUserId) { this.employeeUserId = employeeUserId; }
    public String getEmployeeUsername() { return employeeUsername; }
    public void setEmployeeUsername(String employeeUsername) { this.employeeUsername = employeeUsername; }
    public String getCurrentFullName() { return currentFullName; }
    public void setCurrentFullName(String currentFullName) { this.currentFullName = currentFullName; }
    public String getCurrentEmail() { return currentEmail; }
    public void setCurrentEmail(String currentEmail) { this.currentEmail = currentEmail; }
    public String getCurrentDepartment() { return currentDepartment; }
    public void setCurrentDepartment(String currentDepartment) { this.currentDepartment = currentDepartment; }
    public String getCurrentEmploymentStatus() { return currentEmploymentStatus; }
    public void setCurrentEmploymentStatus(String currentEmploymentStatus) { this.currentEmploymentStatus = currentEmploymentStatus; }
    public String getCurrentDesignationRoleName() { return currentDesignationRoleName; }
    public void setCurrentDesignationRoleName(String currentDesignationRoleName) { this.currentDesignationRoleName = currentDesignationRoleName; }
    public Long getCurrentReportingManagerUserId() { return currentReportingManagerUserId; }
    public void setCurrentReportingManagerUserId(Long currentReportingManagerUserId) { this.currentReportingManagerUserId = currentReportingManagerUserId; }
    public String getCurrentReportingManagerName() { return currentReportingManagerName; }
    public void setCurrentReportingManagerName(String currentReportingManagerName) { this.currentReportingManagerName = currentReportingManagerName; }
    public String getRequestedFullName() { return requestedFullName; }
    public void setRequestedFullName(String requestedFullName) { this.requestedFullName = requestedFullName; }
    public String getRequestedEmail() { return requestedEmail; }
    public void setRequestedEmail(String requestedEmail) { this.requestedEmail = requestedEmail; }
    public String getRequestedDepartment() { return requestedDepartment; }
    public void setRequestedDepartment(String requestedDepartment) { this.requestedDepartment = requestedDepartment; }
    public String getRequestedEmploymentStatus() { return requestedEmploymentStatus; }
    public void setRequestedEmploymentStatus(String requestedEmploymentStatus) { this.requestedEmploymentStatus = requestedEmploymentStatus; }
    public String getRequestedDesignationRoleName() { return requestedDesignationRoleName; }
    public void setRequestedDesignationRoleName(String requestedDesignationRoleName) { this.requestedDesignationRoleName = requestedDesignationRoleName; }
    public Long getRequestedReportingManagerUserId() { return requestedReportingManagerUserId; }
    public void setRequestedReportingManagerUserId(Long requestedReportingManagerUserId) { this.requestedReportingManagerUserId = requestedReportingManagerUserId; }
    public String getRequestedReportingManagerName() { return requestedReportingManagerName; }
    public void setRequestedReportingManagerName(String requestedReportingManagerName) { this.requestedReportingManagerName = requestedReportingManagerName; }
    public Long getReplacementTeamLeadUserId() { return replacementTeamLeadUserId; }
    public void setReplacementTeamLeadUserId(Long replacementTeamLeadUserId) { this.replacementTeamLeadUserId = replacementTeamLeadUserId; }
    public String getReplacementTeamLeadName() { return replacementTeamLeadName; }
    public void setReplacementTeamLeadName(String replacementTeamLeadName) { this.replacementTeamLeadName = replacementTeamLeadName; }
    public Integer getDirectReportsAffectedCount() { return directReportsAffectedCount; }
    public void setDirectReportsAffectedCount(Integer directReportsAffectedCount) { this.directReportsAffectedCount = directReportsAffectedCount; }
    public OnboardingWorkflowStage getWorkflowStage() { return workflowStage; }
    public void setWorkflowStage(OnboardingWorkflowStage workflowStage) { this.workflowStage = workflowStage; }
    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }
    public String getHrComment() { return hrComment; }
    public void setHrComment(String hrComment) { this.hrComment = hrComment; }
    public String getHeadHrComment() { return headHrComment; }
    public void setHeadHrComment(String headHrComment) { this.headHrComment = headHrComment; }
    public String getChroComment() { return chroComment; }
    public void setChroComment(String chroComment) { this.chroComment = chroComment; }
    public String getSuperAdminComment() { return superAdminComment; }
    public void setSuperAdminComment(String superAdminComment) { this.superAdminComment = superAdminComment; }
    public String getHrActionBy() { return hrActionBy; }
    public void setHrActionBy(String hrActionBy) { this.hrActionBy = hrActionBy; }
    public String getHeadHrActionBy() { return headHrActionBy; }
    public void setHeadHrActionBy(String headHrActionBy) { this.headHrActionBy = headHrActionBy; }
    public String getChroActionBy() { return chroActionBy; }
    public void setChroActionBy(String chroActionBy) { this.chroActionBy = chroActionBy; }
    public String getSuperAdminActionBy() { return superAdminActionBy; }
    public void setSuperAdminActionBy(String superAdminActionBy) { this.superAdminActionBy = superAdminActionBy; }
    public LocalDateTime getHrActionAt() { return hrActionAt; }
    public void setHrActionAt(LocalDateTime hrActionAt) { this.hrActionAt = hrActionAt; }
    public LocalDateTime getHeadHrActionAt() { return headHrActionAt; }
    public void setHeadHrActionAt(LocalDateTime headHrActionAt) { this.headHrActionAt = headHrActionAt; }
    public LocalDateTime getChroActionAt() { return chroActionAt; }
    public void setChroActionAt(LocalDateTime chroActionAt) { this.chroActionAt = chroActionAt; }
    public LocalDateTime getSuperAdminActionAt() { return superAdminActionAt; }
    public void setSuperAdminActionAt(LocalDateTime superAdminActionAt) { this.superAdminActionAt = superAdminActionAt; }
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelledComment() { return cancelledComment; }
    public void setCancelledComment(String cancelledComment) { this.cancelledComment = cancelledComment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
