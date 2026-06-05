package com.prashant.githubexplorer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class GithubRateLimitException extends RuntimeException {

    public GithubRateLimitException() {
        super("GitHub API rate limit exceeded. Please wait a moment and try again, "
                + "or configure a GitHub Personal Access Token to increase the limit.");
    }

    public GithubRateLimitException(String resetTime) {
        super("GitHub API rate limit exceeded. Rate limit resets at: " + resetTime);
    }
}