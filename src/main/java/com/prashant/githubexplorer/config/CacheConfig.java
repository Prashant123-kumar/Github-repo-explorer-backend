package com.prashant.githubexplorer.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * In-memory Caffeine cache.
 *
 * <p>TTL and max size are both externalised via application.properties
 * so they can be tuned without a code change.
 *
 * <p>Cache names:
 * <ul>
 *   <li>{@code githubUsers}   – user profile, keyed by username</li>
 *   <li>{@code githubRepos}   – full repo list, keyed by username</li>
 *   <li>{@code repoDetails}   – single repo detail, keyed by username+repo</li>
 *   <li>{@code languageStats} – aggregated language breakdown per user</li>
 * </ul>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CacheConfig {

    @Value("${cache.ttl-seconds:60}")
    private long ttlSeconds;

    @Value("${cache.max-size:500}")
    private long maxSize;

    @Bean
    public CacheManager cacheManager() {

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "githubUsers",
                "githubRepos",
                "repoDetails",
                "languageStats"
        );

        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                        .maximumSize(maxSize)
                        .recordStats()   // exposes hit/miss metrics via Actuator
        );

        log.info("Cache configured – TTL={}s, maxSize={}", ttlSeconds, maxSize);
        return cacheManager;
    }
}