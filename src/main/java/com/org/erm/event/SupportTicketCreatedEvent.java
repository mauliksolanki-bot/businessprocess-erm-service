package com.org.erm.event;

/**
 * Published after a support ticket has been committed to the database, so that
 * best-effort external integrations (e.g. GitHub issue creation) can react to it
 * without risking a race against the enclosing transaction.
 */
public record SupportTicketCreatedEvent(Long ticketId, String requesterFullName, String requesterEmployeeId) {
}
