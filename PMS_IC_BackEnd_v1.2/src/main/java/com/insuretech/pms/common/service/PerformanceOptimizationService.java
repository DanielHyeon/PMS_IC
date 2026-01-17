package com.insuretech.pms.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Service for optimizing performance when handling large data sets
 * Provides caching, pagination, and query optimization strategies
 */
@Service
@RequiredArgsConstructor
public class PerformanceOptimizationService {

    private static final Logger logger = Logger.getLogger(PerformanceOptimizationService.class.getName());

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 500;
    private static final int CACHE_TTL_MINUTES = 5;

    /**
     * Create optimized pageable for large data sets
     */
    public Pageable createOptimizedPageable(int pageNumber, int pageSize, String sortBy, String sortDirection) {
        // Ensure page is valid (0-based)
        int page = Math.max(0, pageNumber - 1);

        // Limit page size
        int limitedPageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        limitedPageSize = Math.max(limitedPageSize, 1);

        // Create sort
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection.toUpperCase()), sortBy);

        return PageRequest.of(page, limitedPageSize, sort);
    }

    /**
     * Create pageable with default size
     */
    public Pageable createOptimizedPageable(int pageNumber) {
        return createOptimizedPageable(pageNumber, DEFAULT_PAGE_SIZE, "id", "asc");
    }

    /**
     * Calculate optimal batch size based on memory constraints
     */
    public int calculateOptimalBatchSize(long totalItems, int maxMemoryMb) {
        // Estimate bytes per item
        final int ESTIMATED_BYTES_PER_ITEM = 1024; // 1KB average
        final int TARGET_BATCH_MEMORY_MB = Math.min(maxMemoryMb, 50); // Max 50MB per batch
        final long TARGET_BATCH_MEMORY_BYTES = TARGET_BATCH_MEMORY_MB * 1024L * 1024L;

        long calculatedBatchSize = TARGET_BATCH_MEMORY_BYTES / ESTIMATED_BYTES_PER_ITEM;
        calculatedBatchSize = Math.max(100, calculatedBatchSize);
        calculatedBatchSize = Math.min(calculatedBatchSize, MAX_PAGE_SIZE);

        logger.info("Calculated optimal batch size: " + calculatedBatchSize + " for " + totalItems + " items");
        return (int) calculatedBatchSize;
    }

    /**
     * Get cache key for pagination results
     */
    public String getCacheKey(String prefix, int page, int pageSize) {
        return prefix + ":page:" + page + ":size:" + pageSize;
    }

    /**
     * Get cache key for filtered results
     */
    public String getCacheKey(String prefix, String filter, int page, int pageSize) {
        return prefix + ":filter:" + filter + ":page:" + page + ":size:" + pageSize;
    }

    /**
     * Check if data set is large enough to require optimization
     */
    public boolean isLargeDataSet(long itemCount) {
        return itemCount > 1000;
    }

    /**
     * Calculate recommended chunk size for batch processing
     */
    public int getRecommendedChunkSize(long totalItems) {
        if (totalItems <= 100) {
            return (int) totalItems;
        } else if (totalItems <= 1000) {
            return 100;
        } else if (totalItems <= 10000) {
            return 500;
        } else if (totalItems <= 100000) {
            return 1000;
        } else {
            return 2000;
        }
    }

    /**
     * Convert page size limit
     */
    public int limitPageSize(int requestedPageSize) {
        int limited = Math.max(1, Math.min(requestedPageSize, MAX_PAGE_SIZE));
        if (limited != requestedPageSize) {
            logger.warning("Requested page size " + requestedPageSize + " limited to " + limited);
        }
        return limited;
    }

    /**
     * Get cache TTL in seconds
     */
    public long getCacheTtlSeconds() {
        return (long) CACHE_TTL_MINUTES * 60;
    }

    /**
     * Estimate memory usage for data set
     */
    public long estimateMemoryUsageBytes(long itemCount) {
        final int BYTES_PER_ITEM = 1024; // Conservative estimate
        return itemCount * BYTES_PER_ITEM;
    }

    /**
     * Check if memory usage is acceptable
     */
    public boolean isMemoryUsageAcceptable(long estimatedBytes) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long availableMemory = maxMemory - usedMemory;

        // Allow operation if estimated bytes < 30% of available memory
        return estimatedBytes < (availableMemory * 0.3);
    }

    /**
     * Log query performance metrics
     */
    @Transactional(readOnly = true)
    public void logQueryPerformance(String queryName, long durationMs, long resultCount) {
        if (durationMs > 1000) {
            logger.warning(
                String.format(
                    "SLOW QUERY: %s took %dms to return %d results",
                    queryName, durationMs, resultCount
                )
            );
        } else {
            logger.info(
                String.format(
                    "Query: %s took %dms to return %d results",
                    queryName, durationMs, resultCount
                )
            );
        }
    }

    /**
     * Get performance recommendations
     */
    public Map<String, Object> getPerformanceRecommendations(long itemCount, long durationMs) {
        Map<String, Object> recommendations = new ConcurrentHashMap<>();

        recommendations.put("totalItems", itemCount);
        recommendations.put("queryDurationMs", durationMs);
        recommendations.put("isLargeDataSet", isLargeDataSet(itemCount));

        if (isLargeDataSet(itemCount)) {
            recommendations.put("recommendPagination", true);
            recommendations.put("recommendedPageSize", getRecommendedChunkSize(itemCount));
            recommendations.put("recommendedCaching", true);
            recommendations.put("cacheTtlSeconds", getCacheTtlSeconds());
        }

        if (durationMs > 5000) {
            recommendations.put("performance", "POOR");
            recommendations.put("suggestion", "Consider adding database indexes or optimizing query");
        } else if (durationMs > 1000) {
            recommendations.put("performance", "ACCEPTABLE");
            recommendations.put("suggestion", "Query performance could be improved with pagination");
        } else {
            recommendations.put("performance", "GOOD");
        }

        return recommendations;
    }
}
