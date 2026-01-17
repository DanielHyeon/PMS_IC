package com.insuretech.pms.common.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@DisplayName("Performance Optimization Service Tests")
class PerformanceOptimizationServiceTest {

    @Autowired
    private PerformanceOptimizationService performanceService;

    @BeforeEach
    void setUp() {
        assertThat(performanceService).isNotNull();
    }

    @Test
    @DisplayName("Create optimized pageable with default parameters")
    void testCreateOptimizedPageableDefault() {
        Pageable pageable = performanceService.createOptimizedPageable(1);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("Create optimized pageable with custom parameters")
    void testCreateOptimizedPageableCustom() {
        Pageable pageable = performanceService.createOptimizedPageable(2, 25, "name", "asc");

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(25);
    }

    @Test
    @DisplayName("Limit page size to maximum")
    void testLimitPageSize() {
        int limited = performanceService.limitPageSize(1000);

        assertThat(limited).isLessThanOrEqualTo(500);
        assertThat(limited).isEqualTo(500);
    }

    @Test
    @DisplayName("Limit page size minimum value")
    void testLimitPageSizeMinimum() {
        int limited = performanceService.limitPageSize(0);

        assertThat(limited).isEqualTo(1);
    }

    @Test
    @DisplayName("Calculate optimal batch size for small dataset")
    void testCalculateOptimalBatchSizeSmall() {
        int batchSize = performanceService.calculateOptimalBatchSize(100, 50);

        assertThat(batchSize).isGreaterThan(0);
        assertThat(batchSize).isLessThanOrEqualTo(500);
    }

    @Test
    @DisplayName("Calculate optimal batch size for large dataset")
    void testCalculateOptimalBatchSizeLarge() {
        int batchSize = performanceService.calculateOptimalBatchSize(100000, 50);

        assertThat(batchSize).isGreaterThan(0);
        assertThat(batchSize).isLessThanOrEqualTo(500);
    }

    @Test
    @DisplayName("Detect large data sets")
    void testIsLargeDataSet() {
        assertThat(performanceService.isLargeDataSet(100)).isFalse();
        assertThat(performanceService.isLargeDataSet(1000)).isFalse();
        assertThat(performanceService.isLargeDataSet(1001)).isTrue();
        assertThat(performanceService.isLargeDataSet(100000)).isTrue();
    }

    @Test
    @DisplayName("Get recommended chunk size based on item count")
    void testGetRecommendedChunkSize() {
        assertThat(performanceService.getRecommendedChunkSize(50)).isEqualTo(50);
        assertThat(performanceService.getRecommendedChunkSize(500)).isEqualTo(100);
        assertThat(performanceService.getRecommendedChunkSize(5000)).isEqualTo(500);
        assertThat(performanceService.getRecommendedChunkSize(50000)).isEqualTo(1000);
        assertThat(performanceService.getRecommendedChunkSize(500000)).isEqualTo(2000);
    }

    @Test
    @DisplayName("Generate cache key with pagination")
    void testGetCacheKey() {
        String cacheKey = performanceService.getCacheKey("wip", 1, 50);

        assertThat(cacheKey).contains("wip");
        assertThat(cacheKey).contains("page");
        assertThat(cacheKey).contains("size");
    }

    @Test
    @DisplayName("Generate cache key with filter")
    void testGetCacheKeyWithFilter() {
        String cacheKey = performanceService.getCacheKey("wip", "CRITICAL", 1, 50);

        assertThat(cacheKey).contains("wip");
        assertThat(cacheKey).contains("filter");
        assertThat(cacheKey).contains("CRITICAL");
    }

    @Test
    @DisplayName("Get cache TTL in seconds")
    void testGetCacheTtlSeconds() {
        long ttl = performanceService.getCacheTtlSeconds();

        assertThat(ttl).isGreaterThan(0);
        assertThat(ttl).isEqualTo(5 * 60); // 5 minutes
    }

    @Test
    @DisplayName("Estimate memory usage")
    void testEstimateMemoryUsageBytes() {
        long usage = performanceService.estimateMemoryUsageBytes(1000);

        assertThat(usage).isGreaterThan(0);
        assertThat(usage).isEqualTo(1000 * 1024); // 1MB for 1000 items
    }

    @Test
    @DisplayName("Check memory usage acceptability")
    void testIsMemoryUsageAcceptable() {
        long smallUsage = 1024 * 1024; // 1MB - should be acceptable
        boolean acceptable = performanceService.isMemoryUsageAcceptable(smallUsage);

        assertThat(acceptable).isTrue();
    }

    @Test
    @DisplayName("Get performance recommendations for small dataset")
    void testGetPerformanceRecommendationsSmall() {
        Map<String, Object> recommendations = performanceService.getPerformanceRecommendations(100, 50);

        assertThat(recommendations).containsKeys("totalItems", "queryDurationMs");
        assertThat(recommendations.get("isLargeDataSet")).isEqualTo(false);
        assertThat(recommendations.get("performance")).isEqualTo("GOOD");
    }

    @Test
    @DisplayName("Get performance recommendations for large dataset with slow query")
    void testGetPerformanceRecommendationsLargeSlow() {
        Map<String, Object> recommendations = performanceService.getPerformanceRecommendations(10000, 6000);

        assertThat(recommendations).containsKeys("totalItems", "queryDurationMs");
        assertThat(recommendations.get("isLargeDataSet")).isEqualTo(true);
        assertThat(recommendations.get("recommendPagination")).isEqualTo(true);
        assertThat(recommendations.get("recommendCaching")).isEqualTo(true);
        assertThat(recommendations.get("performance")).isEqualTo("POOR");
    }

    @Test
    @DisplayName("Page number must be valid (0-based internally)")
    void testPageNumberValidation() {
        Pageable page0 = performanceService.createOptimizedPageable(0);
        Pageable page1 = performanceService.createOptimizedPageable(1);
        Pageable page2 = performanceService.createOptimizedPageable(2);

        assertThat(page0.getPageNumber()).isEqualTo(0);
        assertThat(page1.getPageNumber()).isEqualTo(0);
        assertThat(page2.getPageNumber()).isEqualTo(1);
    }
}
