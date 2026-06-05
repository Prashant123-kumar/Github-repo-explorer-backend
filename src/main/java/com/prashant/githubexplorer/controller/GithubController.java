package com.prashant.githubexplorer.controller;

import com.prashant.githubexplorer.config.GithubProperties;
import com.prashant.githubexplorer.dto.*;
import com.prashant.githubexplorer.service.GithubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing all GitHub Explorer endpoints.
 *
 * <p>Base path: {@code /api/github}
 *
 * <p>All calls are proxied through the backend to:
 * <ol>
 *   <li>Keep the GitHub token server-side</li>
 *   <li>Benefit from the in-memory Caffeine cache</li>
 *   <li>Return uniform error envelopes to the frontend</li>
 * </ol>
 */
@Validated
@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
@Tag(name = "GitHub Explorer", description = "Proxy endpoints for GitHub REST API v3")
public class GithubController {

    private final GithubService      githubService;
    private final GithubProperties   githubProperties;

    // ──────────────────────────────────────────────────────────────────────────
    //  User profile
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Get GitHub user profile",
            description = "Returns public profile data for the given GitHub username."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile found"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "GitHub rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{username}")
    public ResponseEntity<GithubUserDto> getUser(
            @Parameter(description = "GitHub username", example = "torvalds")
            @PathVariable
            @NotBlank(message = "Username must not be blank")
            String username) {

        return ResponseEntity.ok(githubService.getUser(username));
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Repository list (paginated + sorted)
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Get paginated public repositories",
            description = """
                    Returns a paginated, sorted slice of the user's public repositories.

                    The full repo list is fetched once from GitHub and cached for 60 s.
                    Sorting and pagination are applied in-memory, so subsequent page
                    requests within the TTL window do not hit GitHub again.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repos returned"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{username}/repos")
    public ResponseEntity<PagedResponse<RepoDto>> getRepositories(

            @Parameter(description = "GitHub username", example = "torvalds")
            @PathVariable
            @NotBlank String username,

            @Parameter(description = "Sort field: stars | name | updated | forks",
                    example = "stars")
            @RequestParam(defaultValue = "stars") String sort,

            @Parameter(description = "Page number (1-based)", example = "1")
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "Page must be >= 1") int page,

            @Parameter(description = "Items per page (1–100)", example = "10")
            @RequestParam(defaultValue = "#{@githubProperties.repos.defaultPageSize}")
            @Min(1) @Max(100) int size) {

        int cappedSize = Math.min(size, githubProperties.getRepos().getMaxPageSize());
        return ResponseEntity.ok(
                githubService.getRepositories(username, sort, page, cappedSize));
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Repository detail
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Get repository detail",
            description = "Returns expanded metadata for a single repository."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repo found"),
            @ApiResponse(responseCode = "404", description = "Repo not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{username}/repos/{repoName}")
    public ResponseEntity<RepoDetailDto> getRepositoryDetail(
            @Parameter(description = "Repo owner login", example = "torvalds")
            @PathVariable String username,

            @Parameter(description = "Repository name", example = "linux")
            @PathVariable String repoName) {

        return ResponseEntity.ok(
                githubService.getRepositoryDetail(username, repoName));
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Language statistics (bonus)
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Get language statistics",
            description = """
                    Returns the distribution of primary programming languages
                    across all of the user's public repositories.
                    Useful for rendering a language breakdown chart in the frontend.
                    """
    )
    @GetMapping("/{username}/languages")
    public ResponseEntity<LanguageStatsResponse> getLanguageStats(
            @Parameter(description = "GitHub username", example = "torvalds")
            @PathVariable String username) {

        return ResponseEntity.ok(githubService.getLanguageStats(username));
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Cache management
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Evict cache for a user",
            description = "Forces the next request for this username to bypass "
                    + "the cache and fetch fresh data from GitHub."
    )
    @DeleteMapping("/{username}/cache")
    public ResponseEntity<Void> evictUserCache(
            @PathVariable String username) {

        githubService.evictUserCache(username);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary     = "Evict cache for a specific repo",
            description = "Forces the next repo-detail request to fetch fresh data."
    )
    @DeleteMapping("/{username}/repos/{repoName}/cache")
    public ResponseEntity<Void> evictRepoCache(
            @PathVariable String username,
            @PathVariable String repoName) {

        githubService.evictRepoDetailCache(username, repoName);
        return ResponseEntity.noContent().build();
    }
}