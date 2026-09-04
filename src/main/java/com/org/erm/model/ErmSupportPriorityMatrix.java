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
@Table(name = "ERM_SUPPORT_PRIORITY_MATRIX")
public class ErmSupportPriorityMatrix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "IMPACT_LEVEL", nullable = false, length = 20)
    private String impactLevel;

    @Column(name = "URGENCY_LEVEL", nullable = false, length = 20)
    private String urgencyLevel;

    @Column(name = "PRIORITY_CODE", nullable = false, length = 20)
    private String priorityCode;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public String getImpactLevel() {
        return impactLevel;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public String getPriorityCode() {
        return priorityCode;
    }

    public boolean isActive() {
        return active;
    }
}
