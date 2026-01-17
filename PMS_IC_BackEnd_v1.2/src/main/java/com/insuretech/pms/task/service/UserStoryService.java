package com.insuretech.pms.task.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insuretech.pms.lineage.service.LineageEventProducer;
import com.insuretech.pms.task.dto.CreateUserStoryRequest;
import com.insuretech.pms.task.dto.ReorderUserStoryRequest;
import com.insuretech.pms.task.dto.UpdateUserStoryRequest;
import com.insuretech.pms.task.dto.UserStoryResponse;
import com.insuretech.pms.task.entity.Sprint;
import com.insuretech.pms.task.entity.UserStory;
import com.insuretech.pms.task.repository.SprintRepository;
import com.insuretech.pms.task.repository.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserStoryService {

    private final UserStoryRepository userStoryRepository;
    private final SprintRepository sprintRepository;
    private final ObjectMapper objectMapper;
    private final LineageEventProducer lineageEventProducer;

    public List<UserStoryResponse> getAllStories(String projectId) {
        List<UserStory> stories = userStoryRepository.findByProjectIdOrderByPriorityOrderAsc(projectId);
        return stories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<UserStoryResponse> getStoriesByStatus(String projectId, UserStory.StoryStatus status) {
        List<UserStory> stories = userStoryRepository.findByProjectIdAndStatusOrderByPriorityOrderAsc(projectId, status);
        return stories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<UserStoryResponse> getStoriesByEpic(String projectId, String epic) {
        List<UserStory> stories = userStoryRepository.findByProjectIdAndEpicOrderByPriorityOrderAsc(projectId, epic);
        return stories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserStoryResponse createStory(CreateUserStoryRequest request) {
        Integer maxPriority = userStoryRepository.findMaxPriorityOrderByProjectIdAndStatus(
                request.getProjectId(), UserStory.StoryStatus.BACKLOG);
        int nextPriority = (maxPriority == null) ? 1 : maxPriority + 1;

        UserStory story = UserStory.builder()
                .projectId(request.getProjectId())
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .storyPoints(request.getStoryPoints())
                .epic(request.getEpic())
                .assigneeId(request.getAssignee())
                .status(UserStory.StoryStatus.BACKLOG)
                .priorityOrder(nextPriority)
                .acceptanceCriteria(serializeAcceptanceCriteria(request.getAcceptanceCriteria()))
                .build();

        UserStory saved = userStoryRepository.save(story);

        // Publish lineage event
        lineageEventProducer.publishStoryCreated(
                saved.getId(), request.getProjectId(), saved.getTitle(), saved.getStoryPoints());

        return toResponse(saved);
    }

    public UserStoryResponse updateStory(String id, UpdateUserStoryRequest request) {
        UserStory story = userStoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User story not found: " + id));

        if (request.getTitle() != null) {
            story.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            story.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            story.setPriority(request.getPriority());
        }
        if (request.getStoryPoints() != null) {
            story.setStoryPoints(request.getStoryPoints());
        }
        if (request.getStatus() != null) {
            story.setStatus(request.getStatus());
        }
        if (request.getEpic() != null) {
            story.setEpic(request.getEpic());
        }
        if (request.getAssignee() != null) {
            story.setAssigneeId(request.getAssignee());
        }
        if (request.getAcceptanceCriteria() != null) {
            story.setAcceptanceCriteria(serializeAcceptanceCriteria(request.getAcceptanceCriteria()));
        }
        if (request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new RuntimeException("Sprint not found: " + request.getSprintId()));
            story.setSprint(sprint);
        }

        UserStory updated = userStoryRepository.save(story);
        return toResponse(updated);
    }

    public void deleteStory(String id) {
        UserStory story = userStoryRepository.findById(id).orElse(null);
        userStoryRepository.deleteById(id);

        // Publish lineage event
        if (story != null) {
            lineageEventProducer.publishStoryDeleted(id, story.getProjectId());
        }
    }

    public void reorderStory(ReorderUserStoryRequest request) {
        UserStory story = userStoryRepository.findById(request.getStoryId())
                .orElseThrow(() -> new RuntimeException("User story not found: " + request.getStoryId()));

        Integer currentOrder = story.getPriorityOrder();
        Integer newOrder = request.getNewPriorityOrder();

        if (currentOrder.equals(newOrder)) {
            return;
        }

        List<UserStory> stories = userStoryRepository.findByProjectIdAndStatusOrderByPriorityOrderAsc(
                story.getProjectId(), story.getStatus());

        if (currentOrder < newOrder) {
            for (UserStory s : stories) {
                if (s.getPriorityOrder() > currentOrder && s.getPriorityOrder() <= newOrder) {
                    s.setPriorityOrder(s.getPriorityOrder() - 1);
                }
            }
        } else {
            for (UserStory s : stories) {
                if (s.getPriorityOrder() >= newOrder && s.getPriorityOrder() < currentOrder) {
                    s.setPriorityOrder(s.getPriorityOrder() + 1);
                }
            }
        }

        story.setPriorityOrder(newOrder);
        userStoryRepository.saveAll(stories);
    }

    private UserStoryResponse toResponse(UserStory story) {
        List<String> criteria = deserializeAcceptanceCriteria(story.getAcceptanceCriteria());
        return UserStoryResponse.fromEntity(story, criteria);
    }

    private String serializeAcceptanceCriteria(List<String> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(criteria);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize acceptance criteria", e);
        }
    }

    private List<String> deserializeAcceptanceCriteria(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
