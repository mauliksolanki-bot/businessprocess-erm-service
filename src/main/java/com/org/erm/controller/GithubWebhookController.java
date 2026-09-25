package com.org.erm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.erm.config.GithubProperties;
import com.org.erm.model.SupportPriority;
import com.org.erm.model.SupportTicketStatus;
import com.org.erm.service.SupportTicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Receives verified GitHub issue and issue-comment webhooks. */
@RestController
@RequestMapping("/api/webhooks/github")
public class GithubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GithubWebhookController.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Pattern PRIORITY_PATTERN = Pattern.compile("(?i)(?:^|[^a-z0-9])(p[1-4]|critical|urgent|high|medium|low)(?:$|[^a-z0-9])");
    private static final Pattern BODY_PRIORITY_PATTERN = Pattern.compile("(?im)^\\s*priority\\s*:\\s*(p[1-4]|critical|urgent|high|medium|low)\\s*$");

    private final GithubProperties githubProperties;
    private final SupportTicketService supportTicketService;
    private final ObjectMapper objectMapper;

    public GithubWebhookController(GithubProperties githubProperties,
                                   SupportTicketService supportTicketService,
                                   ObjectMapper objectMapper) {
        this.githubProperties = githubProperties;
        this.supportTicketService = supportTicketService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody String rawBody) {

        if (!isSignatureValid(rawBody, signatureHeader)) {
            log.warn("Rejected GitHub webhook call with an invalid or missing signature");
            return ResponseEntity.status(401).body("Invalid signature");
        }
        if (!"issues".equals(eventType) && !"issue_comment".equals(eventType)) {
            return ResponseEntity.ok("Ignored event type: " + eventType);
        }

        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            log.info("Received GitHub webhook event={} action={} delivery={}", eventType,
                    payload.path("action").asText("unknown"), deliveryId);
            if ("issues".equals(eventType)) {
                processIssueEvent(payload, deliveryId);
            } else if (!processIssueCommentEvent(payload, deliveryId)) {
                return ResponseEntity.status(503).body("Support ticket is not linked to this GitHub issue yet");
            }
        } catch (Exception ex) {
            log.error("Error processing GitHub {} webhook payload", eventType, ex);
            return ResponseEntity.status(500).body("Unable to process GitHub webhook");
        }
        return ResponseEntity.ok("Processed");
    }

    private void processIssueEvent(JsonNode payload, String deliveryId) {
        JsonNode issue = payload.path("issue");
        if (!issue.has("number")) {
            return;
        }
        String action = payload.path("action").asText("unknown");
        int issueNumber = issue.path("number").asInt();
        String actor = payload.path("sender").path("login").asText("github-webhook");
        SupportTicketStatus status = switch (action) {
            case "closed" -> "not_planned".equals(issue.path("state_reason").asText())
                    ? SupportTicketStatus.CANCELLED : SupportTicketStatus.RESOLVED;
            case "reopened" -> SupportTicketStatus.REOPENED;
            default -> null;
        };
        SupportPriority priority = resolvePriority(issue);
        String details = describeIssueAction(action, issue, payload.path("changes"), payload);
        supportTicketService.applyExternalIssueEvent(issueNumber, deliveryId, actor, action, details, status, priority);
    }

    private boolean processIssueCommentEvent(JsonNode payload, String deliveryId) {
        JsonNode issue = payload.path("issue");
        JsonNode comment = payload.path("comment");
        if (!issue.has("number") || comment.isMissingNode()) {
            log.warn("Ignoring malformed GitHub issue_comment webhook delivery {}", deliveryId);
            return true;
        }
        return supportTicketService.applyExternalIssueComment(
                issue.path("number").asInt(),
                deliveryId,
                comment.path("id").isNumber() ? comment.path("id").asLong() : null,
                comment.path("user").path("login").asText("github-webhook"),
                payload.path("action").asText("created"),
                comment.path("body").asText("")
        );
    }

    private SupportPriority resolvePriority(JsonNode issue) {
        for (JsonNode label : issue.path("labels")) {
            String labelName = label.path("name").asText("");
            SupportPriority priority = parsePriority(labelName);
            if (priority != null) {
                return priority;
            }
        }
        SupportPriority fromTitle = parsePriority(issue.path("title").asText(""));
        if (fromTitle != null) {
            return fromTitle;
        }
        Matcher bodyPriority = BODY_PRIORITY_PATTERN.matcher(issue.path("body").asText(""));
        return bodyPriority.find() ? parsePriority(bodyPriority.group(1)) : null;
    }

    private SupportPriority parsePriority(String value) {
        Matcher matcher = PRIORITY_PATTERN.matcher(value.toLowerCase(Locale.ROOT).replace('_', ' '));
        if (!matcher.find()) {
            return null;
        }
        return switch (matcher.group(1).toLowerCase(Locale.ROOT)) {
            case "p1", "critical", "urgent" -> SupportPriority.P1;
            case "p2", "high" -> SupportPriority.P2;
            case "p3", "medium" -> SupportPriority.P3;
            case "p4", "low" -> SupportPriority.P4;
            default -> null;
        };
    }

    private String describeIssueAction(String action, JsonNode issue, JsonNode changes, JsonNode payload) {
        List<String> details = new ArrayList<>();
        String actor = payload.path("sender").path("login").asText(null);
        if (actor != null && !actor.isBlank()) {
            details.add("GitHub user: " + actor);
        }
        switch (action) {
            case "closed" -> details.add("Issue closed" + (issue.hasNonNull("state_reason")
                    ? " (" + issue.path("state_reason").asText() + ")" : ""));
            case "reopened" -> details.add("Issue reopened");
            case "opened" -> details.add("Issue opened");
            case "labeled", "unlabeled" -> details.add("Label " + action + ": "
                    + payload.path("label").path("name").asText(issue.path("labels").toString()));
            case "assigned", "unassigned" -> details.add("Assignee " + action + ": "
                    + payload.path("assignee").path("login").asText(issue.path("assignees").toString()));
            case "milestoned", "demilestoned" -> details.add("Milestone " + action + ": "
                    + payload.path("milestone").path("title").asText(issue.path("milestone").path("title").asText("-")));
            case "locked", "unlocked", "pinned", "unpinned", "transferred" ->
                    details.add("Issue " + action);
            default -> details.add("Issue action: " + action);
        }
        if (changes.isObject()) {
            changes.fields().forEachRemaining(entry -> {
                JsonNode oldValue = entry.getValue().path("from");
                JsonNode newValue = issue.path(entry.getKey());
                details.add(entry.getKey() + " changed from " + oldValue.asText(oldValue.toString())
                        + " to " + newValue.asText(newValue.toString()));
            });
        }
        if (issue.hasNonNull("html_url")) {
            details.add(issue.path("html_url").asText());
        }
        return String.join("; ", details);
    }

    private boolean isSignatureValid(String rawBody, String signatureHeader) {
        String secret = githubProperties.getWebhookSecret();
        if (secret == null || secret.isBlank() || signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedHex = "sha256=" + HexFormat.of().formatHex(computed);
            return MessageDigest.isEqual(computedHex.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            log.error("Failed to verify GitHub webhook signature", ex);
            return false;
        }
    }
}
