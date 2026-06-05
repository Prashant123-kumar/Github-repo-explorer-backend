package com.prashant.githubexplorer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Expanded repo detail shown when user clicks a repo card.
 */
@Data
public class RepoDetailDto {

    private String name;
    private String description;
    private String language;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("clone_url")
    private String cloneUrl;

    @JsonProperty("homepage")
    private String homepage;

    @JsonProperty("stargazers_count")
    private int stargazersCount;

    @JsonProperty("forks_count")
    private int forksCount;

    @JsonProperty("open_issues_count")
    private int openIssuesCount;

    @JsonProperty("watchers_count")
    private int watchersCount;

    @JsonProperty("subscribers_count")
    private int subscribersCount;

    @JsonProperty("default_branch")
    private String defaultBranch;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("pushed_at")
    private String pushedAt;

    private boolean fork;

    @JsonProperty("topics")
    private List<String> topics;

    private LicenseDto license;

    @Data
    public static class LicenseDto {
        private String key;
        private String name;
        @JsonProperty("spdx_id")
        private String spdxId;
    }
}