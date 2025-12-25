package com.insuretech.pms.task.controller;

import com.insuretech.pms.task.dto.CreateUserStoryRequest;
import com.insuretech.pms.task.dto.ReorderUserStoryRequest;
import com.insuretech.pms.task.dto.UpdateUserStoryRequest;
import com.insuretech.pms.task.dto.UserStoryResponse;
import com.insuretech.pms.task.entity.UserStory;
import com.insuretech.pms.task.service.UserStoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
@Tag(name = "User Stories", description = "사용자 스토리 관리 API")
public class UserStoryController {

    private final UserStoryService userStoryService;

    @GetMapping
    @Operation(summary = "모든 사용자 스토리 조회", description = "프로젝트의 모든 사용자 스토리를 조회합니다")
    public ResponseEntity<List<UserStoryResponse>> getAllStories(
            @RequestParam String projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String epic) {

        List<UserStoryResponse> stories;

        if (status != null) {
            UserStory.StoryStatus storyStatus = UserStory.StoryStatus.valueOf(status.toUpperCase());
            stories = userStoryService.getStoriesByStatus(projectId, storyStatus);
        } else if (epic != null) {
            stories = userStoryService.getStoriesByEpic(projectId, epic);
        } else {
            stories = userStoryService.getAllStories(projectId);
        }

        return ResponseEntity.ok(stories);
    }

    @PostMapping
    @Operation(summary = "사용자 스토리 생성", description = "새로운 사용자 스토리를 생성합니다")
    public ResponseEntity<UserStoryResponse> createStory(@Valid @RequestBody CreateUserStoryRequest request) {
        UserStoryResponse story = userStoryService.createStory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(story);
    }

    @PutMapping("/{id}")
    @Operation(summary = "사용자 스토리 수정", description = "기존 사용자 스토리를 수정합니다")
    public ResponseEntity<UserStoryResponse> updateStory(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserStoryRequest request) {
        UserStoryResponse story = userStoryService.updateStory(id, request);
        return ResponseEntity.ok(story);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "사용자 스토리 삭제", description = "사용자 스토리를 삭제합니다")
    public ResponseEntity<Void> deleteStory(@PathVariable String id) {
        userStoryService.deleteStory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reorder")
    @Operation(summary = "사용자 스토리 순서 변경", description = "백로그 내에서 사용자 스토리의 우선순위를 변경합니다")
    public ResponseEntity<Void> reorderStory(@Valid @RequestBody ReorderUserStoryRequest request) {
        userStoryService.reorderStory(request);
        return ResponseEntity.ok().build();
    }
}
