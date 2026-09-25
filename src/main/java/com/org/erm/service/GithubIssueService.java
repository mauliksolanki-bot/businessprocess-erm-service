package com.org.erm.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.erm.config.GithubProperties;
import com.org.erm.model.ErmSupportTicket;
import com.org.erm.model.SupportPriority;
import com.org.erm.model.SupportTicketType;
import com.org.erm.repository.ErmSupportTicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.org.erm.event.SupportTicketCreatedEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Best-effort integration that creates a GitHub issue for every new support ticket and places it
 * on the configured Project (v2) board's "Backlog" status column. Failures here are always
 * logged and swallowed - a support ticket must never fail to be created because GitHub is
 * unreachable or misconfigured.
 */
@Service
public class GithubIssueService {

    private static final Logger log = LoggerFactory.getLogger(GithubIssueService.class);
    private static final String GITHUB_API_BASE_URL = "https://api.github.com";

    private final GithubProperties githubProperties;
    private final ErmSupportTicketRepository ticketRepository;
    private final RestClient restClient;

    @Value("${app.ui-base-url}")
    private String uiBaseUrl;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    // Cached Project v2 board coordinates - looked up once, reused afterwards.
    private volatile ProjectBoardInfo cachedProjectBoardInfo;

    // Labels confirmed to exist in the target repo - avoids re-checking on every ticket.
    private final Set<String> knownLabels = ConcurrentHashMap.newKeySet();

    public GithubIssueService(GithubProperties githubProperties,
                              ErmSupportTicketRepository ticketRepository,
                              ObjectMapper objectMapper) {
        this.githubProperties = githubProperties;
        this.ticketRepository = ticketRepository;
        this.restClient = RestClient.builder()
                .baseUrl(GITHUB_API_BASE_URL)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketCreated(SupportTicketCreatedEvent event) {
        if (!githubProperties.isUsable()) {
            log.info("GitHub integration is disabled or not configured; skipping issue creation for ticket id {}", event.ticketId());
            return;
        }
        try {
            createIssueForTicket(event.ticketId(), event.requesterFullName(), event.requesterEmployeeId());
        } catch (Exception ex) {
            log.error("Unable to sync ticket id {} to GitHub", event.ticketId(), ex);
        }
    }

    private void createIssueForTicket(Long ticketId, String requesterFullName, String requesterEmployeeId) {
        ErmSupportTicket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) {
            log.warn("Support ticket id {} not found; skipping GitHub sync", ticketId);
            return;
        }

        String title = buildTitle(ticket);
        String body = buildBody(ticket, requesterFullName, requesterEmployeeId);
        List<String> labels = List.of(
                "support-ticket",
                ticket.getTicketType().name().toLowerCase(Locale.ROOT).replace('_', '-'),
                priorityLabel(ticket.getPriorityCode()).toLowerCase(Locale.ROOT)
        );

        ensureLabelsExist(labels);

        GithubIssueCreateResponse created;
        try {
            created = restClient.post()
                    .uri("/repos/{owner}/{repo}/issues", repositoryOwner(), repositoryName())
                    .header("Authorization", "Bearer " + githubProperties.getToken())
                    .body(new GithubIssueCreateRequest(title, body, labels))
                    .retrieve()
                    .body(GithubIssueCreateResponse.class);
        } catch (Exception ex) {
            log.warn("Issue creation with labels failed for ticket {} ({}); retrying without labels",
                    ticket.getTicketNumber(), ex.getMessage());
            try {
                created = restClient.post()
                        .uri("/repos/{owner}/{repo}/issues", repositoryOwner(), repositoryName())
                        .header("Authorization", "Bearer " + githubProperties.getToken())
                        .body(new GithubIssueCreateRequest(title, body, List.of()))
                        .retrieve()
                        .body(GithubIssueCreateResponse.class);
            } catch (Exception retryEx) {
                log.error("Failed to create GitHub issue for ticket {}", ticket.getTicketNumber(), retryEx);
                return;
            }
        }

        if (created == null) {

            log.error("GitHub issue creation for ticket {} returned an empty response", ticket.getTicketNumber());
            return;
        }

        updateTicketGithubFields(ticket.getId(), created.number(), created.htmlUrl(), created.nodeId(), null);
        log.info("Created GitHub issue #{} for support ticket {}", created.number(), ticket.getTicketNumber());

        try {
            String projectItemId = addIssueToBacklog(created.nodeId());
            if (projectItemId != null) {
                updateTicketGithubFields(ticket.getId(), null, null, null, projectItemId);
            }
        } catch (Exception ex) {
            log.error("Created GitHub issue #{} for ticket {} but failed to place it on the Project board",
                    created.number(), ticket.getTicketNumber(), ex);
        }
    }

    void updateTicketGithubFields(Long ticketId, Integer issueNumber, String issueUrl, String issueNodeId, String projectItemId) {
        ticketRepository.findById(ticketId).ifPresent(ticket -> {
            if (issueNumber != null) {
                ticket.setGithubIssueNumber(issueNumber);
            }
            if (issueUrl != null) {
                ticket.setGithubIssueUrl(issueUrl);
            }
            if (issueNodeId != null) {
                ticket.setGithubIssueNodeId(issueNodeId);
            }
            if (projectItemId != null) {
                ticket.setGithubProjectItemId(projectItemId);
            }
            ticketRepository.save(ticket);
        });
    }

    /**
     * Splits the configured "owner/repo" string so it can be substituted as two separate
     * URI template variables. Passing the full "owner/repo" string as a single template
     * variable causes Spring's URI encoding to percent-encode the slash (%2F), producing
     * a malformed path that GitHub responds to with 404 Not Found.
     */
    private String repositoryOwner() {
        return githubProperties.getRepository().split("/", 2)[0];
    }

    private String repositoryName() {
        String[] parts = githubProperties.getRepository().split("/", 2);
        return parts.length > 1 ? parts[1] : "";
    }

    /**
     * GitHub rejects issue creation with 422 Unprocessable Content if a requested label does not
     * already exist in the repository. Create any missing labels first (best-effort, ignoring
     * failures) so ticket sync keeps working without requiring manual label setup per repo.
     */
    private void ensureLabelsExist(List<String> labels) {
        for (String label : labels) {
            if (knownLabels.contains(label)) {
                continue;
            }
            try {
                restClient.post()
                        .uri("/repos/{owner}/{repo}/labels", repositoryOwner(), repositoryName())
                        .header("Authorization", "Bearer " + githubProperties.getToken())
                        .body(new GithubLabelCreateRequest(label, "ededed"))
                        .retrieve()
                        .toBodilessEntity();
                log.info("Created missing GitHub label '{}' on {}", label, githubProperties.getRepository());
            } catch (Exception ex) {
                // 422/"already_exists" simply means the label is already there - either way, treat
                // the label as usable from now on so we don't keep retrying on every ticket.
                log.debug("Could not create GitHub label '{}' (likely already exists): {}", label, ex.getMessage());
            }
            knownLabels.add(label);
        }
    }

    private String addIssueToBacklog(String issueNodeId) {
        ProjectBoardInfo boardInfo = resolveProjectBoardInfo();
        if (boardInfo == null) {
            return null;
        }

        JsonNode addResult = graphQl(
                """
                mutation($projectId: ID!, $contentId: ID!) {
                  addProjectV2ItemById(input: { projectId: $projectId, contentId: $contentId }) {
                    item { id }
                  }
                }
                """,
                Map.of("projectId", boardInfo.projectId(), "contentId", issueNodeId)
        );
        String itemId = addResult.path("data").path("addProjectV2ItemById").path("item").path("id").asText(null);
        if (!StringUtils.hasText(itemId)) {
            log.error("GitHub GraphQL addProjectV2ItemById did not return an item id: {}", addResult);
            return null;
        }

        graphQl(
                """
                mutation($projectId: ID!, $itemId: ID!, $fieldId: ID!, $optionId: String!) {
                  updateProjectV2ItemFieldValue(input: {
                    projectId: $projectId,
                    itemId: $itemId,
                    fieldId: $fieldId,
                    value: { singleSelectOptionId: $optionId }
                  }) {
                    projectV2Item { id }
                  }
                }
                """,
                Map.of(
                        "projectId", boardInfo.projectId(),
                        "itemId", itemId,
                        "fieldId", boardInfo.statusFieldId(),
                        "optionId", boardInfo.backlogOptionId()
                )
        );
        return itemId;
    }

    private ProjectBoardInfo resolveProjectBoardInfo() {
        if (cachedProjectBoardInfo != null) {
            return cachedProjectBoardInfo;
        }
        synchronized (this) {
            if (cachedProjectBoardInfo != null) {
                return cachedProjectBoardInfo;
            }
            JsonNode result = graphQl(
                    """
                    query($login: String!, $number: Int!) {
                      user(login: $login) {
                        projectV2(number: $number) {
                          id
                          field(name: "Status") {
                            ... on ProjectV2SingleSelectField {
                              id
                              options { id name }
                            }
                          }
                        }
                      }
                    }
                    """,
                    Map.of("login", githubProperties.getProjectOwner(), "number", githubProperties.getProjectNumber())
            );

            JsonNode projectNode = result.path("data").path("user").path("projectV2");
            String projectId = projectNode.path("id").asText(null);
            String statusFieldId = projectNode.path("field").path("id").asText(null);
            if (!StringUtils.hasText(projectId) || !StringUtils.hasText(statusFieldId)) {
                log.error("Unable to resolve GitHub Project #{} for owner {}: {}",
                        githubProperties.getProjectNumber(), githubProperties.getProjectOwner(), result);
                return null;
            }

            String backlogOptionId = null;
            for (JsonNode option : projectNode.path("field").path("options")) {
                if (githubProperties.getBacklogStatusName().equalsIgnoreCase(option.path("name").asText())) {
                    backlogOptionId = option.path("id").asText(null);
                    break;
                }
            }
            if (!StringUtils.hasText(backlogOptionId)) {
                log.error("Unable to find a Status option named '{}' on GitHub Project #{}",
                        githubProperties.getBacklogStatusName(), githubProperties.getProjectNumber());
                return null;
            }

            cachedProjectBoardInfo = new ProjectBoardInfo(projectId, statusFieldId, backlogOptionId);
            return cachedProjectBoardInfo;
        }
    }

    private JsonNode graphQl(String query, Map<String, Object> variables) {
        return restClient.post()
                .uri("/graphql")
                .header("Authorization", "Bearer " + githubProperties.getToken())
                .body(new GithubGraphQlRequest(query, variables))
                .retrieve()
                .body(JsonNode.class);
    }

    private String buildTitle(ErmSupportTicket ticket) {
        return "[" + tagFor(ticket.getTicketType()) + "][" + priorityLabel(ticket.getPriorityCode()) + "] " + ticket.getShortDescription();
    }

    private String buildBody(ErmSupportTicket ticket, String requesterFullName, String requesterEmployeeId) {
        String priorityTitleCase = titleCase(priorityLabel(ticket.getPriorityCode()));
        String applicationTicketUrl = uiBaseUrl.replaceAll("/+$", "") + "/support/" + ticket.getTicketNumber();

        return "Support Ticket: " + ticket.getTicketNumber() + "\n\n" +
                "Reported By: " + requesterFullName + "\n" +
                "Employee ID: " + (StringUtils.hasText(requesterEmployeeId) ? requesterEmployeeId : "Not available") + "\n" +
                "Category: " + ticket.getCategoryTitle() + "\n" +
                "Priority: " + priorityTitleCase + "\n\n" +
                "Description:\n" + ticket.getDescription() + "\n\n" +
                "Environment: " + environmentLabel() + "\n\n" +
                "Application Ticket:\n" + applicationTicketUrl;
    }

    private String environmentLabel() {
        if (activeProfile == null || activeProfile.isBlank()) {
            return "Development";
        }
        String normalized = activeProfile.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("production")) {
            return "Production";
        }
        if (normalized.contains("sit")) {
            return "SIT";
        }
        return "Development";
    }

    private String tagFor(SupportTicketType ticketType) {
        return switch (ticketType) {
            case SUPPORT_TICKET -> "SUPPORT";
            case INCIDENT -> "INCIDENT";
            case SECURITY_INCIDENT -> "SECURITY";
        };
    }

    private String priorityLabel(SupportPriority priority) {
        return switch (priority) {
            case P1 -> "CRITICAL";
            case P2 -> "HIGH";
            case P3 -> "MEDIUM";
            case P4 -> "LOW";
        };
    }

    private String titleCase(String allCapsWord) {
        if (allCapsWord.isEmpty()) {
            return allCapsWord;
        }
        return allCapsWord.charAt(0) + allCapsWord.substring(1).toLowerCase(Locale.ROOT);
    }

    private record ProjectBoardInfo(String projectId, String statusFieldId, String backlogOptionId) {
    }

    private record GithubIssueCreateRequest(String title, String body, List<String> labels) {
    }

    private record GithubLabelCreateRequest(String name, String color) {
    }

    private record GithubIssueCreateResponse(
            int number,
            @JsonProperty("html_url") String htmlUrl,
            @JsonProperty("node_id") String nodeId
    ) {
    }

    private record GithubGraphQlRequest(String query, Map<String, Object> variables) {
    }
}
