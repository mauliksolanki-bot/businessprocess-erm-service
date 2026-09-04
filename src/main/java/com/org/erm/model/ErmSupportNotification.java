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
@Table(name = "ERM_SUPPORT_NOTIFICATIONS")
public class ErmSupportNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TICKET_ID", nullable = false)
    private Long ticketId;

    @Column(name = "RECIPIENT_USERNAME", nullable = false, length = 100)
    private String recipientUsername;

    @Column(name = "CHANNEL", nullable = false, length = 20)
    private String channel;

    @Column(name = "EVENT_TYPE", nullable = false, length = 50)
    private String eventType;

    @Column(name = "MESSAGE_TEXT", nullable = false, length = 1000)
    private String messageText;

    @Column(name = "READ_AT")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public void setRecipientUsername(String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
