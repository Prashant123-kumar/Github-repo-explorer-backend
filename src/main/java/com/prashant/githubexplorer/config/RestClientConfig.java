package com.prashant.githubexplorer.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Configures the RestClient bean used to call the GitHub API.
 *
 * <p>If a GitHub PAT is provided via {@code github.api.token}, it is
 * attached as a Bearer token on every request, raising the rate limit
 * from 60 to 5 000 requests per hour.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final GithubProperties githubProperties;

    @Bean
    public RestClient restClient() {

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(githubProperties.getApi().getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT,
                        "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        String token = githubProperties.getApi().getToken();

        if (StringUtils.hasText(token)) {
            log.info("GitHub PAT detected – authenticated mode (5 000 req/hr)");
            builder.defaultHeader(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + token
            );
        } else {
            log.warn("No GitHub PAT configured – unauthenticated mode (60 req/hr). "
                    + "Set github.api.token in application.properties to increase limit.");
        }

        return builder.build();
    }
}