package com.prashant.githubexplorer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class GithubApiException extends RuntimeException {

    public GithubApiException(String message) {
        super(message);
    }

    public GithubApiException(String message, Throwable cause) {
        super(message, cause);
    }
}