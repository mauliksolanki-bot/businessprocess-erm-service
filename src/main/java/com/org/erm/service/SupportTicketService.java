package com.org.erm.service;

import com.org.erm.dto.response.SupportCategoryOptionResponse;
import com.org.erm.dto.response.SupportCatalogResponse;
import com.org.erm.dto.response.SupportQueueSummaryResponse;
import com.org.erm.dto.request.SupportTicketAssignRequest;
import com.org.erm.dto.request.SupportTicketCommentRequest;
import com.org.erm.dto.response.SupportTicketCommentResponse;
import com.org.erm.dto.request.SupportTicketCreateRequest;
import com.org.erm.dto.response.SupportTicketResponse;
import com.org.erm.dto.request.SupportTicketStatusRequest;
import com.org.erm.model.ErmSupportAuditLog;
import com.org.erm.model.ErmSupportCategory;
import com.org.erm.model.ErmSupportNotification;
import com.org.erm.model.ErmSupportPriorityMatrix;
import com.org.erm.model.ErmSupportQueue;
import com.org.erm.model.ErmSupportQueueMember;
import com.org.erm.model.ErmSupportSlaPolicy;
import com.org.erm.model.ErmSupportTicket;
import com.org.erm.model.ErmSupportTicketComment;
import com.org.erm.model.ErmUser;
import com.org.erm.model.SupportPriority;
import com.org.erm.model.SupportTicketStatus;
import com.org.erm.model.SupportTicketType;
import com.org.erm.repository.ErmSupportAuditLogRepository;
import com.org.erm.repository.ErmSupportCategoryRepository;
import com.org.erm.repository.ErmSupportNotificationRepository;
import com.org.erm.repository.ErmSupportPriorityMatrixRepository;
import com.org.erm.repository.ErmSupportQueueMemberRepository;
import com.org.erm.repository.ErmSupportQueueRepository;
import com.org.erm.repository.ErmSupportSlaPolicyRepository;
import com.org.erm.repository.ErmSupportTicketCommentRepository;
import com.org.erm.repository.ErmSupportTicketRepository;
import com.org.erm.repository.ErmUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SupportTicketService {

    private static final List<String> IMPACT_LEVELS = List.of("Low", "Medium", "High", "Critical");
    private static final List<String> URGENCY_LEVELS = List.of("Low", "Medium", "High", "Critical");
    private static final List<String> OPEN_STATUS_NAMES = List.of(
            SupportTicketStatus.NEW.name(),
            SupportTicketStatus.ASSIGNED.name(),
            SupportTicketStatus.IN_PROGRESS.name(),
            SupportTicketStatus.PENDING_EMPLOYEE.name(),
            SupportTicketStatus.REOPENED.name(),
            SupportTicketStatus.SECURITY_ESCALATED.name()
    );

    private final ErmSupportTicketRepository ticketRepository;
    private final ErmSupportTicketCommentRepository commentRepository;
    private final ErmSupportCategoryRepository categoryRepository;
    private final ErmSupportQueueRepository queueRepository;
    private final ErmSupportQueueMemberRepository queueMemberRepository;
    private final ErmSupportPriorityMatrixRepository priorityMatrixRepository;
    private final ErmSupportSlaPolicyRepository slaPolicyRepository;
    private final ErmSupportAuditLogRepository auditLogRepository;
    private final ErmSupportNotificationRepository notificationRepository;
    private final ErmUserRepository userRepository;

    public SupportTicketService(ErmSupportTicketRepository ticketRepository,
                                ErmSupportTicketCommentRepository commentRepository,
                                ErmSupportCategoryRepository categoryRepository,
                                ErmSupportQueueRepository queueRepository,
                                ErmSupportQueueMemberRepository queueMemberRepository,
                                ErmSupportPriorityMatrixRepository priorityMatrixRepository,
                                ErmSupportSlaPolicyRepository slaPolicyRepository,
                                ErmSupportAuditLogRepository auditLogRepository,
                                ErmSupportNotificationRepository notificationRepository,
                                ErmUserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.categoryRepository = categoryRepository;
        this.queueRepository = queueRepository;
        this.queueMemberRepository = queueMemberRepository;
        this.priorityMatrixRepository = priorityMatrixRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SupportTicketResponse create(SupportTicketCreateRequest request, Authentication authentication) {
        ErmUser requester = loadCurrentUser(authentication);
        SupportTicketType ticketType = parseTicketType(request.ticketType());
        String queueCode = queueCodeFor(ticketType);
        ErmSupportCategory category = loadCategory(request.categoryCode(), ticketType);
        ErmSupportQueue queue = loadQueue(queueCode);
        SupportPriority priority = derivePriority(request.impactLevel(), request.urgencyLevel(), ticketType);
        LocalDateTime now = LocalDateTime.now();

        ErmSupportTicket ticket = new ErmSupportTicket();
        ticket.setTicketNumber(generateTicketNumber());
        ticket.setRequesterUserId(requester.getId());
        ticket.setRequesterUsername(requester.getUsername());
        ticket.setRequesterFullName(requester.getFullName());
        ticket.setTicketType(ticketType);
        ticket.setCategoryCode(category.getCategoryCode());
        ticket.setCategoryTitle(category.getCategoryTitle());
        ticket.setSubcategoryCode(normalizeOptional(request.subcategoryCode()));
        ticket.setSubcategoryTitle(normalizeOptional(request.subcategoryCode()));
        ticket.setImpactLevel(normalizeRequired(request.impactLevel(), "Impact is required"));
        ticket.setUrgencyLevel(normalizeRequired(request.urgencyLevel(), "Urgency is required"));
        ticket.setPriorityCode(priority);
        ticket.setQueueId(queue.getId());
        ticket.setQueueCode(queue.getQueueCode());
        ticket.setQueueTitle(queue.getQueueTitle());
        ticket.setSource(StringUtils.hasText(request.source()) ? request.source().trim().toUpperCase(Locale.ROOT) : "PORTAL");
        ticket.setShortDescription(normalizeRequired(request.shortDescription(), "Short description is required"));
        ticket.setDescription(normalizeRequired(request.description(), "Description is required"));
        ticket.setSecurityIncident(ticketType == SupportTicketType.SECURITY_INCIDENT);
        ticket.setStatus(SupportTicketStatus.NEW);
        ticket.setCreatedByUsername(requester.getUsername());
        ticket.setUpdatedByUsername(requester.getUsername());
        applySla(ticket, queue.getQueueCode(), ticketType.name(), priority.name(), now);

        ErmSupportQueueMember assignedMember = autoAssign(ticket, queue, now);
        ticket = ticketRepository.save(ticket);

        recordAudit(ticket.getId(), requester.getUsername(), "CREATED", null, ticket.getStatus().name(), "Support ticket created");
        recordNotification(ticket.getId(), requester.getUsername(), "IN_APP", "CREATED", "Ticket " + ticket.getTicketNumber() + " created");
        if (assignedMember != null && ticket.getAssigneeUsername() != null) {
            recordNotification(ticket.getId(), ticket.getAssigneeUsername(), "IN_APP", "ASSIGNED", "Ticket " + ticket.getTicketNumber() + " assigned to you");
        }

        return toResponse(ticket, true);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> list(String scope, String queueCode, String status, Authentication authentication) {
        ErmUser currentUser = loadCurrentUser(authentication);
        List<ErmSupportTicket> tickets;
        String normalizedScope = StringUtils.hasText(scope) ? scope.trim().toLowerCase(Locale.ROOT) : "mine";

        if ("mine".equals(normalizedScope)) {
            tickets = ticketRepository.findAllByRequesterUserIdOrderByCreatedAtDesc(currentUser.getId());
        } else if ("assigned".equals(normalizedScope)) {
            tickets = ticketRepository.findAllByAssigneeUserIdOrderByCreatedAtDesc(currentUser.getId());
        } else if ("queue".equals(normalizedScope)) {
            ErmSupportQueue queue = loadQueue(queueCode);
            ensureQueueAccess(currentUser.getId(), queue.getId());
            tickets = ticketRepository.findAllByQueueIdInOrderByCreatedAtDesc(List.of(queue.getId()));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid scope");
        }

        SupportTicketStatus filteredStatus = parseStatus(status);
        return tickets.stream()
                .filter(ticket -> filteredStatus == null || ticket.getStatus() == filteredStatus)
                .map(ticket -> toResponse(ticket, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse getById(Long ticketId, Authentication authentication) {
        ErmSupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support ticket not found"));
        ErmUser currentUser = loadCurrentUser(authentication);
        ensureCanView(currentUser.getId(), ticket);
        return toResponse(ticket, true);
    }

    @Transactional
    public SupportTicketResponse addComment(Long ticketId, SupportTicketCommentRequest request, Authentication authentication) {
        ErmSupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support ticket not found"));
        ErmUser currentUser = loadCurrentUser(authentication);
        ensureCanView(currentUser.getId(), ticket);
        if (ticket.getStatus() == SupportTicketStatus.CLOSED || ticket.getStatus() == SupportTicketStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comments are disabled for closed tickets");
        }

        LocalDateTime now = LocalDateTime.now();
        ErmSupportTicketComment comment = new ErmSupportTicketComment();
        comment.setTicketId(ticket.getId());
        comment.setActorUsername(currentUser.getUsername());
        comment.setActionType("COMMENT");
        comment.setCommentText(normalizeRequired(request.comment(), "Comment is required"));
        commentRepository.save(comment);
        ticket.setUpdatedByUsername(currentUser.getUsername());
        ticketRepository.save(ticket);

        recordAudit(ticket.getId(), currentUser.getUsername(), "COMMENT", ticket.getStatus().name(), ticket.getStatus().name(), comment.getCommentText());
        recordNotification(ticket.getId(), ticket.getRequesterUsername(), "IN_APP", "COMMENT", "New comment on ticket " + ticket.getTicketNumber());
        if (ticket.getAssigneeUsername() != null) {
            recordNotification(ticket.getId(), ticket.getAssigneeUsername(), "IN_APP", "COMMENT", "New comment on ticket " + ticket.getTicketNumber());
        }
        return toResponse(ticket, true);
    }

    @Transactional
    public SupportTicketResponse updateStatus(Long ticketId, SupportTicketStatusRequest request, Authentication authentication) {
        ErmSupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support ticket not found"));
        ErmUser currentUser = loadCurrentUser(authentication);
        ensureCanView(currentUser.getId(), ticket);

        SupportTicketStatus nextStatus = request.status();
        SupportTicketStatus previousStatus = ticket.getStatus();
        LocalDateTime now = LocalDateTime.now();

        ticket.setStatus(nextStatus);
        ticket.setUpdatedByUsername(currentUser.getUsername());
        if (nextStatus == SupportTicketStatus.IN_PROGRESS && ticket.getFirstResponseAt() == null) {
            ticket.setFirstResponseAt(now);
        }
        if (nextStatus == SupportTicketStatus.RESOLVED) {
            ticket.setResolvedAt(now);
        }
        if (nextStatus == SupportTicketStatus.CLOSED) {
            ticket.setClosedAt(now);
            if (ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(now);
            }
        }
        if (nextStatus == SupportTicketStatus.REOPENED) {
            ticket.setClosedAt(null);
        }

        ticket = ticketRepository.save(ticket);
        if (StringUtils.hasText(request.comment())) {
            ErmSupportTicketComment comment = new ErmSupportTicketComment();
            comment.setTicketId(ticket.getId());
            comment.setActorUsername(currentUser.getUsername());
            comment.setActionType("STATUS_CHANGE");
            comment.setCommentText(normalizeRequired(request.comment(), "Comment is required"));
            commentRepository.save(comment);
        }
        recordAudit(ticket.getId(), currentUser.getUsername(), "STATUS_CHANGE", previousStatus.name(), nextStatus.name(), "Status changed");
        recordNotification(ticket.getId(), ticket.getRequesterUsername(), "IN_APP", "STATUS_CHANGE", "Ticket " + ticket.getTicketNumber() + " moved to " + nextStatus.name());
        return toResponse(ticket, true);
    }

    @Transactional
    public SupportTicketResponse assign(Long ticketId, SupportTicketAssignRequest request, Authentication authentication) {
        ErmSupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support ticket not found"));
        ErmUser currentUser = loadCurrentUser(authentication);
        ensureCanView(currentUser.getId(), ticket);

        ErmSupportQueue queue = queueRepository.findById(ticket.getQueueId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Support queue not found"));

        if (StringUtils.hasText(request.queueCode())) {
            queue = loadQueue(request.queueCode());
            ticket.setQueueId(queue.getId());
            ticket.setQueueCode(queue.getQueueCode());
            ticket.setQueueTitle(queue.getQueueTitle());
        }

        if (request.assigneeUserId() == null) {
            autoAssign(ticket, queue, LocalDateTime.now());
        } else {
            ensureQueueMember(queue.getId(), request.assigneeUserId());
            ErmUser assignee = userRepository.findById(request.assigneeUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignee not found"));
            ticket.setAssigneeUserId(assignee.getId());
            ticket.setAssigneeUsername(assignee.getUsername());
            ticket.setAssigneeFullName(assignee.getFullName());
            queueMemberRepository.findByQueueIdAndUserId(queue.getId(), assignee.getId()).ifPresent(member -> {
                member.setLastAssignedAt(LocalDateTime.now());
                queueMemberRepository.save(member);
            });
        }

        ticket.setStatus(SupportTicketStatus.ASSIGNED);
        ticket.setUpdatedByUsername(currentUser.getUsername());
        ticket = ticketRepository.save(ticket);
        recordAudit(ticket.getId(), currentUser.getUsername(), "ASSIGN", null, ticket.getStatus().name(), "Ticket assigned");
        return toResponse(ticket, true);
    }

    @Transactional(readOnly = true)
    public SupportCatalogResponse catalog(Authentication authentication) {
        loadCurrentUser(authentication);
        List<SupportCategoryOptionResponse> categories = categoryRepository.findAllByActiveTrueOrderBySortOrderAscCategoryTitleAsc().stream()
                .map(item -> new SupportCategoryOptionResponse(
                        item.getCategoryCode(),
                        item.getCategoryTitle(),
                        item.getTicketType(),
                        item.getParentCategoryCode()
                ))
                .toList();
        List<SupportQueueSummaryResponse> queues = queueRepository.findAllByActiveTrueOrderByQueueTitleAsc().stream()
                .map(queue -> new SupportQueueSummaryResponse(
                        queue.getId(),
                        queue.getQueueCode(),
                        queue.getQueueTitle(),
                        queue.getQueueType(),
                        queueMemberRepository.countByQueueIdAndActiveTrue(queue.getId()),
                        ticketRepository.countByQueueIdAndStatusIn(queue.getId(), openStatuses())
                ))
                .toList();
        return new SupportCatalogResponse(List.of(
                SupportTicketType.SUPPORT_TICKET.name(),
                SupportTicketType.INCIDENT.name(),
                SupportTicketType.SECURITY_INCIDENT.name()
        ), IMPACT_LEVELS, URGENCY_LEVELS, categories, queues);
    }

    @Transactional(readOnly = true)
    public List<SupportQueueSummaryResponse> workbenchQueues(Authentication authentication) {
        ErmUser currentUser = loadCurrentUser(authentication);
        List<ErmSupportQueueMember> memberships = queueMemberRepository.findAllByUserIdAndActiveTrue(currentUser.getId());
        if (memberships.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized for support workbench");
        }

        return memberships.stream()
                .map(membership -> queueRepository.findById(membership.getQueueId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Queue not found")))
                .distinct()
                .map(queue -> new SupportQueueSummaryResponse(
                        queue.getId(),
                        queue.getQueueCode(),
                        queue.getQueueTitle(),
                        queue.getQueueType(),
                        queueMemberRepository.countByQueueIdAndActiveTrue(queue.getId()),
                        ticketRepository.countByQueueIdAndStatusIn(queue.getId(), openStatuses())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> workbenchTickets(String queueCode, Authentication authentication) {
        ErmUser currentUser = loadCurrentUser(authentication);
        ErmSupportQueue queue = loadQueue(queueCode);
        ensureQueueAccess(currentUser.getId(), queue.getId());
        return ticketRepository.findAllByQueueIdInOrderByCreatedAtDesc(List.of(queue.getId())).stream()
                .filter(ticket -> OPEN_STATUS_NAMES.contains(ticket.getStatus().name()))
                .map(ticket -> toResponse(ticket, false))
                .toList();
    }

    private ErmUser loadCurrentUser(Authentication authentication) {
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current user not found"));
    }

    private SupportTicketType parseTicketType(String value) {
        try {
            return SupportTicketType.valueOf(normalizeRequired(value, "Ticket type is required").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ticket type");
        }
    }

    private SupportTicketStatus parseStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return SupportTicketStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ticket status");
        }
    }

    private String queueCodeFor(SupportTicketType ticketType) {
        if (ticketType == SupportTicketType.SECURITY_INCIDENT) {
            return "security";
        }
        if (ticketType == SupportTicketType.INCIDENT) {
            return "incident";
        }
        return "support";
    }

    private ErmSupportCategory loadCategory(String categoryCode, SupportTicketType ticketType) {
        ErmSupportCategory category = categoryRepository.findByCategoryCodeIgnoreCaseAndActiveTrue(normalizeRequired(categoryCode, "Category is required"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category"));
        if (!category.getTicketType().equalsIgnoreCase(ticketType.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category does not match selected ticket type");
        }
        return category;
    }

    private ErmSupportQueue loadQueue(String queueCode) {
        return queueRepository.findByQueueCodeIgnoreCaseAndActiveTrue(normalizeRequired(queueCode, "Queue code is required"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown support queue"));
    }

    private SupportPriority derivePriority(String impactLevel, String urgencyLevel, SupportTicketType ticketType) {
        if (ticketType == SupportTicketType.SECURITY_INCIDENT) {
            return SupportPriority.P1;
        }
        String normalizedImpact = normalizeRequired(impactLevel, "Impact is required");
        String normalizedUrgency = normalizeRequired(urgencyLevel, "Urgency is required");
        if (!IMPACT_LEVELS.contains(normalizedImpact) || !URGENCY_LEVELS.contains(normalizedUrgency)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impact and urgency must be one of Low, Medium, High, Critical");
        }
        ErmSupportPriorityMatrix matrix = priorityMatrixRepository.findByImpactLevelIgnoreCaseAndUrgencyLevelIgnoreCaseAndActiveTrue(normalizedImpact, normalizedUrgency)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No priority matrix configured for impact and urgency"));
        return SupportPriority.valueOf(matrix.getPriorityCode());
    }

    private void applySla(ErmSupportTicket ticket, String queueCode, String ticketType, String priorityCode, LocalDateTime now) {
        ErmSupportSlaPolicy slaPolicy = slaPolicyRepository
                .findByQueueCodeIgnoreCaseAndTicketTypeIgnoreCaseAndPriorityCodeIgnoreCaseAndActiveTrue(queueCode, ticketType, priorityCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No SLA policy configured for this ticket"));
        ticket.setResponseDueAt(now.plusMinutes(slaPolicy.getResponseMinutes()));
        ticket.setResolutionDueAt(now.plusMinutes(slaPolicy.getResolutionMinutes()));
    }

    private ErmSupportQueueMember autoAssign(ErmSupportTicket ticket, ErmSupportQueue queue, LocalDateTime now) {
        List<ErmSupportQueueMember> members = queueMemberRepository.findAllByQueueIdAndActiveTrueOrderByLastAssignedAtAscIdAsc(queue.getId());
        if (members.isEmpty()) {
            ticket.setAssigneeUserId(null);
            ticket.setAssigneeUsername(null);
            ticket.setAssigneeFullName(null);
            return null;
        }

        ErmSupportQueueMember selected = members.stream()
                .map(member -> new MemberCandidate(member, ticketRepository.countByAssigneeUserIdAndStatusIn(member.getUserId(), openStatuses())))
                .sorted(Comparator
                        .comparingLong(MemberCandidate::openTicketCount)
                        .thenComparing(candidate -> candidate.member().getLastAssignedAt(), Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(candidate -> candidate.member().getId()))
                .map(MemberCandidate::member)
                .findFirst()
                .orElse(null);

        if (selected == null) {
            return null;
        }

        ErmUser assignee = userRepository.findById(selected.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignee user not found"));
        ticket.setAssigneeUserId(assignee.getId());
        ticket.setAssigneeUsername(assignee.getUsername());
        ticket.setAssigneeFullName(assignee.getFullName());
        ticket.setStatus(SupportTicketStatus.ASSIGNED);
        selected.setLastAssignedAt(now);
        queueMemberRepository.save(selected);
        return selected;
    }

    private void ensureQueueAccess(Long userId, Long queueId) {
        if (queueMemberRepository.findByQueueIdAndUserId(queueId, userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized for this support queue");
        }
    }

    private void ensureQueueMember(Long queueId, Long userId) {
        if (queueMemberRepository.findByQueueIdAndUserId(queueId, userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected assignee is not a member of this queue");
        }
    }

    private void ensureCanView(Long userId, ErmSupportTicket ticket) {
        if (Objects.equals(ticket.getRequesterUserId(), userId) || Objects.equals(ticket.getAssigneeUserId(), userId)) {
            return;
        }
        if (queueMemberRepository.findByQueueIdAndUserId(ticket.getQueueId(), userId).isPresent()) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this ticket");
    }

    private List<SupportTicketStatus> openStatuses() {
        return List.of(
                SupportTicketStatus.NEW,
                SupportTicketStatus.ASSIGNED,
                SupportTicketStatus.IN_PROGRESS,
                SupportTicketStatus.PENDING_EMPLOYEE,
                SupportTicketStatus.REOPENED,
                SupportTicketStatus.SECURITY_ESCALATED
        );
    }

    private SupportTicketResponse toResponse(ErmSupportTicket ticket, boolean includeComments) {
        List<SupportTicketCommentResponse> comments = includeComments
                ? commentRepository.findAllByTicketIdOrderByCreatedAtAscIdAsc(ticket.getId()).stream()
                .map(comment -> new SupportTicketCommentResponse(
                        comment.getId(),
                        comment.getActorUsername(),
                        comment.getActionType(),
                        comment.getCommentText(),
                        comment.getCreatedAt()
                ))
                .toList()
                : List.of();

        return new SupportTicketResponse(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getTicketType(),
                ticket.getCategoryCode(),
                ticket.getCategoryTitle(),
                ticket.getSubcategoryCode(),
                ticket.getSubcategoryTitle(),
                ticket.getImpactLevel(),
                ticket.getUrgencyLevel(),
                ticket.getPriorityCode(),
                ticket.getQueueId(),
                ticket.getQueueCode(),
                ticket.getQueueTitle(),
                ticket.getAssigneeUserId(),
                ticket.getAssigneeUsername(),
                ticket.getAssigneeFullName(),
                ticket.getStatus(),
                ticket.getSource(),
                ticket.getShortDescription(),
                ticket.getDescription(),
                ticket.isSecurityIncident(),
                ticket.getResponseDueAt(),
                ticket.getResolutionDueAt(),
                ticket.getFirstResponseAt(),
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
                ticket.getCreatedByUsername(),
                ticket.getUpdatedByUsername(),
                comments,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    private void recordAudit(Long ticketId, String actor, String actionType, String fromStatus, String toStatus, String details) {
        ErmSupportAuditLog auditLog = new ErmSupportAuditLog();
        auditLog.setTicketId(ticketId);
        auditLog.setActorUsername(actor);
        auditLog.setActionType(actionType);
        auditLog.setFromStatus(fromStatus);
        auditLog.setToStatus(toStatus);
        auditLog.setDetails(details);
        auditLogRepository.save(auditLog);
    }

    private void recordNotification(Long ticketId, String recipientUsername, String channel, String eventType, String messageText) {
        if (!StringUtils.hasText(recipientUsername)) {
            return;
        }
        ErmSupportNotification notification = new ErmSupportNotification();
        notification.setTicketId(ticketId);
        notification.setRecipientUsername(recipientUsername);
        notification.setChannel(channel);
        notification.setEventType(eventType);
        notification.setMessageText(messageText);
        notificationRepository.save(notification);
    }

    private String generateTicketNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int suffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "SUP-" + datePart + "-" + suffix;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record MemberCandidate(ErmSupportQueueMember member, long openTicketCount) {
    }
}
