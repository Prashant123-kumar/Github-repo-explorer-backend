package com.prashant.githubexplorer.advice;

import com.prashant.githubexplorer.dto.ErrorResponse;
import com.prashant.githubexplorer.exception.GithubApiException;
import com.prashant.githubexplorer.exception.GithubRateLimitException;
import com.prashant.githubexplorer.exception.GithubUserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Centralised exception → HTTP response mapping.
 *
 * <p>Every handler returns the same {@link ErrorResponse} envelope so the
 * frontend can rely on a predictable shape regardless of error type.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 404 Not Found ────────────────────────────────────────────────────────

    @ExceptionHandler(GithubUserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            GithubUserNotFoundException ex, HttpServletRequest req) {

        log.warn("User not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), req);
    }

    // ── 429 Too Many Requests ────────────────────────────────────────────────

    @ExceptionHandler(GithubRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(
            GithubRateLimitException ex, HttpServletRequest req) {

        log.warn("GitHub rate limit hit: {}", ex.getMessage());
        return build(HttpStatus.TOO_MANY_REQUESTS,
                "Rate Limit Exceeded", ex.getMessage(), req);
    }

    // ── 502 Bad Gateway ──────────────────────────────────────────────────────

    @ExceptionHandler(GithubApiException.class)
    public ResponseEntity<ErrorResponse> handleGithubApiError(
            GithubApiException ex, HttpServletRequest req) {

        log.error("GitHub API error: {}", ex.getMessage(), ex);
        return build(HttpStatus.BAD_GATEWAY,
                "GitHub API Error", ex.getMessage(), req);
    }

    // ── 400 Bad Request (validation) ─────────────────────────────────────────

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest req) {

        String message = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(ex.getMessage());

        log.warn("Validation error: {}", message);
        return build(HttpStatus.BAD_REQUEST, "Validation Error", message, req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest req) {

        String message = "Required parameter '" + ex.getParameterName()
                + "' of type " + ex.getParameterType() + " is missing.";
        log.warn("Missing request param: {}", message);
        return build(HttpStatus.BAD_REQUEST, "Bad Request", message, req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {

        String message = "Parameter '" + ex.getName() + "' has an invalid value: "
                + ex.getValue();
        log.warn("Type mismatch: {}", message);
        return build(HttpStatus.BAD_REQUEST, "Bad Request", message, req);
    }

    // ── 500 Internal Server Error (catch-all) ────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest req) {

        log.error("Unhandled exception at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                req);
    }

    // ── Builder helper ───────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String error, String message, HttpServletRequest req) {

        ErrorResponse body = new ErrorResponse(
                status.value(), error, message, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}