package com.org.erm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.erm.config.GithubProperties;
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
import java.util.HexFormat;

/**
 * Receives GitHub's "issues" webhook events and syncs support ticket status accordingly.
 * This endpoint is permitAll in SecurityConfig; trust is instead established via HMAC-SHA256
 * signature verification against the shared webhook secret (github.webhook-secret).
 */
@RestController
@RequestMapping("/api/webhooks/github")
public class GithubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GithubWebhookController.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

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
            @RequestBody String rawBody) {

        if (!isSignatureValid(rawBody, signatureHeader)) {
            log.warn("Rejected GitHub webhook call with an invalid or missing signature");
            return ResponseEntity.status(401).body("Invalid signature");
        }

        if (!"issues".equals(eventType)) {
            // Ping events (webhook setup test) and any other event types are accepted but ignored.
            return ResponseEntity.ok("Ignored event type: " + eventType);
        }

        try {
            processIssuesEvent(rawBody);
        } catch (Exception ex) {
            log.error("Error processing GitHub issues webhook payload", ex);
            // Always 200 back to GitHub so it doesn't keep retrying a payload we can't process.
        }
        return ResponseEntity.ok("Processed");
    }

    private void processIssuesEvent(String rawBody) throws Exception {
        JsonNode payload = objectMapper.readTree(rawBody);
        String action = payload.path("action").asText("");
        JsonNode issue = payload.path("issue");
        if (issue.isMissingNode() || !issue.has("number")) {
            return;
        }
        int issueNumber = issue.path("number").asInt();
        String issueUrl = issue.path("html_url").asText(null);

        switch (action) {
            case "closed" -> {
                String stateReason = issue.path("state_reason").asText("completed");
                SupportTicketStatus nextStatus = "not_planned".equals(stateReason)
                        ? SupportTicketStatus.CANCELLED
                        : SupportTicketStatus.RESOLVED;
                supportTicketService.applyExternalStatusUpdate(
                        issueNumber, nextStatus, "github-webhook",
                        "GitHub issue #" + issueNumber + " was closed (" + stateReason + "). Synced from " + issueUrl);
            }
            case "reopened" -> supportTicketService.applyExternalStatusUpdate(
                    issueNumber, SupportTicketStatus.REOPENED, "github-webhook",
                    "GitHub issue #" + issueNumber + " was reopened. Synced from " + issueUrl);
            default -> {
                // opened/edited/labeled/etc. - no ticket status implication, ignore.
            }
        }
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
            return MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception ex) {
            log.error("Failed to verify GitHub webhook signature", ex);
            return false;
        }
    }
}
