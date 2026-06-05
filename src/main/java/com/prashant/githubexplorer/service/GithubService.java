package com.prashant.githubexplorer.service;

import com.prashant.githubexplorer.client.GithubApiClient;
import com.prashant.githubexplorer.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core business-logic layer for the GitHub Explorer.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Delegates HTTP calls to {@link GithubApiClient}</li>
 *   <li>Caches results in Caffeine ({@code 60 s} TTL)</li>
 *   <li>Sorts and paginates repo lists in-memory</li>
 *   <li>Computes per-user language statistics</li>
 *   <li>Exposes cache-evict helpers</li>
 * </ul>
 *
 * <p><strong>Caching strategy</strong><br>
 * The full repo list is cached once per username (not per sort/page
 * combination). Sorting and slicing happen in memory so we never hit
 * GitHub for the same user's repos more than once per TTL window.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GithubService {

    private final GithubApiClient githubApiClient;

    // ──────────────────────────────────────────────────────────────────────────
    //  User profile
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the public profile for {@code username}.
     * Result is cached for 60 s under {@code githubUsers::<username>}.
     */
    @Cacheable(value = "githubUsers", key = "#username.toLowerCase()")
    public GithubUserDto getUser(String username) {
        log.info("[CACHE MISS] Fetching user profile for '{}'", username);
        return githubApiClient.getUser(username);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Repositories (paginated + sorted)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated, sorted slice of the user's public repos.
     *
     * <p>The raw list is fetched (and cached) once; sorting and pagination
     * are applied in-memory on every call so the cache key stays simple.
     *
     * @param username  GitHub login
     * @param sortBy    {@code stars} | {@code name} | {@code updated} (default: stars)
     * @param page      1-based page number
     * @param pageSize  items per page (capped at configured max)
     * @return {@link PagedResponse} wrapping the current slice of {@link RepoDto}
     */
    public PagedResponse<RepoDto> getRepositories(
            String username, String sortBy, int page, int pageSize) {

        List<RepoDto> allRepos = getAllReposCached(username);
        List<RepoDto> sorted   = sortRepos(allRepos, sortBy);
        return new PagedResponse<>(sorted, page, pageSize, sortBy);
    }

    /**
     * Fetches (and caches) the COMPLETE repo list for a user.
     * This is the only place GitHub is called for repos.
     */
    @Cacheable(value = "githubRepos", key = "#username.toLowerCase()")
    public List<RepoDto> getAllReposCached(String username) {
        log.info("[CACHE MISS] Fetching all repos for '{}'", username);
        return githubApiClient.getAllRepositories(username);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Repository detail
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns expanded details for a single repository.
     * Cached under {@code repoDetails::<username>-<repoName>}.
     */
    @Cacheable(
            value = "repoDetails",
            key   = "#username.toLowerCase() + '-' + #repoName.toLowerCase()")
    public RepoDetailDto getRepositoryDetail(String username, String repoName) {
        log.info("[CACHE MISS] Fetching repo detail for '{}/{}'", username, repoName);
        return githubApiClient.getRepositoryDetail(username, repoName);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Language statistics
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Computes the distribution of primary languages across a user's repos.
     *
     * <p>Repos without a detected language are counted under {@code "Unknown"}.
     * Percentages are rounded to one decimal place.
     */
    @Cacheable(value = "languageStats", key = "#username.toLowerCase()")
    public LanguageStatsResponse getLanguageStats(String username) {
        log.info("[CACHE MISS] Computing language stats for '{}'", username);

        List<RepoDto> repos = getAllReposCached(username);

        Map<String, Long> counts = repos.stream()
                .collect(Collectors.groupingBy(
                        r -> (r.getLanguage() != null && !r.getLanguage().isBlank())
                                ? r.getLanguage()
                                : "Unknown",
                        Collectors.counting()
                ));

        // Sort descending by count for nicer chart rendering
        Map<String, Long> sortedCounts = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        long total = repos.size();
        Map<String, Double> percentages = sortedCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Math.round((e.getValue() * 1000.0 / total)) / 10.0,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return new LanguageStatsResponse(username, sortedCounts, percentages, (int) total);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Cache management
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Evicts all cached data for the given username.
     * Useful when a user explicitly requests a refresh.
     */
    @Caching(evict = {
            @CacheEvict(value = "githubUsers",   key = "#username.toLowerCase()"),
            @CacheEvict(value = "githubRepos",   key = "#username.toLowerCase()"),
            @CacheEvict(value = "languageStats", key = "#username.toLowerCase()")
    })
    public void evictUserCache(String username) {
        log.info("Cache evicted for user '{}'", username);
    }

    /**
     * Evicts the cached detail entry for a single repo.
     */
    @CacheEvict(
            value = "repoDetails",
            key   = "#username.toLowerCase() + '-' + #repoName.toLowerCase()")
    public void evictRepoDetailCache(String username, String repoName) {
        log.info("Repo-detail cache evicted for '{}/{}'", username, repoName);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Internal helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Sorts a copy of the repo list by the requested field.
     * The original list (held in cache) is never mutated.
     */
    private List<RepoDto> sortRepos(List<RepoDto> repos, String sortBy) {

        List<RepoDto> mutable = new ArrayList<>(repos);

        switch (sortBy.toLowerCase()) {

            case "name" ->
                    mutable.sort(Comparator.comparing(
                            RepoDto::getName, String.CASE_INSENSITIVE_ORDER));

            case "updated" ->
                    mutable.sort(Comparator.comparing(
                            RepoDto::getUpdatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())));

            case "forks" ->
                    mutable.sort(Comparator.comparingInt(
                            RepoDto::getForksCount).reversed());

            case "stars" ->
                    mutable.sort(Comparator.comparingInt(
                            RepoDto::getStargazersCount).reversed());

            default -> {
                log.warn("Unknown sort field '{}', defaulting to 'stars'", sortBy);
                mutable.sort(Comparator.comparingInt(
                        RepoDto::getStargazersCount).reversed());
            }
        }

        return mutable;
    }
}