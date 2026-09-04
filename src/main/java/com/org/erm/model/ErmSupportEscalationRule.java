package com.org.erm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ERM_SUPPORT_ESCALATION_RULES")
public class ErmSupportEscalationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "QUEUE_CODE", nullable = false, length = 100)
    private String queueCode;

    @Column(name = "PRIORITY_CODE", nullable = false, length = 20)
    private String priorityCode;

    @Column(name = "WARN_MINUTES", nullable = false)
    private Integer warnMinutes;

    @Column(name = "BREACH_MINUTES", nullable = false)
    private Integer breachMinutes;

    @Column(name = "ESCALATE_TO_ROLE_NAME", nullable = false, length = 100)
    private String escalateToRoleName;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public String getQueueCode() {
        return queueCode;
    }

    public String getPriorityCode() {
        return priorityCode;
    }

    public Integer getWarnMinutes() {
        return warnMinutes;
    }

    public Integer getBreachMinutes() {
        return breachMinutes;
    }

    public String getEscalateToRoleName() {
        return escalateToRoleName;
    }

    public boolean isActive() {
        return active;
    }
}
