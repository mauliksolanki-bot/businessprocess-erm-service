package com.org.erm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 defaults to Jackson 3 and no longer auto-configures a Jackson 2
 * ({@code com.fasterxml.jackson.databind.ObjectMapper}) bean. Several components in this
 * codebase (e.g. GithubWebhookController, GithubIssueService, jjwt-jackson) still depend on
 * the Jackson 2 API, so we provide the bean explicitly here.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
