package com.org.erm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github")
public class GithubProperties {

    private boolean integrationEnabled = true;
    private String token = "";
    private String repository = "";
    private String projectOwner = "";
    private int projectNumber = 1;
    private String backlogStatusName = "Backlog";
    private String webhookSecret = "";

    public boolean isIntegrationEnabled() {
        return integrationEnabled;
    }

    public void setIntegrationEnabled(boolean integrationEnabled) {
        this.integrationEnabled = integrationEnabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getProjectOwner() {
        return projectOwner;
    }

    public void setProjectOwner(String projectOwner) {
        this.projectOwner = projectOwner;
    }

    public int getProjectNumber() {
        return projectNumber;
    }

    public void setProjectNumber(int projectNumber) {
        this.projectNumber = projectNumber;
    }

    public String getBacklogStatusName() {
        return backlogStatusName;
    }

    public void setBacklogStatusName(String backlogStatusName) {
        this.backlogStatusName = backlogStatusName;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    /**
     * Integration is only usable when explicitly enabled and a token has been configured.
     */
    public boolean isUsable() {
        return integrationEnabled && token != null && !token.isBlank() && repository != null && !repository.isBlank();
    }
}
