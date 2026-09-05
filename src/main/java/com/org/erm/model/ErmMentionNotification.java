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
@Table(name = "ERM_MENTION_NOTIFICATIONS")
public class ErmMentionNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "RECIPIENT_USERNAME", nullable = false, length = 100)
    private String recipientUsername;

    @Column(name = "ACTOR_USERNAME", nullable = false, length = 100)
    private String actorUsername;

    @Column(name = "CONTEXT_TYPE", nullable = false, length = 60)
    private String contextType;

    @Column(name = "CONTEXT_ID")
    private Long contextId;

    @Column(name = "MESSAGE_TEXT", nullable = false, length = 1000)
    private String messageText;

    @Column(name = "HREF", nullable = false, length = 255)
    private String href;

    @Column(name = "READ_AT")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getContextType() {
        return contextType;
    }

    public Long getContextId() {
        return contextId;
    }

    public String getMessageText() {
        return messageText;
    }

    public String getHref() {
        return href;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setRecipientUsername(String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public void setContextType(String contextType) {
        this.contextType = contextType;
    }

    public void setContextId(Long contextId) {
        this.contextId = contextId;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
