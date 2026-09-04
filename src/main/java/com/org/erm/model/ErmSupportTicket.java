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

import java.time.LocalDateTime;

@Entity
@Table(name = "ERM_SUPPORT_TICKETS")
public class ErmSupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TICKET_NUMBER", nullable = false, unique = true, length = 30)
    private String ticketNumber;

    @Column(name = "REQUESTER_USER_ID", nullable = false)
    private Long requesterUserId;

    @Column(name = "REQUESTER_USERNAME", nullable = false, length = 100)
    private String requesterUsername;

    @Column(name = "REQUESTER_FULL_NAME", nullable = false, length = 150)
    private String requesterFullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "TICKET_TYPE", nullable = false, length = 50)
    private SupportTicketType ticketType;

    @Column(name = "CATEGORY_CODE", nullable = false, length = 100)
    private String categoryCode;

    @Column(name = "CATEGORY_TITLE", nullable = false, length = 120)
    private String categoryTitle;

    @Column(name = "SUBCATEGORY_CODE", length = 100)
    private String subcategoryCode;

    @Column(name = "SUBCATEGORY_TITLE", length = 120)
    private String subcategoryTitle;

    @Column(name = "IMPACT_LEVEL", nullable = false, length = 20)
    private String impactLevel;

    @Column(name = "URGENCY_LEVEL", nullable = false, length = 20)
    private String urgencyLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "PRIORITY_CODE", nullable = false, length = 20)
    private SupportPriority priorityCode;

    @Column(name = "QUEUE_ID", nullable = false)
    private Long queueId;

    @Column(name = "QUEUE_CODE", nullable = false, length = 100)
    private String queueCode;

    @Column(name = "QUEUE_TITLE", nullable = false, length = 120)
    private String queueTitle;

    @Column(name = "ASSIGNEE_USER_ID")
    private Long assigneeUserId;

    @Column(name = "ASSIGNEE_USERNAME", length = 100)
    private String assigneeUsername;

    @Column(name = "ASSIGNEE_FULL_NAME", length = 150)
    private String assigneeFullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 50)
    private SupportTicketStatus status = SupportTicketStatus.NEW;

    @Column(name = "SOURCE", nullable = false, length = 50)
    private String source;

    @Column(name = "SHORT_DESCRIPTION", nullable = false, length = 255)
    private String shortDescription;

    @Column(name = "DESCRIPTION", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "SECURITY_INCIDENT", nullable = false)
    private boolean securityIncident;

    @Column(name = "RESPONSE_DUE_AT")
    private LocalDateTime responseDueAt;

    @Column(name = "RESOLUTION_DUE_AT")
    private LocalDateTime resolutionDueAt;

    @Column(name = "FIRST_RESPONSE_AT")
    private LocalDateTime firstResponseAt;

    @Column(name = "RESOLVED_AT")
    private LocalDateTime resolvedAt;

    @Column(name = "CLOSED_AT")
    private LocalDateTime closedAt;

    @Column(name = "CREATED_BY_USERNAME", nullable = false, length = 100)
    private String createdByUsername;

    @Column(name = "UPDATED_BY_USERNAME", nullable = false, length = 100)
    private String updatedByUsername;

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

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public Long getRequesterUserId() {
        return requesterUserId;
    }

    public void setRequesterUserId(Long requesterUserId) {
        this.requesterUserId = requesterUserId;
    }

    public String getRequesterUsername() {
        return requesterUsername;
    }

    public void setRequesterUsername(String requesterUsername) {
        this.requesterUsername = requesterUsername;
    }

    public String getRequesterFullName() {
        return requesterFullName;
    }

    public void setRequesterFullName(String requesterFullName) {
        this.requesterFullName = requesterFullName;
    }

    public SupportTicketType getTicketType() {
        return ticketType;
    }

    public void setTicketType(SupportTicketType ticketType) {
        this.ticketType = ticketType;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getCategoryTitle() {
        return categoryTitle;
    }

    public void setCategoryTitle(String categoryTitle) {
        this.categoryTitle = categoryTitle;
    }

    public String getSubcategoryCode() {
        return subcategoryCode;
    }

    public void setSubcategoryCode(String subcategoryCode) {
        this.subcategoryCode = subcategoryCode;
    }

    public String getSubcategoryTitle() {
        return subcategoryTitle;
    }

    public void setSubcategoryTitle(String subcategoryTitle) {
        this.subcategoryTitle = subcategoryTitle;
    }

    public String getImpactLevel() {
        return impactLevel;
    }

    public void setImpactLevel(String impactLevel) {
        this.impactLevel = impactLevel;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public SupportPriority getPriorityCode() {
        return priorityCode;
    }

    public void setPriorityCode(SupportPriority priorityCode) {
        this.priorityCode = priorityCode;
    }

    public Long getQueueId() {
        return queueId;
    }

    public void setQueueId(Long queueId) {
        this.queueId = queueId;
    }

    public String getQueueCode() {
        return queueCode;
    }

    public void setQueueCode(String queueCode) {
        this.queueCode = queueCode;
    }

    public String getQueueTitle() {
        return queueTitle;
    }

    public void setQueueTitle(String queueTitle) {
        this.queueTitle = queueTitle;
    }

    public Long getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(Long assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public String getAssigneeUsername() {
        return assigneeUsername;
    }

    public void setAssigneeUsername(String assigneeUsername) {
        this.assigneeUsername = assigneeUsername;
    }

    public String getAssigneeFullName() {
        return assigneeFullName;
    }

    public void setAssigneeFullName(String assigneeFullName) {
        this.assigneeFullName = assigneeFullName;
    }

    public SupportTicketStatus getStatus() {
        return status;
    }

    public void setStatus(SupportTicketStatus status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSecurityIncident() {
        return securityIncident;
    }

    public void setSecurityIncident(boolean securityIncident) {
        this.securityIncident = securityIncident;
    }

    public LocalDateTime getResponseDueAt() {
        return responseDueAt;
    }

    public void setResponseDueAt(LocalDateTime responseDueAt) {
        this.responseDueAt = responseDueAt;
    }

    public LocalDateTime getResolutionDueAt() {
        return resolutionDueAt;
    }

    public void setResolutionDueAt(LocalDateTime resolutionDueAt) {
        this.resolutionDueAt = resolutionDueAt;
    }

    public LocalDateTime getFirstResponseAt() {
        return firstResponseAt;
    }

    public void setFirstResponseAt(LocalDateTime firstResponseAt) {
        this.firstResponseAt = firstResponseAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public String getUpdatedByUsername() {
        return updatedByUsername;
    }

    public void setUpdatedByUsername(String updatedByUsername) {
        this.updatedByUsername = updatedByUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
