package com.prashant.githubexplorer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Lightweight repo record used in the paginated list response.
 */
@Data
public class RepoDto {

    private String name;
    private String description;
    private String language;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("stargazers_count")
    private int stargazersCount;

    @JsonProperty("forks_count")
    private int forksCount;

    @JsonProperty("open_issues_count")
    private int openIssuesCount;

    @JsonProperty("watchers_count")
    private int watchersCount;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("default_branch")
    private String defaultBranch;

    private boolean fork;

    @JsonProperty("is_template")
    private boolean isTemplate;

    @JsonProperty("topics")
    private java.util.List<String> topics;
}