package com.prashant.githubexplorer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class GithubUserNotFoundException extends RuntimeException {

    public GithubUserNotFoundException(String username) {
        super("GitHub user not found: '" + username + "'");
    }
}