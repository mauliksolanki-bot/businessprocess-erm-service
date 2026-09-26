package com.org.erm.service;

import com.org.erm.dto.response.SupportCategoryOptionResponse;
import com.org.erm.dto.response.SupportCatalogResponse;
import com.org.erm.dto.response.SupportAssigneeOptionResponse;
import com.org.erm.dto.response.SupportQueueSummaryResponse;
import com.org.erm.dto.request.SupportTicketAssignRequest;
import com.org.erm.dto.request.SupportTicketCommentRequest;
import com.org.erm.dto.response.SupportTicketCommentResponse;
import com.org.erm.dto.request.SupportTicketCreateRequest;
import com.org.erm.dto.request.SupportTicketDetailsUpdateRequest;
import com.org.erm.dto.response.SupportTicketResponse;
import com.org.erm.dto.request.SupportTicketStatusRequest;
import com.org.erm.event.SupportTicketCreatedEvent;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class SupportTicketService {

    private static final Logger log = LoggerFactory.getLogger(SupportTicketService.class);

    private static final String QUEUE_CODE_APP_SUPPORT = "ERM_APP_SUPPORT";
    private static final String QUEUE_CODE_IT_SUPPORT = "ERM_IT_SUPPORT";
    private static final String ROLE_APPLICATION_SUPPORT_SPECIALIST = "Application Support Specialist";
    private static final String ROLE_IT_SECURITY = "IT Security";
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
    private final MentionNotificationService mentionNotificationService;
    private final ApplicationEventPublisher eventPublisher;

    public SupportTicketService(ErmSupportTicketRepository ticketRepository,
                                ErmSupportTicketCommentRepository commentRepository,
                                ErmSupportCategoryRepository categoryRepository,
                                ErmSupportQueueRepository queueRepository,
                                ErmSupportQueueMemberRepository queueMemberRepository,
                                ErmSupportPriorityMatrixRepository priorityMatrixRepository,
                                ErmSupportSlaPolicyRepository slaPolicyRepository,
                                ErmSupportAuditLogRepository auditLogRepository,
                                ErmSupportNotificationRepository notificationRepository,
                                ErmUserRepository userRepository,
                                MentionNotificationService mentionNotificationService,
                                ApplicationEventPublisher eventPublisher) {
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
        this.mentionNotificationService = mentionNotificationService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SupportTicketResponse create(SupportTicketCreateRequest request, Authentication authentication) {
        try {
            ErmUser requester = loadCurrentUser(authentication);
            SupportTicketType ticketType = parseTicketType(request.ticketType());
            String queueCode = queueCodeFor(ticketType);
            ErmSupportCategory category = loadCategory(request.categoryCode(), ticketType);
            ErmSupportQueue queue = loadQueue(queueCode);
            SupportPriority priority = derivePriority(request.impactLevel(), request.urgencyLevel(), ticketType);
            LocalDateTime now = LocalDateTime.now();

            ErmSupportTicket ticket = new ErmSupportTicket();
            ticket.setTicketNumber(generateTicketNumber(ticketType));
            ticket.setRequesterUserId(requester.getId());
            ticket.setRequesterEmployeeId(requester.getEmployeeId());
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

            ErmSupportQueueMember assignedMember = null;
            // if client requested a specific assignee, honor it (validate membership)
            if (request.assigneeUserId() != null) {
                ensureQueueMember(queue.getId(), request.assigneeUserId());
                ErmUser assignee = userRepository.findById(request.assigneeUserId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignee not found"));
                ticket.setAssigneeUserId(assignee.getId());
                ticket.setAssigneeEmployeeId(assignee.getEmployeeId());
                ticket.setAssigneeUsername(assignee.getUsername());
                ticket.setAssigneeFullName(assignee.getFullName());
                ticket.setStatus(SupportTicketStatus.ASSIGNED);
                // update queue member last assigned
                assignedMember = queueMemberRepository.findByQueueIdAndUserIdAndActiveTrue(queue.getId(), assignee.getId()).orElse(null);
                if (assignedMember != null) {
                    assignedMember.setLastAssignedAt(now);
                    queueMemberRepository.save(assignedMember);
                }
            } else {
                assignedMember = autoAssign(ticket, queue, now);
            }

            ticket = ticketRepository.save(ticket);
            eventPublisher.publishEvent(new SupportTicketCreatedEvent(ticket.getId(), requester.getFullName(), requester.getEmployeeId()));

            recordAudit(ticket.getId(), requester.getUsername(), "CREATED", null, ticket.getStatus().name(), "Support ticket created");
            recordNotification(ticket.getId(), requester.getUsername(), "IN_APP", "CREATED", "Ticket " + ticket.getTicketNumber() + " created");
            if (assignedMember != null && ticket.getAssigneeUsername() != null) {
                recordNotification(ticket.getId(), ticket.getAssigneeUsername(), "IN_APP", "ASSIGNED", "Ticket " + ticket.getTicketNumber() + " assigned to you");
            }

            return toResponse(ticket, true);
        } catch (ResponseStatusException ex) {
            // Known client-facing errors — rethrow to allow proper HTTP mapping
            throw ex;
        } catch (Exception ex) {
            // Log unexpected server errors to aid debugging and rethrow as 500
            org.slf4j.LoggerFactory.getLogger(SupportTicketService.class).error("Error creating support ticket: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create support ticket");
        }
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> list(String scope, String queueCode, String status, Authentication authentication) {
        ErmUser currentUser = loadCurrentUser(authentication);
        List<ErmSupportTicket> tickets;
        String normalizedScope = StringUtils.hasText(scope) ? scope.trim().toLowerCase(Locale.ROOT) : "mine";

        if ("mine".equals(normalizedScope)) {
            tickets = ticketRepository.findAllByRequesterUserIdOrderByCreatedAtDesc(currentUser.getId());
        } else if ("assigned".equals(normalizedScope)) {
            ensureAssignedScopeAccess(authentication);
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

    @Transactional(readOnly = true)
    public SupportTicketResponse getByTicketNumber(String ticketNumber, Authentication authentication) {
        ErmSupportTicket ticket = ticketRepository.findByTicketNumber(normalizeRequired(ticketNumber, "Ticket number is required"))
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
        mentionNotificationService.notifyMentions(
                currentUser.getUsername(),
                comment.getCommentText(),
                "SUPPORT_TICKET",
                ticket.getId(),
                currentUser.getUsername() + " mentioned you on support ticket " + ticket.getTicketNumber(),
                "/support/ticket/" + ticket.getTicketNumber()
        );
        return toResponse(ticket, true);
    }

    @Transactional
    public SupportTicketResponse updateStatus(Long ticketId, SupportTicketStatusRequest request, Authentication authentication) {
        ErmSupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support ticket not found"));
        ErmUser currentUser = loadCurrentUser(authentication);
        ensureCanView(currentUser.getId(), ticket);
        ensureCanUpdateStatus(currentUser, ticket);

        SupportTicketStatus nextStatus = request.status();
        SupportTicketStatus previousStatus = ticket.getStatus();
        LocalDateTime now = LocalDateTime.now();
        if (isClosureStatus(nextStatus) && !StringUtils.hasText(request.comment())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Closure details are required when state is RESOLVED or CLOSED");
        }

        applyStatusTransition(ticket, nextStatus, now);
        ticket.setUpdatedByUsername(currentUser.getUsername());
        ticket = ticketRepository.save(ticket);
        if (StringUtils.hasText(request.comment())) {
            ErmSupportTicketComment comment = new ErmSupportTicketComment();
            comment.setTicketId(ticket.getId());
            comment.setActorUsername(currentUser.getUsername());
            comment.setActionType("STATUS_CHANGE");
            comment.setCommentText(normalizeRequired(request.comment(), "Comment is required"));
            commentRepository.save(comment);
            mentionNotificationService.notifyMentions(
                    currentUser.getUsername(),
                    comment.getCommentText(),
                    "SUPPORT_TICKET",
                    ticket.getId(),
                    currentUser.getUsername() + " mentioned you on support ticket " + ticket.getTicketNumber(),
                    "/support/ticket/" + ticket.getTicketNumber()
            );
        }
        recordAudit(ticket.getId(), currentUser.getUsername(), "STATUS_CHANGE", previousStatus.name(), nextStatus.name(), "Status changed");
        recordNotification(ticket.getId(), ticket.getRequesterUsername(), "IN_APP", "STATUS_CHANGE", "Ticket " + ticket.getTicketNumber() + " moved to " + nextStatus.name());
        return toResponse(ticket, true);
    }

    /** Applies an issue action received from a verified external integration. */
    @Transactional
    public void applyExternalIssueEvent(int githubIssueNumber, String deliveryId, String actorLabel, String actionType, String details,
                                        SupportTicketStatus nextStatus, SupportPriority nextPriority) {
        ticketRepository.findByGithubIssueNumber(githubIssueNumber).ifPresent(ticket -> {
            if (StringUtils.hasText(deliveryId) && commentRepository.existsByGithubDeliveryId(deliveryId)) {
                return;
            }
            String actor = StringUtils.hasText(actorLabel) ? actorLabel : "github-webhook";
            String previousStatus = ticket.getStatus().name();
            String previousPriority = ticket.getPriorityCode().name();
            LocalDateTime now = LocalDateTime.now();
            boolean statusChanged = nextStatus != null && ticket.getStatus() != nextStatus;
            boolean priorityChanged = nextPriority != null && ticket.getPriorityCode() != nextPriority;

            if (statusChanged) {
                applyStatusTransition(ticket, nextStatus, now);
            }
            if (priorityChanged) {
                ticket.setPriorityCode(nextPriority);
                applySla(ticket, ticket.getQueueCode(), ticket.getTicketType().name(), nextPriority.name(), now);
            }
            ticket.setUpdatedByUsername(actor);
            ticketRepository.save(ticket);

            saveExternalActivity(ticket.getId(), actor, "GITHUB_" + actionType.toUpperCase(Locale.ROOT), details, null, deliveryId);
            String auditDetails = details == null ? "GitHub issue action: " + actionType : details;
            if (priorityChanged) {
                auditDetails += " (priority " + previousPriority + " -> " + nextPriority.name() + ")";
            }
            recordAudit(ticket.getId(), actor, "GITHUB_" + actionType.toUpperCase(Locale.ROOT),
                    statusChanged ? previousStatus : null, statusChanged ? nextStatus.name() : null,
                    auditDetails.length() > 1000 ? auditDetails.substring(0, 1000) : auditDetails);

            if (statusChanged) {
                recordNotification(ticket.getId(), ticket.getRequesterUsername(), "IN_APP", "STATUS_CHANGE",
                        "Ticket " + ticket.getTicketNumber() + " moved to " + nextStatus.name());
            }
        });
    }

    /** Stores or updates a GitHub issue comment, using its id to make webhook retries idempotent. */
    @Transactional
    public boolean applyExternalIssueComment(int githubIssueNumber, String deliveryId, Long githubCommentId, String actorLabel,
                                             String action, String body) {
        ErmSupportTicket ticket = ticketRepository.findByGithubIssueNumber(githubIssueNumber).orElse(null);
        if (ticket == null) {
            log.warn("Received GitHub issue comment for unlinked issue #{} (delivery {})", githubIssueNumber, deliveryId);
            return false;
        }
        String actor = StringUtils.hasText(ticket.getAssigneeUsername())
                ? ticket.getAssigneeFullName()
                : (StringUtils.hasText(actorLabel) ? actorLabel : "github-webhook");
        if (StringUtils.hasText(deliveryId) && commentRepository.existsByGithubDeliveryId(deliveryId)) {
            return true;
        }
        ticket.setUpdatedByUsername(actor);
        ticketRepository.save(ticket);
        ErmSupportTicketComment comment = githubCommentId == null ? null
                : commentRepository.findByGithubCommentId(githubCommentId).orElse(null);

        if ("deleted".equalsIgnoreCase(action)) {
            saveExternalActivity(ticket.getId(), actor, "GITHUB_COMMENT_DELETED",
                    "GitHub comment deleted" + (StringUtils.hasText(body) ? ": " + body : ""), null, deliveryId);
            recordAudit(ticket.getId(), actor, "GITHUB_COMMENT_DELETED", null, null, "GitHub comment deleted");
            return true;
        }

        if (comment == null) {
            comment = new ErmSupportTicketComment();
            comment.setTicketId(ticket.getId());
            comment.setGithubCommentId(githubCommentId);
        }
        comment.setActorUsername(actor);
        comment.setActionType("GITHUB_COMMENT");
        comment.setCommentText(body);
        if ("edited".equalsIgnoreCase(action)) {
            saveExternalActivity(ticket.getId(), actor, "GITHUB_COMMENT_EDITED", "GitHub comment edited",
                    null, deliveryId);
        } else {
            comment.setGithubDeliveryId(deliveryId);
        }
        commentRepository.save(comment);
        recordAudit(ticket.getId(), actor, "GITHUB_COMMENT", null, null,
                "GitHub issue comment " + action);
        return true;
    }

    private void saveExternalActivity(Long ticketId, String actor, String actionType, String details, Long githubCommentId,
                                     String deliveryId) {
        ErmSupportTicketComment activity = new ErmSupportTicketComment();
        activity.setTicketId(ticketId);
        activity.setActorUsername(actor);
        activity.setActionType(actionType.length() > 50 ? actionType.substring(0, 50) : actionType);
        activity.setCommentText(details);
        activity.setGithubCommentId(githubCommentId);
        activity.setGithubDeliveryId(deliveryId);
        commentRepository.save(activity);
    }

    @Transactional
    public SupportTicketResponse updateDetails(Long ticketId, SupportTicketDetailsUpdateRequest request, Authentication authentication) {
        ErmSupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support ticket not found"));
        ErmUser currentUser = loadCurrentUser(authentication);
        ensureCanEditTicketDetails(currentUser, ticket);

        LocalDateTime now = LocalDateTime.now();
        List<String> changeLogs = new ArrayList<>();
        SupportTicketStatus previousStatus = ticket.getStatus();
        boolean lockedForDetails = isClosureStatus(previousStatus);

        ErmSupportQueue activeQueue = queueRepository.findById(ticket.getQueueId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Support queue not found"));

        boolean queueChanged = StringUtils.hasText(request.queueCode()) && !Objects.equals(ticket.getQueueCode(), request.queueCode().trim());
        boolean assigneeChanged = request.assigneeUserId() != null && !Objects.equals(ticket.getAssigneeUserId(), request.assigneeUserId());
        String normalizedImpact = StringUtils.hasText(request.impactLevel()) ? normalizeRequired(request.impactLevel(), "Impact is required") : null;
        String normalizedUrgency = StringUtils.hasText(request.urgencyLevel()) ? normalizeRequired(request.urgencyLevel(), "Urgency is required") : null;
        boolean impactChanged = normalizedImpact != null && !Objects.equals(ticket.getImpactLevel(), normalizedImpact);
        boolean urgencyChanged = normalizedUrgency != null && !Objects.equals(ticket.getUrgencyLevel(), normalizedUrgency);
        boolean closureDetailsProvided = StringUtils.hasText(request.closureDetails());

        if (lockedForDetails && (queueChanged || assigneeChanged || impactChanged || urgencyChanged || closureDetailsProvided)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only State can be changed when ticket is RESOLVED or CLOSED");
        }

        if (StringUtils.hasText(request.queueCode())) {
            ErmSupportQueue nextQueue = loadQueue(request.queueCode());
            if (!Objects.equals(ticket.getQueueId(), nextQueue.getId())) {
                changeLogs.add(formatChangeLine("Assignment Group", ticket.getQueueTitle(), nextQueue.getQueueTitle()));
                ticket.setQueueId(nextQueue.getId());
                ticket.setQueueCode(nextQueue.getQueueCode());
                ticket.setQueueTitle(nextQueue.getQueueTitle());
                activeQueue = nextQueue;
            }
        }

        if (request.assigneeUserId() != null && !Objects.equals(ticket.getAssigneeUserId(), request.assigneeUserId())) {
            ensureQueueMember(activeQueue.getId(), request.assigneeUserId());
            ErmUser assignee = userRepository.findById(request.assigneeUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignee not found"));

            String fromAssignee = resolveAssigneeLabel(ticket.getAssigneeFullName(), ticket.getAssigneeUsername());
            String toAssignee = resolveAssigneeLabel(assignee.getFullName(), assignee.getUsername());
            changeLogs.add(formatChangeLine("Assigned To", fromAssignee, toAssignee));

            ticket.setAssigneeUserId(assignee.getId());
            ticket.setAssigneeEmployeeId(assignee.getEmployeeId());
            ticket.setAssigneeUsername(assignee.getUsername());
            ticket.setAssigneeFullName(assignee.getFullName());
            queueMemberRepository.findByQueueIdAndUserIdAndActiveTrue(activeQueue.getId(), assignee.getId()).ifPresent(member -> {
                member.setLastAssignedAt(now);
                queueMemberRepository.save(member);
            });
        }

        if (StringUtils.hasText(request.impactLevel())) {
            if (!IMPACT_LEVELS.contains(normalizedImpact)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impact must be one of Low, Medium, High, Critical");
            }
            if (!Objects.equals(ticket.getImpactLevel(), normalizedImpact)) {
                changeLogs.add(formatChangeLine("Impact", ticket.getImpactLevel(), normalizedImpact));
                ticket.setImpactLevel(normalizedImpact);
            }
        }

        if (StringUtils.hasText(request.urgencyLevel())) {
            if (!URGENCY_LEVELS.contains(normalizedUrgency)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Urgency must be one of Low, Medium, High, Critical");
            }
            if (!Objects.equals(ticket.getUrgencyLevel(), normalizedUrgency)) {
                changeLogs.add(formatChangeLine("Urgency", ticket.getUrgencyLevel(), normalizedUrgency));
                ticket.setUrgencyLevel(normalizedUrgency);
            }
        }

        if (StringUtils.hasText(request.impactLevel()) || StringUtils.hasText(request.urgencyLevel())) {
            SupportPriority recalculatedPriority = derivePriority(ticket.getImpactLevel(), ticket.getUrgencyLevel(), ticket.getTicketType());
            if (recalculatedPriority != ticket.getPriorityCode()) {
                changeLogs.add(formatChangeLine("Priority", ticket.getPriorityCode().name(), recalculatedPriority.name()));
            }
            ticket.setPriorityCode(recalculatedPriority);
            applySla(ticket, ticket.getQueueCode(), ticket.getTicketType().name(), recalculatedPriority.name(), now);
        }

        if (request.status() != null && request.status() != ticket.getStatus()) {
            if (isClosureStatus(request.status()) && !StringUtils.hasText(request.closureDetails())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Closure details are required when state is RESOLVED or CLOSED");
            }
            applyStatusTransition(ticket, request.status(), now);
            changeLogs.add(formatChangeLine("State", previousStatus.name(), request.status().name()));
        }

        if (StringUtils.hasText(request.closureDetails())) {
            changeLogs.add(formatChangeLine("Closure Details", "N/A", request.closureDetails().trim()));
        }

        if (changeLogs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No ticket fields were changed");
        }

        ticket.setUpdatedByUsername(currentUser.getUsername());
        ticket = ticketRepository.save(ticket);

        ErmSupportTicketComment trackingComment = new ErmSupportTicketComment();
        trackingComment.setTicketId(ticket.getId());
        trackingComment.setActorUsername(currentUser.getUsername());
        trackingComment.setActionType("DETAILS_UPDATE");
        trackingComment.setCommentText(String.join("\n", changeLogs));
        commentRepository.save(trackingComment);
        mentionNotificationService.notifyMentions(
                currentUser.getUsername(),
                trackingComment.getCommentText(),
                "SUPPORT_TICKET",
                ticket.getId(),
                currentUser.getUsername() + " mentioned you on support ticket " + ticket.getTicketNumber(),
                "/support/ticket/" + ticket.getTicketNumber()
        );

        recordAudit(ticket.getId(), currentUser.getUsername(), "DETAILS_UPDATE", previousStatus.name(), ticket.getStatus().name(), trackingComment.getCommentText());
        recordNotification(ticket.getId(), ticket.getRequesterUsername(), "IN_APP", "DETAILS_UPDATE", "Ticket " + ticket.getTicketNumber() + " details updated");
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
            ticket.setAssigneeEmployeeId(assignee.getEmployeeId());
            ticket.setAssigneeUsername(assignee.getUsername());
            ticket.setAssigneeFullName(assignee.getFullName());
            queueMemberRepository.findByQueueIdAndUserIdAndActiveTrue(queue.getId(), assignee.getId()).ifPresent(member -> {
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
                .filter(queue -> QUEUE_CODE_APP_SUPPORT.equalsIgnoreCase(queue.getQueueCode())
                        || QUEUE_CODE_IT_SUPPORT.equalsIgnoreCase(queue.getQueueCode()))
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
    public List<SupportAssigneeOptionResponse> workbenchAssignees(String queueCode, Authentication authentication) {
        ensureAssignedScopeAccess(authentication);
        List<ErmSupportQueue> queues = resolveAssigneeLookupQueues(queueCode);
        return mapQueueAssignees(queues);
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
            String normalized = normalizeRequired(value, "Ticket type is required").toUpperCase(Locale.ROOT);
            if ("SERVICE_REQUEST".equals(normalized) || "SERVICE REQUEST".equals(normalized)) {
                return SupportTicketType.SUPPORT_TICKET;
            }
            if ("INCIDENT_APPLICATION".equals(normalized) || "INCIDENT APPLICATION".equals(normalized)) {
                return SupportTicketType.INCIDENT;
            }
            if ("INCIDENT_SECURITY".equals(normalized) || "INCIDENT SECURITY".equals(normalized)) {
                return SupportTicketType.SECURITY_INCIDENT;
            }
            return SupportTicketType.valueOf(normalized);
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
            return QUEUE_CODE_IT_SUPPORT;
        }
        return QUEUE_CODE_APP_SUPPORT;
    }

    private ErmSupportCategory loadCategory(String categoryCode, SupportTicketType ticketType) {
        String normalizedCategory = normalizeRequired(categoryCode, "Category is required");
        ErmSupportCategory category = categoryRepository.findByCategoryCodeIgnoreCaseAndActiveTrue(normalizedCategory)
                .orElseGet(() -> categoryRepository.findAllByActiveTrueOrderBySortOrderAscCategoryTitleAsc().stream()
                        .filter(item -> item.getCategoryTitle().equalsIgnoreCase(normalizedCategory))
                        .findFirst()
                        .orElse(null));
        if (category == null) {
            category = categoryRepository.findByCategoryCodeIgnoreCaseAndActiveTrue(mapLegacyCategoryAlias(normalizedCategory, ticketType))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category"));
        }
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
            ticket.setAssigneeEmployeeId(null);
            ticket.setAssigneeUsername(null);
            ticket.setAssigneeFullName(null);
            return null;
        }

        ErmSupportQueueMember selected = members.stream()
                .filter(member -> userRepository.findById(member.getUserId())
                        .map(user -> user.isActive()
                                && "active".equalsIgnoreCase(user.getEmploymentStatus())
                                && isEligibleForAutoAssignment(user, queue.getQueueCode()))
                        .orElse(false))
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
        ticket.setAssigneeEmployeeId(assignee.getEmployeeId());
        ticket.setAssigneeUsername(assignee.getUsername());
        ticket.setAssigneeFullName(assignee.getFullName());
        ticket.setStatus(SupportTicketStatus.ASSIGNED);
        selected.setLastAssignedAt(now);
        queueMemberRepository.save(selected);
        return selected;
    }

    private void ensureQueueAccess(Long userId, Long queueId) {
        if (!queueMemberRepository.existsByQueueIdAndUserIdAndActiveTrue(queueId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized for this support queue");
        }
    }

    private void ensureQueueMember(Long queueId, Long userId) {
        if (!queueMemberRepository.existsByQueueIdAndUserIdAndActiveTrue(queueId, userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected assignee is not an active member of this queue");
        }
        ErmUser assignee = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignee not found"));
        if (!assignee.isActive() || !"active".equalsIgnoreCase(assignee.getEmploymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected assignee must be an active employee");
        }
    }

    private void ensureCanView(Long userId, ErmSupportTicket ticket) {
        if (Objects.equals(ticket.getRequesterUserId(), userId) || Objects.equals(ticket.getAssigneeUserId(), userId)) {
            return;
        }
        if (queueMemberRepository.findByQueueIdAndUserIdAndActiveTrue(ticket.getQueueId(), userId).isPresent()) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this ticket");
    }

    private void ensureCanUpdateStatus(ErmUser actor, ErmSupportTicket ticket) {
        if (queueMemberRepository.existsByQueueIdAndUserIdAndActiveTrue(ticket.getQueueId(), actor.getId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only active members of the ticket queue can update ticket status");
    }

    private void ensureCanEditTicketDetails(ErmUser actor, ErmSupportTicket ticket) {
        if (queueMemberRepository.existsByQueueIdAndUserIdAndActiveTrue(ticket.getQueueId(), actor.getId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only active members of the ticket queue can edit ticket details");
    }

    private void ensureAssignedScopeAccess(Authentication authentication) {
        ErmUser currentUser = loadCurrentUser(authentication);
        if (queueMemberRepository.existsByUserIdAndActiveTrue(currentUser.getId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to view assigned tickets");
    }

    private List<SupportAssigneeOptionResponse> mapQueueAssignees(List<ErmSupportQueue> queues) {
        return queues.stream()
                .flatMap(queue -> queueMemberRepository.findAllByQueueIdAndActiveTrueOrderByLastAssignedAtAscIdAsc(queue.getId()).stream())
                .map(member -> userRepository.findById(member.getUserId()).orElse(null))
                .filter(Objects::nonNull)
                .filter(ErmUser::isActive)
                .filter(user -> "active".equalsIgnoreCase(user.getEmploymentStatus()))
                .collect(Collectors.toMap(
                        ErmUser::getId,
                        user -> user,
                        (left, right) -> left
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(ErmUser::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(user -> new SupportAssigneeOptionResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName(),
                        user.getEmail()
                ))
                .toList();
    }

    private List<ErmSupportQueue> resolveAssigneeLookupQueues(String queueCode) {
        String normalized = normalizeRequired(queueCode, "Queue code is required").trim().toUpperCase(Locale.ROOT);
        List<ErmSupportQueue> queues;
        if (QUEUE_CODE_APP_SUPPORT.equalsIgnoreCase(normalized)) {
            queues = loadExistingQueues(List.of(QUEUE_CODE_APP_SUPPORT, "support", "incident"));
        } else if (QUEUE_CODE_IT_SUPPORT.equalsIgnoreCase(normalized)) {
            queues = loadExistingQueues(List.of(QUEUE_CODE_IT_SUPPORT, "security"));
        } else {
            queues = List.of(loadQueue(normalized));
        }
        if (queues.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown support queue");
        }
        return queues;
    }

    private List<ErmSupportQueue> loadExistingQueues(List<String> queueCodes) {
        return queueCodes.stream()
                .map(code -> queueRepository.findByQueueCodeIgnoreCaseAndActiveTrue(code).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean isEligibleForAutoAssignment(ErmUser user, String queueCode) {
        if (!StringUtils.hasText(queueCode)) {
            return false;
        }
        String normalizedQueueCode = queueCode.trim().toUpperCase(Locale.ROOT);
        if (QUEUE_CODE_IT_SUPPORT.equalsIgnoreCase(normalizedQueueCode) || "SECURITY".equals(normalizedQueueCode)) {
            return hasRole(user, ROLE_IT_SECURITY);
        }
        if (QUEUE_CODE_APP_SUPPORT.equalsIgnoreCase(normalizedQueueCode)
                || "SUPPORT".equals(normalizedQueueCode)
                || "INCIDENT".equals(normalizedQueueCode)) {
            return hasRole(user, ROLE_APPLICATION_SUPPORT_SPECIALIST);
        }
        return false;
    }

    private boolean hasRole(ErmUser user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equalsIgnoreCase(role.getName()));
    }

    private boolean isClosureStatus(SupportTicketStatus status) {
        return status == SupportTicketStatus.RESOLVED || status == SupportTicketStatus.CLOSED;
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
                ticket.getGithubIssueNumber() != null ? "#" + ticket.getGithubIssueNumber() : null,
                ticket.getGithubIssueUrl(),
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

    private String generateTicketNumber(SupportTicketType ticketType) {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int suffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        String prefix = switch (ticketType) {
            case SUPPORT_TICKET -> "RITM-";
            case INCIDENT -> "INC-APP-";
            case SECURITY_INCIDENT -> "INC-SEC-";
        };
        return prefix + datePart + "-" + suffix;
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

    private String mapLegacyCategoryAlias(String value, SupportTicketType ticketType) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("APPLICATION SUPPORT".equals(normalized)) {
            if (ticketType == SupportTicketType.INCIDENT) {
                return "application-issue";
            }
            if (ticketType == SupportTicketType.SECURITY_INCIDENT) {
                return "security-incident";
            }
            return "software-access";
        }
        if ("SECURITY SUPPORT".equals(normalized)) {
            return "security-incident";
        }
        if ("SERVICE REQUEST".equals(normalized)) {
            return "account-access";
        }
        return value;
    }

    private void applyStatusTransition(ErmSupportTicket ticket, SupportTicketStatus nextStatus, LocalDateTime now) {
        ticket.setStatus(nextStatus);
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
    }

    private String resolveAssigneeLabel(String fullName, String username) {
        if (StringUtils.hasText(fullName)) {
            return fullName.trim();
        }
        if (StringUtils.hasText(username)) {
            return username.trim();
        }
        return "Unassigned";
    }

    private String formatChangeLine(String fieldName, String fromValue, String newValue) {
        String safeFrom = StringUtils.hasText(fromValue) ? fromValue.trim() : "N/A";
        String safeTo = StringUtils.hasText(newValue) ? newValue.trim() : "N/A";
        return fieldName + " - " + safeFrom + " --> " + safeTo;
    }

    private record MemberCandidate(ErmSupportQueueMember member, long openTicketCount) {
    }
}
