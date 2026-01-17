package com.insuretech.pms.lineage.controller;

import com.insuretech.pms.lineage.dto.*;
import com.insuretech.pms.lineage.service.LineageQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller for lineage visualization and impact analysis.
 * Provides endpoints for graph data, timeline events, and dependency tracking.
 */
@Slf4j
@RestController
@RequestMapping("/api/lineage")
@RequiredArgsConstructor
@Tag(name = "Lineage", description = "Lineage visualization and impact analysis APIs")
public class LineageController {

    private final LineageQueryService lineageQueryService;

    @GetMapping("/graph/{projectId}")
    @Operation(summary = "Get lineage graph for a project",
               description = "Returns all nodes (Requirements, Stories, Tasks) and edges for visualization")
    public ResponseEntity<LineageGraphDto> getLineageGraph(
            @Parameter(description = "Project ID") @PathVariable String projectId) {
        log.info("Fetching lineage graph for project: {}", projectId);
        LineageGraphDto graph = lineageQueryService.getProjectGraph(projectId);
        return ResponseEntity.ok(graph);
    }

    @GetMapping("/timeline/{projectId}")
    @Operation(summary = "Get activity timeline for a project",
               description = "Returns paginated lineage events with optional filtering")
    public ResponseEntity<Page<LineageEventDto>> getTimeline(
            @Parameter(description = "Project ID") @PathVariable String projectId,
            @Parameter(description = "Filter by aggregate type (REQUIREMENT, USER_STORY, TASK, SPRINT)")
            @RequestParam(required = false) String aggregateType,
            @Parameter(description = "Filter events after this date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @Parameter(description = "Filter events before this date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime until,
            @Parameter(description = "Filter by user ID")
            @RequestParam(required = false) String userId,
            Pageable pageable) {
        log.info("Fetching timeline for project: {}, aggregateType: {}, since: {}, until: {}",
                projectId, aggregateType, since, until);
        Page<LineageEventDto> events = lineageQueryService.getTimeline(
                projectId, aggregateType, since, until, userId, pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/history/{aggregateType}/{aggregateId}")
    @Operation(summary = "Get history for a specific entity",
               description = "Returns all events related to a specific requirement, story, or task")
    public ResponseEntity<List<LineageEventDto>> getEntityHistory(
            @Parameter(description = "Entity type (REQUIREMENT, USER_STORY, TASK)")
            @PathVariable String aggregateType,
            @Parameter(description = "Entity ID")
            @PathVariable String aggregateId) {
        log.info("Fetching history for {}: {}", aggregateType, aggregateId);
        List<LineageEventDto> history = lineageQueryService.getEntityHistory(aggregateType, aggregateId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/upstream/{aggregateType}/{aggregateId}")
    @Operation(summary = "Get upstream dependencies",
               description = "Returns all entities that lead to this entity")
    public ResponseEntity<LineageTreeDto> getUpstream(
            @Parameter(description = "Entity type") @PathVariable String aggregateType,
            @Parameter(description = "Entity ID") @PathVariable String aggregateId,
            @Parameter(description = "Maximum depth to traverse")
            @RequestParam(defaultValue = "3") int depth) {
        log.info("Fetching upstream for {}: {} with depth {}", aggregateType, aggregateId, depth);
        LineageTreeDto tree = lineageQueryService.getUpstream(aggregateType, aggregateId, depth);
        return ResponseEntity.ok(tree);
    }

    @GetMapping("/downstream/{aggregateType}/{aggregateId}")
    @Operation(summary = "Get downstream dependencies",
               description = "Returns all entities that depend on this entity")
    public ResponseEntity<LineageTreeDto> getDownstream(
            @Parameter(description = "Entity type") @PathVariable String aggregateType,
            @Parameter(description = "Entity ID") @PathVariable String aggregateId,
            @Parameter(description = "Maximum depth to traverse")
            @RequestParam(defaultValue = "3") int depth) {
        log.info("Fetching downstream for {}: {} with depth {}", aggregateType, aggregateId, depth);
        LineageTreeDto tree = lineageQueryService.getDownstream(aggregateType, aggregateId, depth);
        return ResponseEntity.ok(tree);
    }

    @GetMapping("/impact/{aggregateType}/{aggregateId}")
    @Operation(summary = "Analyze impact of entity change",
               description = "Returns impact analysis showing affected stories, tasks, and sprints")
    public ResponseEntity<ImpactAnalysisDto> analyzeImpact(
            @Parameter(description = "Entity type") @PathVariable String aggregateType,
            @Parameter(description = "Entity ID") @PathVariable String aggregateId) {
        log.info("Analyzing impact for {}: {}", aggregateType, aggregateId);
        ImpactAnalysisDto impact = lineageQueryService.analyzeImpact(aggregateType, aggregateId);
        return ResponseEntity.ok(impact);
    }

    @GetMapping("/statistics/{projectId}")
    @Operation(summary = "Get lineage statistics for a project",
               description = "Returns coverage metrics and counts")
    public ResponseEntity<LineageGraphDto.LineageStatisticsDto> getStatistics(
            @Parameter(description = "Project ID") @PathVariable String projectId) {
        log.info("Fetching lineage statistics for project: {}", projectId);
        LineageGraphDto.LineageStatisticsDto stats = lineageQueryService.getStatistics(projectId);
        return ResponseEntity.ok(stats);
    }
}
