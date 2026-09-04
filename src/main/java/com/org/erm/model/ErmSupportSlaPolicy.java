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
@Table(name = "ERM_SUPPORT_SLA_POLICIES")
public class ErmSupportSlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "QUEUE_CODE", nullable = false, length = 100)
    private String queueCode;

    @Column(name = "TICKET_TYPE", nullable = false, length = 50)
    private String ticketType;

    @Column(name = "PRIORITY_CODE", nullable = false, length = 20)
    private String priorityCode;

    @Column(name = "RESPONSE_MINUTES", nullable = false)
    private Integer responseMinutes;

    @Column(name = "RESOLUTION_MINUTES", nullable = false)
    private Integer resolutionMinutes;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public String getQueueCode() {
        return queueCode;
    }

    public String getTicketType() {
        return ticketType;
    }

    public String getPriorityCode() {
        return priorityCode;
    }

    public Integer getResponseMinutes() {
        return responseMinutes;
    }

    public Integer getResolutionMinutes() {
        return resolutionMinutes;
    }

    public boolean isActive() {
        return active;
    }
}
