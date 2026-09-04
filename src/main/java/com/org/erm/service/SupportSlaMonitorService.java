package com.org.erm.service;

import com.org.erm.model.ErmSupportAuditLog;
import com.org.erm.model.ErmSupportNotification;
import com.org.erm.model.ErmSupportTicket;
import com.org.erm.model.SupportTicketStatus;
import com.org.erm.repository.ErmSupportAuditLogRepository;
import com.org.erm.repository.ErmSupportNotificationRepository;
import com.org.erm.repository.ErmSupportTicketRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SupportSlaMonitorService {

    private static final List<SupportTicketStatus> TRACKED_STATUSES = List.of(
            SupportTicketStatus.NEW,
            SupportTicketStatus.ASSIGNED,
            SupportTicketStatus.IN_PROGRESS,
            SupportTicketStatus.PENDING_EMPLOYEE,
            SupportTicketStatus.REOPENED,
            SupportTicketStatus.SECURITY_ESCALATED,
            SupportTicketStatus.RESOLVED
    );

    private final ErmSupportTicketRepository ticketRepository;
    private final ErmSupportAuditLogRepository auditLogRepository;
    private final ErmSupportNotificationRepository notificationRepository;
    private final JdbcTemplate jdbcTemplate;

    public SupportSlaMonitorService(ErmSupportTicketRepository ticketRepository,
                                    ErmSupportAuditLogRepository auditLogRepository,
                                    ErmSupportNotificationRepository notificationRepository,
                                    JdbcTemplate jdbcTemplate) {
        this.ticketRepository = ticketRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelay = 600000)
    @Transactional
    public void evaluateOpenTickets() {
        if (!supportTablesAvailable()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (ErmSupportTicket ticket : ticketRepository.findAll()) {
            if (!TRACKED_STATUSES.contains(ticket.getStatus())) {
                continue;
            }
            evaluateTicket(ticket, now);
        }
    }

    private void evaluateTicket(ErmSupportTicket ticket, LocalDateTime now) {
        if (ticket.getCreatedAt() == null) {
            return;
        }
        if (ticket.getResponseDueAt() != null) {
            LocalDateTime warningAt = warningAt(ticket.getCreatedAt(), ticket.getResponseDueAt());
            if (!auditLogRepository.existsByTicketIdAndActionType(ticket.getId(), "SLA_WARNING_RESPONSE")
                    && !now.isBefore(warningAt)) {
                recordEvent(ticket, "SLA_WARNING_RESPONSE", "Support response SLA warning");
            }
            if (!auditLogRepository.existsByTicketIdAndActionType(ticket.getId(), "SLA_BREACH_RESPONSE")
                    && !now.isBefore(ticket.getResponseDueAt())) {
                recordEvent(ticket, "SLA_BREACH_RESPONSE", "Support response SLA breached");
            }
        }
        if (ticket.getResolutionDueAt() != null) {
            LocalDateTime warningAt = warningAt(ticket.getCreatedAt(), ticket.getResolutionDueAt());
            if (!auditLogRepository.existsByTicketIdAndActionType(ticket.getId(), "SLA_WARNING_RESOLUTION")
                    && !now.isBefore(warningAt)) {
                recordEvent(ticket, "SLA_WARNING_RESOLUTION", "Support resolution SLA warning");
            }
            if (!auditLogRepository.existsByTicketIdAndActionType(ticket.getId(), "SLA_BREACH_RESOLUTION")
                    && !now.isBefore(ticket.getResolutionDueAt())) {
                recordEvent(ticket, "SLA_BREACH_RESOLUTION", "Support resolution SLA breached");
            }
        }
    }

    private LocalDateTime warningAt(LocalDateTime createdAt, LocalDateTime dueAt) {
        Duration total = Duration.between(createdAt, dueAt);
        if (total.isZero() || total.isNegative()) {
            return dueAt;
        }
        return createdAt.plus(total.multipliedBy(4).dividedBy(5));
    }

    private boolean supportTablesAvailable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'ERM_SUPPORT_TICKETS'",
                Integer.class
        );
        return count != null && count > 0;
    }

    private void recordEvent(ErmSupportTicket ticket, String actionType, String details) {
        recordAudit(ticket, actionType, details);
        recordNotification(ticket, actionType, details);
    }

    private void recordAudit(ErmSupportTicket ticket, String actionType, String details) {
        ErmSupportAuditLog log = new ErmSupportAuditLog();
        log.setTicketId(ticket.getId());
        log.setActorUsername(StringUtils.hasText(ticket.getAssigneeUsername()) ? ticket.getAssigneeUsername() : ticket.getRequesterUsername());
        log.setActionType(actionType);
        log.setFromStatus(ticket.getStatus().name());
        log.setToStatus(ticket.getStatus().name());
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private void recordNotification(ErmSupportTicket ticket, String actionType, String details) {
        if (StringUtils.hasText(ticket.getRequesterUsername())) {
            saveNotification(ticket, ticket.getRequesterUsername(), "EMAIL", actionType, details);
            saveNotification(ticket, ticket.getRequesterUsername(), "IN_APP", actionType, details);
        }
        if (StringUtils.hasText(ticket.getAssigneeUsername())) {
            saveNotification(ticket, ticket.getAssigneeUsername(), "EMAIL", actionType, details);
            saveNotification(ticket, ticket.getAssigneeUsername(), "IN_APP", actionType, details);
        }
    }

    private void saveNotification(ErmSupportTicket ticket, String recipientUsername, String channel, String eventType, String messageText) {
        ErmSupportNotification notification = new ErmSupportNotification();
        notification.setTicketId(ticket.getId());
        notification.setRecipientUsername(recipientUsername);
        notification.setChannel(channel);
        notification.setEventType(eventType);
        notification.setMessageText(messageText);
        notificationRepository.save(notification);
    }
}
