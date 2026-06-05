package com.prashant.githubexplorer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Reports whether a given username is currently cached.
 */
@Getter
@AllArgsConstructor
public class CacheStatusResponse {

    private final String  username;
    private final boolean userCached;
    private final boolean reposCached;
    private final String  message;
}