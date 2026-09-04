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
@Table(name = "ERM_SUPPORT_QUEUES")
public class ErmSupportQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "QUEUE_CODE", nullable = false, unique = true, length = 100)
    private String queueCode;

    @Column(name = "QUEUE_TITLE", nullable = false, length = 120)
    private String queueTitle;

    @Column(name = "QUEUE_TYPE", nullable = false, length = 50)
    private String queueType;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getQueueCode() {
        return queueCode;
    }

    public String getQueueTitle() {
        return queueTitle;
    }

    public String getQueueType() {
        return queueType;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }
}
