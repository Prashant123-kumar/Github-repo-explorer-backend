package com.prashant.githubexplorer.dto;

import lombok.Getter;

import java.util.List;

/**
 * Generic paginated response envelope.
 *
 * <p>Wraps any list with pagination metadata so the frontend
 * knows how to render "load more" / page controls.
 */
@Getter
public class PagedResponse<T> {

    /** The slice of data for the current page. */
    private final List<T> data;

    /** 1-based current page number. */
    private final int currentPage;

    /** Number of items per page. */
    private final int pageSize;

    /** Total number of items across all pages. */
    private final int totalItems;

    /** Total number of pages. */
    private final int totalPages;

    /** Whether a next page exists. */
    private final boolean hasNext;

    /** Whether a previous page exists. */
    private final boolean hasPrevious;

    /** Sort field applied to this result. */
    private final String sortBy;

    public PagedResponse(List<T> allItems,
                         int currentPage,
                         int pageSize,
                         String sortBy) {

        this.totalItems  = allItems.size();
        this.pageSize    = pageSize;
        this.currentPage = currentPage;
        this.sortBy      = sortBy;
        this.totalPages  = (int) Math.ceil((double) totalItems / pageSize);
        this.hasNext     = currentPage < totalPages;
        this.hasPrevious = currentPage > 1;

        int start = (currentPage - 1) * pageSize;
        int end   = Math.min(start + pageSize, totalItems);

        this.data = (start >= totalItems)
                ? List.of()
                : allItems.subList(start, end);
    }
}