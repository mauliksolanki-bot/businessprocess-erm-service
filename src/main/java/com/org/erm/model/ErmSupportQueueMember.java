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
@Table(name = "ERM_SUPPORT_QUEUE_MEMBERS")
public class ErmSupportQueueMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "QUEUE_ID", nullable = false)
    private Long queueId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean active = true;

    @Column(name = "LAST_ASSIGNED_AT")
    private LocalDateTime lastAssignedAt;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public Long getQueueId() {
        return queueId;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getLastAssignedAt() {
        return lastAssignedAt;
    }

    public void setLastAssignedAt(LocalDateTime lastAssignedAt) {
        this.lastAssignedAt = lastAssignedAt;
    }
}
