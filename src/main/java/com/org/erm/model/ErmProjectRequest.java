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
@Table(name = "ERM_PROJECT_REQUESTS")
public class ErmProjectRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PROJECT_NAME", nullable = false, length = 150)
    private String projectName;

    @Column(name = "PROJECT_CODE", nullable = false, unique = true, length = 30)
    private String projectCode;

    @Column(name = "CLIENT_NAME", nullable = false, length = 150)
    private String clientName;

    @Column(name = "PROJECT_TYPE", nullable = false, length = 40)
    private String projectType;

    @Column(name = "PRIORITY", nullable = false, length = 20)
    private String priority;

    @Column(name = "PLANNED_START_DATE", nullable = false)
    private LocalDate plannedStartDate;

    @Column(name = "PLANNED_END_DATE", nullable = false)
    private LocalDate plannedEndDate;

    @Column(name = "BUDGET_AMOUNT", nullable = false, precision = 15, scale = 2)
    private BigDecimal budgetAmount;

    @Column(name = "CURRENCY", nullable = false, length = 10)
    private String currency;

    @Column(name = "DELIVERY_MANAGER_USER_ID", nullable = false)
    private Long deliveryManagerUserId;

    @Column(name = "DELIVERY_MANAGER_EMPLOYEE_ID", nullable = false, length = 50)
    private String deliveryManagerEmployeeId;

    @Column(name = "DELIVERY_MANAGER_NAME", nullable = false, length = 150)
    private String deliveryManagerName;

    @Column(name = "PROJECT_OWNER_USER_ID", nullable = false)
    private Long projectOwnerUserId;

    @Column(name = "PROJECT_OWNER_EMPLOYEE_ID", nullable = false, length = 50)
    private String projectOwnerEmployeeId;

    @Column(name = "PROJECT_OWNER_NAME", nullable = false, length = 150)
    private String projectOwnerName;

    @Column(name = "PROJECT_DIRECTOR_USER_ID")
    private Long projectDirectorUserId;

    @Column(name = "PROJECT_DIRECTOR_EMPLOYEE_ID", length = 50)
    private String projectDirectorEmployeeId;

    @Column(name = "PROJECT_DIRECTOR_NAME", length = 150)
    private String projectDirectorName;

    @Column(name = "PROJECT_MANAGER_USER_ID")
    private Long projectManagerUserId;

    @Column(name = "PROJECT_MANAGER_EMPLOYEE_ID", length = 50)
    private String projectManagerEmployeeId;

    @Column(name = "PROJECT_MANAGER_NAME", length = 150)
    private String projectManagerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "PROJECT_STATUS", nullable = false, length = 30)
    private ProjectStatus projectStatus = ProjectStatus.PLANNED;

    @Column(name = "DESCRIPTION", nullable = false, length = 2000)
    private String description;

    @Column(name = "RISK_NOTES", length = 1000)
    private String riskNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "WORKFLOW_STAGE", nullable = false, length = 50)
    private ProjectWorkflowStage workflowStage = ProjectWorkflowStage.PM_SUBMITTED;

    @Column(name = "CREATED_BY_USERNAME", nullable = false, length = 100)
    private String createdByUsername;

    @Column(name = "DM_ACTION_BY", length = 100)
    private String dmActionBy;

    @Column(name = "DM_ACTION_AT")
    private LocalDateTime dmActionAt;

    @Column(name = "DM_COMMENT", length = 500)
    private String dmComment;

    @Column(name = "CTO_ACTION_BY", length = 100)
    private String ctoActionBy;

    @Column(name = "CTO_ACTION_AT")
    private LocalDateTime ctoActionAt;

    @Column(name = "CTO_COMMENT", length = 500)
    private String ctoComment;

    @Column(name = "DIRECTOR_ACTION_BY", length = 100)
    private String directorActionBy;

    @Column(name = "DIRECTOR_ACTION_AT")
    private LocalDateTime directorActionAt;

    @Column(name = "DIRECTOR_COMMENT", length = 500)
    private String directorComment;

    @Column(name = "SUPER_ADMIN_ACTION_BY", length = 100)
    private String superAdminActionBy;

    @Column(name = "SUPER_ADMIN_ACTION_AT")
    private LocalDateTime superAdminActionAt;

    @Column(name = "SUPER_ADMIN_COMMENT", length = 500)
    private String superAdminComment;

    @Column(name = "REFER_BACK_BY", length = 100)
    private String referBackBy;

    @Column(name = "REFER_BACK_AT")
    private LocalDateTime referBackAt;

    @Column(name = "REFER_BACK_COMMENT", length = 500)
    private String referBackComment;

    @Column(name = "REFER_BACK_STAGE", length = 100)
    private String referBackStage;

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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDate getPlannedStartDate() {
        return plannedStartDate;
    }

    public void setPlannedStartDate(LocalDate plannedStartDate) {
        this.plannedStartDate = plannedStartDate;
    }

    public LocalDate getPlannedEndDate() {
        return plannedEndDate;
    }

    public void setPlannedEndDate(LocalDate plannedEndDate) {
        this.plannedEndDate = plannedEndDate;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getDeliveryManagerUserId() {
        return deliveryManagerUserId;
    }

    public void setDeliveryManagerUserId(Long deliveryManagerUserId) {
        this.deliveryManagerUserId = deliveryManagerUserId;
    }

    public String getDeliveryManagerEmployeeId() { return deliveryManagerEmployeeId; }
    public void setDeliveryManagerEmployeeId(String deliveryManagerEmployeeId) { this.deliveryManagerEmployeeId = deliveryManagerEmployeeId; }

    public String getDeliveryManagerName() {
        return deliveryManagerName;
    }

    public void setDeliveryManagerName(String deliveryManagerName) {
        this.deliveryManagerName = deliveryManagerName;
    }

    public Long getProjectOwnerUserId() {
        return projectOwnerUserId;
    }

    public void setProjectOwnerUserId(Long projectOwnerUserId) {
        this.projectOwnerUserId = projectOwnerUserId;
    }

    public String getProjectOwnerEmployeeId() { return projectOwnerEmployeeId; }
    public void setProjectOwnerEmployeeId(String projectOwnerEmployeeId) { this.projectOwnerEmployeeId = projectOwnerEmployeeId; }

    public String getProjectOwnerName() {
        return projectOwnerName;
    }

    public void setProjectOwnerName(String projectOwnerName) {
        this.projectOwnerName = projectOwnerName;
    }

    public Long getProjectDirectorUserId() {
        return projectDirectorUserId;
    }

    public void setProjectDirectorUserId(Long projectDirectorUserId) {
        this.projectDirectorUserId = projectDirectorUserId;
    }

    public String getProjectDirectorEmployeeId() { return projectDirectorEmployeeId; }
    public void setProjectDirectorEmployeeId(String projectDirectorEmployeeId) { this.projectDirectorEmployeeId = projectDirectorEmployeeId; }

    public String getProjectDirectorName() {
        return projectDirectorName;
    }

    public void setProjectDirectorName(String projectDirectorName) {
        this.projectDirectorName = projectDirectorName;
    }

    public Long getProjectManagerUserId() {
        return projectManagerUserId;
    }

    public void setProjectManagerUserId(Long projectManagerUserId) {
        this.projectManagerUserId = projectManagerUserId;
    }

    public String getProjectManagerEmployeeId() { return projectManagerEmployeeId; }
    public void setProjectManagerEmployeeId(String projectManagerEmployeeId) { this.projectManagerEmployeeId = projectManagerEmployeeId; }

    public String getProjectManagerName() {
        return projectManagerName;
    }

    public void setProjectManagerName(String projectManagerName) {
        this.projectManagerName = projectManagerName;
    }

    public ProjectStatus getProjectStatus() {
        return projectStatus;
    }

    public void setProjectStatus(ProjectStatus projectStatus) {
        this.projectStatus = projectStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRiskNotes() {
        return riskNotes;
    }

    public void setRiskNotes(String riskNotes) {
        this.riskNotes = riskNotes;
    }

    public ProjectWorkflowStage getWorkflowStage() {
        return workflowStage;
    }

    public void setWorkflowStage(ProjectWorkflowStage workflowStage) {
        this.workflowStage = workflowStage;
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

    public String getCtoActionBy() {
        return ctoActionBy;
    }

    public void setCtoActionBy(String ctoActionBy) {
        this.ctoActionBy = ctoActionBy;
    }

    public LocalDateTime getCtoActionAt() {
        return ctoActionAt;
    }

    public void setCtoActionAt(LocalDateTime ctoActionAt) {
        this.ctoActionAt = ctoActionAt;
    }

    public String getCtoComment() {
        return ctoComment;
    }

    public void setCtoComment(String ctoComment) {
        this.ctoComment = ctoComment;
    }

    public String getDirectorActionBy() {
        return directorActionBy;
    }

    public void setDirectorActionBy(String directorActionBy) {
        this.directorActionBy = directorActionBy;
    }

    public LocalDateTime getDirectorActionAt() {
        return directorActionAt;
    }

    public void setDirectorActionAt(LocalDateTime directorActionAt) {
        this.directorActionAt = directorActionAt;
    }

    public String getDirectorComment() {
        return directorComment;
    }

    public void setDirectorComment(String directorComment) {
        this.directorComment = directorComment;
    }

    public String getSuperAdminActionBy() {
        return superAdminActionBy;
    }

    public void setSuperAdminActionBy(String superAdminActionBy) {
        this.superAdminActionBy = superAdminActionBy;
    }

    public LocalDateTime getSuperAdminActionAt() {
        return superAdminActionAt;
    }

    public void setSuperAdminActionAt(LocalDateTime superAdminActionAt) {
        this.superAdminActionAt = superAdminActionAt;
    }

    public String getSuperAdminComment() {
        return superAdminComment;
    }

    public void setSuperAdminComment(String superAdminComment) {
        this.superAdminComment = superAdminComment;
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

    public String getReferBackStage() {
        return referBackStage;
    }

    public void setReferBackStage(String referBackStage) {
        this.referBackStage = referBackStage;
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
