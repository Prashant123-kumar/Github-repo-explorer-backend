package com.prashant.githubexplorer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Maps directly from the GitHub /users/{username} response.
 * Only fields consumed by the frontend are declared.
 */
@Data
public class GithubUserDto {

    private String login;
    private String name;
    private String bio;
    private String email;
    private String location;
    private String company;
    private String blog;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    @JsonProperty("html_url")
    private String htmlUrl;

    private int followers;
    private int following;

    @JsonProperty("public_repos")
    private int publicRepos;

    @JsonProperty("public_gists")
    private int publicGists;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;
}