package com.prashant.githubexplorer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * Language distribution across a user's public repos.
 *
 * <p>{@code counts}  – raw number of repos using each language<br>
 *    {@code percentages} – percentage share (rounded to 1 dp)
 */
@Getter
@AllArgsConstructor
public class LanguageStatsResponse {

    private final String username;
    private final Map<String, Long>   counts;
    private final Map<String, Double> percentages;
    private final int totalReposAnalysed;
}