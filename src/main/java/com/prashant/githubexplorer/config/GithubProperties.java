package com.prashant.githubexplorer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Strongly-typed binding of all github.* properties
 * from application.properties.
 */
@Component
@ConfigurationProperties(prefix = "github")
@Getter
@Setter
public class GithubProperties {

    private Api api = new Api();

    private Repos repos = new Repos();

    @Getter
    @Setter
    public static class Api {
        private String baseUrl = "https://api.github.com";
        /** Optional PAT – raises rate limit to 5 000 req/hr */
        private String token = "";
    }

    @Getter
    @Setter
    public static class Repos {
        private int defaultPageSize = 10;
        private int maxPageSize = 100;
    }
}