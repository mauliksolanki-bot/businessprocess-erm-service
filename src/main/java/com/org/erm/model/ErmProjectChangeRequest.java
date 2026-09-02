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
@Table(name = "ERM_PROJECT_CHANGE_REQUESTS")
public class ErmProjectChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PROJECT_REQUEST_ID", nullable = false)
    private Long projectRequestId;

    @Column(name = "PROJECT_NAME", nullable = false, length = 150)
    private String projectName;

    @Column(name = "PROJECT_CODE", nullable = false, length = 30)
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

    @Column(name = "DELIVERY_MANAGER_NAME", nullable = false, length = 150)
    private String deliveryManagerName;

    @Column(name = "PROJECT_OWNER_USER_ID", nullable = false)
    private Long projectOwnerUserId;

    @Column(name = "PROJECT_OWNER_NAME", nullable = false, length = 150)
    private String projectOwnerName;

    @Column(name = "PROJECT_DIRECTOR_USER_ID")
    private Long projectDirectorUserId;

    @Column(name = "PROJECT_DIRECTOR_NAME", length = 150)
    private String projectDirectorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "PROJECT_STATUS", nullable = false, length = 30)
    private ProjectStatus projectStatus = ProjectStatus.PLANNED;

    @Column(name = "DESCRIPTION", nullable = false, length = 2000)
    private String description;

    @Column(name = "RISK_NOTES", length = 1000)
    private String riskNotes;

    @Column(name = "CHANGE_REASON", nullable = false, length = 500)
    private String changeReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "WORKFLOW_STAGE", nullable = false, length = 50)
    private ProjectChangeWorkflowStage workflowStage = ProjectChangeWorkflowStage.PENDING_DELIVERY_MANAGER_APPROVAL;

    @Column(name = "CREATED_BY_USERNAME", nullable = false, length = 100)
    private String createdByUsername;

    @Column(name = "DM_ACTION_BY", length = 100)
    private String dmActionBy;

    @Column(name = "DM_ACTION_AT")
    private LocalDateTime dmActionAt;

    @Column(name = "DM_COMMENT", length = 500)
    private String dmComment;

    @Column(name = "PROJECT_OWNER_ACTION_BY", length = 100)
    private String projectOwnerActionBy;

    @Column(name = "PROJECT_OWNER_ACTION_AT")
    private LocalDateTime projectOwnerActionAt;

    @Column(name = "PROJECT_OWNER_COMMENT", length = 500)
    private String projectOwnerComment;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public Long getProjectRequestId() { return projectRequestId; }
    public void setProjectRequestId(Long projectRequestId) { this.projectRequestId = projectRequestId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public LocalDate getPlannedStartDate() { return plannedStartDate; }
    public void setPlannedStartDate(LocalDate plannedStartDate) { this.plannedStartDate = plannedStartDate; }
    public LocalDate getPlannedEndDate() { return plannedEndDate; }
    public void setPlannedEndDate(LocalDate plannedEndDate) { this.plannedEndDate = plannedEndDate; }
    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(BigDecimal budgetAmount) { this.budgetAmount = budgetAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Long getDeliveryManagerUserId() { return deliveryManagerUserId; }
    public void setDeliveryManagerUserId(Long deliveryManagerUserId) { this.deliveryManagerUserId = deliveryManagerUserId; }
    public String getDeliveryManagerName() { return deliveryManagerName; }
    public void setDeliveryManagerName(String deliveryManagerName) { this.deliveryManagerName = deliveryManagerName; }
    public Long getProjectOwnerUserId() { return projectOwnerUserId; }
    public void setProjectOwnerUserId(Long projectOwnerUserId) { this.projectOwnerUserId = projectOwnerUserId; }
    public String getProjectOwnerName() { return projectOwnerName; }
    public void setProjectOwnerName(String projectOwnerName) { this.projectOwnerName = projectOwnerName; }
    public Long getProjectDirectorUserId() { return projectDirectorUserId; }
    public void setProjectDirectorUserId(Long projectDirectorUserId) { this.projectDirectorUserId = projectDirectorUserId; }
    public String getProjectDirectorName() { return projectDirectorName; }
    public void setProjectDirectorName(String projectDirectorName) { this.projectDirectorName = projectDirectorName; }
    public ProjectStatus getProjectStatus() { return projectStatus; }
    public void setProjectStatus(ProjectStatus projectStatus) { this.projectStatus = projectStatus; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRiskNotes() { return riskNotes; }
    public void setRiskNotes(String riskNotes) { this.riskNotes = riskNotes; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    public ProjectChangeWorkflowStage getWorkflowStage() { return workflowStage; }
    public void setWorkflowStage(ProjectChangeWorkflowStage workflowStage) { this.workflowStage = workflowStage; }
    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }
    public String getDmActionBy() { return dmActionBy; }
    public void setDmActionBy(String dmActionBy) { this.dmActionBy = dmActionBy; }
    public LocalDateTime getDmActionAt() { return dmActionAt; }
    public void setDmActionAt(LocalDateTime dmActionAt) { this.dmActionAt = dmActionAt; }
    public String getDmComment() { return dmComment; }
    public void setDmComment(String dmComment) { this.dmComment = dmComment; }
    public String getProjectOwnerActionBy() { return projectOwnerActionBy; }
    public void setProjectOwnerActionBy(String projectOwnerActionBy) { this.projectOwnerActionBy = projectOwnerActionBy; }
    public LocalDateTime getProjectOwnerActionAt() { return projectOwnerActionAt; }
    public void setProjectOwnerActionAt(LocalDateTime projectOwnerActionAt) { this.projectOwnerActionAt = projectOwnerActionAt; }
    public String getProjectOwnerComment() { return projectOwnerComment; }
    public void setProjectOwnerComment(String projectOwnerComment) { this.projectOwnerComment = projectOwnerComment; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
