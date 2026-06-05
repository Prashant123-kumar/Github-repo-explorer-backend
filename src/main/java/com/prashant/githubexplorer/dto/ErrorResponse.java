package com.prashant.githubexplorer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Uniform error envelope returned by the global exception handler.
 */
@Getter
public class ErrorResponse {

    private final int status;
    private final String error;
    private final String message;
    private final String path;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;

    public ErrorResponse(int status, String error,
                         String message, String path) {
        this.status    = status;
        this.error     = error;
        this.message   = message;
        this.path      = path;
        this.timestamp = LocalDateTime.now();
    }
}