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
@Table(name = "ERM_SUPPORT_AUDIT_LOG")
public class ErmSupportAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TICKET_ID", nullable = false)
    private Long ticketId;

    @Column(name = "ACTOR_USERNAME", nullable = false, length = 100)
    private String actorUsername;

    @Column(name = "ACTION_TYPE", nullable = false, length = 50)
    private String actionType;

    @Column(name = "FROM_STATUS", length = 50)
    private String fromStatus;

    @Column(name = "TO_STATUS", length = 50)
    private String toStatus;

    @Column(name = "DETAILS", length = 1000)
    private String details;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
