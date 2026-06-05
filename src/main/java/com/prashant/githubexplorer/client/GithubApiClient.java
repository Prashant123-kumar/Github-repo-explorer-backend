package com.prashant.githubexplorer.client;

import com.prashant.githubexplorer.dto.GithubUserDto;
import com.prashant.githubexplorer.dto.RepoDetailDto;
import com.prashant.githubexplorer.dto.RepoDto;
import com.prashant.githubexplorer.exception.GithubApiException;
import com.prashant.githubexplorer.exception.GithubRateLimitException;
import com.prashant.githubexplorer.exception.GithubUserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

/**
 * Thin HTTP client that proxies calls to the GitHub REST API v3.
 *
 * <p>All GitHub-specific HTTP errors are translated to domain exceptions
 * here so the rest of the codebase never deals with HTTP status codes.
 *
 * <p>Every method fetches the maximum allowed page size (100) so the
 * service layer can sort and paginate in-memory without extra round trips.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GithubApiClient {

    private final RestClient restClient;

    // ──────────────────────────────────────────────────────────────────────────
    //  User
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Fetches a GitHub user's public profile.
     *
     * @param username GitHub login (case-insensitive on GitHub's side)
     * @return populated {@link GithubUserDto}
     * @throws GithubUserNotFoundException if the login does not exist
     * @throws GithubRateLimitException    if the API rate limit is hit
     * @throws GithubApiException          for any other upstream error
     */
    public GithubUserDto getUser(String username) {

        log.debug("GET /users/{}", username);

        try {
            return restClient.get()
                    .uri("/users/{username}", username)
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 404,
                            (req, res) -> {
                                throw new GithubUserNotFoundException(username);
                            })
                    .onStatus(
                            status -> status.value() == 403 || status.value() == 429,
                            (req, res) -> {
                                String reset = res.getHeaders()
                                        .getFirst("X-RateLimit-Reset");
                                throw new GithubRateLimitException(
                                        reset != null ? "Unix epoch " + reset : "soon");
                            })
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            (req, res) -> {
                                throw new GithubApiException(
                                        "GitHub API returned " + res.getStatusCode()
                                                + ". Please try again later.");
                            })
                    .body(GithubUserDto.class);

        } catch (ResourceAccessException ex) {
            throw new GithubApiException(
                    "Could not reach GitHub API. Check your network connection.", ex);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Repositories
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Fetches ALL public repositories for a user.
     *
     * <p>GitHub paginates at 100 items max per page; this method walks all
     * pages automatically so the caller always gets the complete set.
     *
     * @param username GitHub login
     * @return full list of repos (may be empty for a brand-new account)
     */
    public List<RepoDto> getAllRepositories(String username) {

        log.debug("Fetching all repos for user '{}'", username);

        List<RepoDto> all = new java.util.ArrayList<>();
        int page = 1;

        while (true) {
            RepoDto[] batch = fetchRepoPage(username, page, 100);
            if (batch == null || batch.length == 0) break;
            all.addAll(Arrays.asList(batch));
            if (batch.length < 100) break;  // last page
            page++;
        }

        log.debug("Total repos fetched for '{}': {}", username, all.size());
        return all;
    }

    private RepoDto[] fetchRepoPage(String username, int page, int perPage) {
        try {
            return restClient.get()
                    .uri("/users/{username}/repos?per_page={perPage}&page={page}&type=public",
                            username, perPage, page)
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 403 || status.value() == 429,
                            (req, res) -> {
                                String reset = res.getHeaders()
                                        .getFirst("X-RateLimit-Reset");
                                throw new GithubRateLimitException(
                                        reset != null ? "Unix epoch " + reset : "soon");
                            })
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            (req, res) -> {
                                throw new GithubApiException(
                                        "GitHub API returned " + res.getStatusCode());
                            })
                    .body(RepoDto[].class);

        } catch (ResourceAccessException ex) {
            throw new GithubApiException(
                    "Could not reach GitHub API.", ex);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Repo detail
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Fetches detailed metadata for a single repository.
     *
     * @param username GitHub login (repo owner)
     * @param repoName repository name
     * @return {@link RepoDetailDto} or throws if not found
     */
    public RepoDetailDto getRepositoryDetail(String username, String repoName) {

        log.debug("GET /repos/{}/{}", username, repoName);

        try {
            return restClient.get()
                    .uri("/repos/{username}/{repoName}", username, repoName)
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 404,
                            (req, res) -> {
                                throw new GithubApiException(
                                        "Repository '" + username + "/" + repoName
                                                + "' not found.");
                            })
                    .onStatus(
                            status -> status.value() == 403 || status.value() == 429,
                            (req, res) -> {
                                throw new GithubRateLimitException();
                            })
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            (req, res) -> {
                                throw new GithubApiException(
                                        "GitHub API error: " + res.getStatusCode());
                            })
                    .body(RepoDetailDto.class);

        } catch (ResourceAccessException ex) {
            throw new GithubApiException(
                    "Could not reach GitHub API.", ex);
        }
    }
}