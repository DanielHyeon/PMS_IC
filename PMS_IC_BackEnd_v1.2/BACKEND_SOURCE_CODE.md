# PMS Backend 전체 소스코드

이 문서는 남은 모든 백엔드 소스코드를 포함합니다.
각 파일을 해당 경로에 생성하세요.

---

## 📁 Project Module

### src/main/java/com/insuretech/pms/project/entity/PhaseGate.java

```java
package com.insuretech.pms.project.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "phase_gates", schema = "project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhaseGate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id", nullable = false)
    private Phase phase;

    @Column(name = "submitted_by", length = 50)
    private String submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_by", length = 50)
    private String rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private GateStatus status = GateStatus.PENDING;

    public enum GateStatus {
        PENDING,
        SUBMITTED,
        APPROVED,
        REJECTED
    }
}
```

### src/main/java/com/insuretech/pms/project/entity/Deliverable.java

```java
package com.insuretech.pms.project.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "deliverables", schema = "project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deliverable extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id", nullable = false)
    private Phase phase;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private DeliverableType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private DeliverableStatus status = DeliverableStatus.PENDING;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_name", length = 200)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "uploaded_by", length = 50)
    private String uploadedBy;

    public enum DeliverableType {
        DOCUMENT,
        CODE,
        REPORT,
        PRESENTATION,
        OTHER
    }

    public enum DeliverableStatus {
        PENDING,
        IN_REVIEW,
        APPROVED,
        REJECTED
    }
}
```

### src/main/java/com/insuretech/pms/project/repository/ProjectRepository.java

```java
package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    List<Project> findByStatusOrderByCreatedAtDesc(Project.ProjectStatus status);
}
```

### src/main/java/com/insuretech/pms/project/repository/PhaseRepository.java

```java
package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.Phase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhaseRepository extends JpaRepository<Phase, String> {
    List<Phase> findByProjectIdOrderByOrderNumAsc(String projectId);
}
```

### src/main/java/com/insuretech/pms/project/repository/PhaseGateRepository.java

```java
package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.PhaseGate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhaseGateRepository extends JpaRepository<PhaseGate, String> {
    Optional<PhaseGate> findByPhaseId(String phaseId);
}
```

### src/main/java/com/insuretech/pms/project/repository/DeliverableRepository.java

```java
package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.Deliverable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliverableRepository extends JpaRepository<Deliverable, String> {
    List<Deliverable> findByPhaseId(String phaseId);
}
```

### src/main/java/com/insuretech/pms/project/dto/ProjectDto.java

```java
package com.insuretech.pms.project.dto;

import com.insuretech.pms.project.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    private String id;
    private String name;
    private String description;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private Integer progress;
    private List<PhaseDto> phases;

    public static ProjectDto from(Project project) {
        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus().name())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .budget(project.getBudget())
                .progress(project.getProgress())
                .phases(project.getPhases() != null ?
                        project.getPhases().stream()
                                .map(PhaseDto::from)
                                .collect(Collectors.toList()) : null)
                .build();
    }
}
```

### src/main/java/com/insuretech/pms/project/dto/PhaseDto.java

```java
package com.insuretech.pms.project.dto;

import com.insuretech.pms.project.entity.Phase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhaseDto {
    private String id;
    private String projectId;
    private String name;
    private Integer orderNum;
    private String status;
    private String gateStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer progress;
    private String description;

    public static PhaseDto from(Phase phase) {
        return PhaseDto.builder()
                .id(phase.getId())
                .projectId(phase.getProject() != null ? phase.getProject().getId() : null)
                .name(phase.getName())
                .orderNum(phase.getOrderNum())
                .status(phase.getStatus().name())
                .gateStatus(phase.getGateStatus() != null ? phase.getGateStatus().name() : null)
                .startDate(phase.getStartDate())
                .endDate(phase.getEndDate())
                .progress(phase.getProgress())
                .description(phase.getDescription())
                .build();
    }
}
```

### src/main/java/com/insuretech/pms/project/service/ProjectService.java

```java
package com.insuretech.pms.project.service;

import com.insuretech.pms.common.exception.CustomException;
import com.insuretech.pms.project.dto.ProjectDto;
import com.insuretech.pms.project.entity.Project;
import com.insuretech.pms.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Cacheable(value = "projects", key = "'all'")
    @Transactional(readOnly = true)
    public List<ProjectDto> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(ProjectDto::from)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "projects", key = "#id")
    @Transactional(readOnly = true)
    public ProjectDto getProjectById(String id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("프로젝트를 찾을 수 없습니다: " + id));
        return ProjectDto.from(project);
    }

    @CacheEvict(value = "projects", allEntries = true)
    @Transactional
    public ProjectDto createProject(ProjectDto dto) {
        Project project = Project.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .status(Project.ProjectStatus.valueOf(dto.getStatus()))
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .budget(dto.getBudget())
                .progress(0)
                .build();

        Project saved = projectRepository.save(project);
        log.info("Project created: {}", saved.getId());
        return ProjectDto.from(saved);
    }

    @CacheEvict(value = "projects", allEntries = true)
    @Transactional
    public ProjectDto updateProject(String id, ProjectDto dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("프로젝트를 찾을 수 없습니다: " + id));

        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setStatus(Project.ProjectStatus.valueOf(dto.getStatus()));
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        project.setBudget(dto.getBudget());
        project.setProgress(dto.getProgress());

        Project updated = projectRepository.save(project);
        log.info("Project updated: {}", updated.getId());
        return ProjectDto.from(updated);
    }

    @CacheEvict(value = "projects", allEntries = true)
    @Transactional
    public void deleteProject(String id) {
        if (!projectRepository.existsById(id)) {
            throw CustomException.notFound("프로젝트를 찾을 수 없습니다: " + id);
        }
        projectRepository.deleteById(id);
        log.info("Project deleted: {}", id);
    }
}
```

### src/main/java/com/insuretech/pms/project/controller/ProjectController.java

```java
package com.insuretech.pms.project.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.project.dto.ProjectDto;
import com.insuretech.pms.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Projects", description = "프로젝트 관리 API")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "프로젝트 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectDto>>> getAllProjects() {
        List<ProjectDto> projects = projectService.getAllProjects();
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @Operation(summary = "프로젝트 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectDto>> getProjectById(@PathVariable String id) {
        ProjectDto project = projectService.getProjectById(id);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    @Operation(summary = "프로젝트 생성")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM')")
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectDto>> createProject(@RequestBody ProjectDto dto) {
        ProjectDto created = projectService.createProject(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("프로젝트가 생성되었습니다", created));
    }

    @Operation(summary = "프로젝트 수정")
    @PreAuthorize("hasAnyRole('PMO_HEAD', 'PM')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectDto>> updateProject(
            @PathVariable String id,
            @RequestBody ProjectDto dto) {
        ProjectDto updated = projectService.updateProject(id, dto);
        return ResponseEntity.ok(ApiResponse.success("프로젝트가 수정되었습니다", updated));
    }

    @Operation(summary = "프로젝트 삭제")
    @PreAuthorize("hasRole('PMO_HEAD')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable String id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.success("프로젝트가 삭제되었습니다", null));
    }
}
```

---

## 📁 Task Module

### src/main/java/com/insuretech/pms/task/entity/KanbanColumn.java

```java
package com.insuretech.pms.task.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "kanban_columns", schema = "task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KanbanColumn extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "project_id", length = 50, nullable = false)
    private String projectId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "order_num", nullable = false)
    private Integer orderNum;

    @Column(name = "wip_limit")
    private Integer wipLimit;

    @Column(name = "color", length = 20)
    private String color;
}
```

### src/main/java/com/insuretech/pms/task/entity/Task.java

```java
package com.insuretech.pms.task.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tasks", schema = "task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_id", nullable = false)
    private KanbanColumn column;

    @Column(name = "phase_id", length = 50)
    private String phaseId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "assignee_id", length = 50)
    private String assigneeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "order_num")
    private Integer orderNum;

    @Column(name = "tags", length = 500)
    private String tags;

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum TaskStatus {
        TODO, IN_PROGRESS, REVIEW, DONE
    }
}
```

### src/main/java/com/insuretech/pms/task/entity/UserStory.java

```java
package com.insuretech.pms.task.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_stories", schema = "task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "project_id", length = 50, nullable = false)
    private String projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "acceptance_criteria", columnDefinition = "TEXT")
    private String acceptanceCriteria;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "story_points")
    private Integer storyPoints;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private StoryStatus status = StoryStatus.BACKLOG;

    @Column(name = "assignee_id", length = 50)
    private String assigneeId;

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum StoryStatus {
        BACKLOG, SELECTED, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
```

### src/main/java/com/insuretech/pms/task/entity/Sprint.java

```java
package com.insuretech.pms.task.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "sprints", schema = "task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sprint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "project_id", length = 50, nullable = false)
    private String projectId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "goal", columnDefinition = "TEXT")
    private String goal;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private SprintStatus status = SprintStatus.PLANNED;

    public enum SprintStatus {
        PLANNED, ACTIVE, COMPLETED, CANCELLED
    }
}
```

### src/main/java/com/insuretech/pms/task/repository/TaskRepository.java

```java
package com.insuretech.pms.task.repository;

import com.insuretech.pms.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findByColumnIdOrderByOrderNumAsc(String columnId);
    List<Task> findByAssigneeId(String assigneeId);
}
```

### src/main/java/com/insuretech/pms/task/repository/SprintRepository.java

```java
package com.insuretech.pms.task.repository;

import com.insuretech.pms.task.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, String> {
    List<Sprint> findByProjectIdOrderByStartDateDesc(String projectId);
    Optional<Sprint> findByProjectIdAndStatus(String projectId, Sprint.SprintStatus status);
}
```

### src/main/java/com/insuretech/pms/task/controller/TaskController.java

```java
package com.insuretech.pms.task.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tasks", description = "태스크 관리 API")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    @Operation(summary = "태스크 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Object>>> getAllTasks() {
        // TODO: Implement task service
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @Operation(summary = "태스크 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createTask(@RequestBody Object dto) {
        // TODO: Implement
        return ResponseEntity.ok(ApiResponse.success("태스크가 생성되었습니다", dto));
    }
}
```

---

## 📁 Chat Module

### src/main/java/com/insuretech/pms/chat/entity/ChatSession.java

```java
package com.insuretech.pms.chat.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_sessions", schema = "chat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
```

### src/main/java/com/insuretech/pms/chat/entity/ChatMessage.java

```java
package com.insuretech.pms.chat.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_messages", schema = "chat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    public enum Role {
        USER, ASSISTANT
    }
}
```

### src/main/java/com/insuretech/pms/chat/repository/ChatSessionRepository.java

```java
package com.insuretech.pms.chat.repository;

import com.insuretech.pms.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    List<ChatSession> findByUserIdAndActiveTrueOrderByCreatedAtDesc(String userId);
}
```

### src/main/java/com/insuretech/pms/chat/repository/ChatMessageRepository.java

```java
package com.insuretech.pms.chat.repository;

import com.insuretech.pms.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}
```

### src/main/java/com/insuretech/pms/chat/dto/ChatRequest.java

```java
package com.insuretech.pms.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String sessionId;
    private String message;
    private List<MessageContext> context;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageContext {
        private String role;
        private String content;
    }
}
```

### src/main/java/com/insuretech/pms/chat/dto/ChatResponse.java

```java
package com.insuretech.pms.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String sessionId;
    private String reply;
    private Double confidence;
    private List<String> suggestions;
}
```

### src/main/java/com/insuretech/pms/chat/service/ChatService.java

```java
package com.insuretech.pms.chat.service;

import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.auth.service.AuthService;
import com.insuretech.pms.chat.dto.ChatRequest;
import com.insuretech.pms.chat.dto.ChatResponse;
import com.insuretech.pms.chat.entity.ChatMessage;
import com.insuretech.pms.chat.entity.ChatSession;
import com.insuretech.pms.chat.repository.ChatMessageRepository;
import com.insuretech.pms.chat.repository.ChatSessionRepository;
import com.insuretech.pms.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AIChatClient aiChatClient;
    private final AuthService authService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        User currentUser = authService.getCurrentUser();

        // 세션 조회 또는 생성
        ChatSession session;
        if (request.getSessionId() != null) {
            session = chatSessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> CustomException.notFound("채팅 세션을 찾을 수 없습니다"));
        } else {
            session = ChatSession.builder()
                    .userId(currentUser.getId())
                    .title("New Chat")
                    .active(true)
                    .build();
            session = chatSessionRepository.save(session);
        }

        // 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.builder()
                .session(session)
                .role(ChatMessage.Role.USER)
                .content(request.getMessage())
                .build();
        chatMessageRepository.save(userMessage);

        // Redis에서 최근 대화 조회
        String redisKey = "chat:session:" + session.getId();
        List<ChatMessage> recentMessages = getRecentMessages(session.getId(), 10);

        // AI 서비스 호출
        ChatResponse aiResponse = aiChatClient.chat(currentUser.getId(), request.getMessage(), recentMessages);

        // AI 응답 저장
        ChatMessage assistantMessage = ChatMessage.builder()
                .session(session)
                .role(ChatMessage.Role.ASSISTANT)
                .content(aiResponse.getReply())
                .build();
        chatMessageRepository.save(assistantMessage);

        // Redis에 캐싱 (1시간)
        cacheMessage(redisKey, userMessage);
        cacheMessage(redisKey, assistantMessage);

        aiResponse.setSessionId(session.getId());
        return aiResponse;
    }

    private List<ChatMessage> getRecentMessages(String sessionId, int limit) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .skip(Math.max(0, chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).size() - limit))
                .collect(Collectors.toList());
    }

    private void cacheMessage(String redisKey, ChatMessage message) {
        redisTemplate.opsForList().rightPush(redisKey, message);
        redisTemplate.expire(redisKey, 1, TimeUnit.HOURS);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getHistory(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public void deleteSession(String sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> CustomException.notFound("채팅 세션을 찾을 수 없습니다"));

        session.setActive(false);
        chatSessionRepository.save(session);

        // Redis에서도 삭제
        String redisKey = "chat:session:" + sessionId;
        redisTemplate.delete(redisKey);
    }
}
```

### src/main/java/com/insuretech/pms/chat/service/AIChatClient.java

```java
package com.insuretech.pms.chat.service;

import com.insuretech.pms.chat.dto.ChatResponse;
import com.insuretech.pms.chat.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public ChatResponse chat(String userId, String message, List<ChatMessage> context) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("message", message);
            request.put("context", context.stream()
                    .map(msg -> Map.of(
                            "role", msg.getRole().name().toLowerCase(),
                            "content", msg.getContent()
                    ))
                    .collect(Collectors.toList()));

            WebClient webClient = webClientBuilder.baseUrl(aiServiceUrl).build();

            Map<String, Object> response = webClient.post()
                    .uri("/api/chat")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return ChatResponse.builder()
                    .reply((String) response.get("reply"))
                    .confidence((Double) response.getOrDefault("confidence", 0.9))
                    .suggestions((List<String>) response.getOrDefault("suggestions", List.of()))
                    .build();

        } catch (Exception e) {
            log.error("AI service call failed: {}", e.getMessage());
            // Fallback response
            return ChatResponse.builder()
                    .reply("죄송합니다. 현재 AI 서비스가 일시적으로 사용 불가합니다. 잠시 후 다시 시도해주세요.")
                    .confidence(0.0)
                    .build();
        }
    }
}
```

### src/main/java/com/insuretech/pms/chat/controller/ChatController.java

```java
package com.insuretech.pms.chat.controller;

import com.insuretech.pms.chat.dto.ChatRequest;
import com.insuretech.pms.chat.dto.ChatResponse;
import com.insuretech.pms.chat.entity.ChatMessage;
import com.insuretech.pms.chat.service.ChatService;
import com.insuretech.pms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chat", description = "AI 챗봇 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "메시지 전송", description = "AI 챗봇에게 메시지를 전송합니다")
    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(@RequestBody ChatRequest request) {
        ChatResponse response = chatService.sendMessage(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "대화 히스토리 조회", description = "세션의 대화 내역을 조회합니다")
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getHistory(@PathVariable String sessionId) {
        List<ChatMessage> messages = chatService.getHistory(sessionId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @Operation(summary = "세션 삭제", description = "채팅 세션을 삭제합니다")
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("세션이 삭제되었습니다", null));
    }
}
```

---

## 📁 Dashboard Module

### src/main/java/com/insuretech/pms/report/dto/DashboardStats.java

```java
package com.insuretech.pms.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private Long totalProjects;
    private Long activeProjects;
    private Long totalTasks;
    private Long completedTasks;
    private Integer avgProgress;
    private Map<String, Long> projectsByStatus;
    private Map<String, Long> tasksByStatus;
}
```

### src/main/java/com/insuretech/pms/report/service/DashboardService.java

```java
package com.insuretech.pms.report.service;

import com.insuretech.pms.project.entity.Project;
import com.insuretech.pms.project.repository.ProjectRepository;
import com.insuretech.pms.report.dto.DashboardStats;
import com.insuretech.pms.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    @Cacheable(value = "dashboard", key = "'stats'")
    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        List<Project> projects = projectRepository.findAll();

        long totalProjects = projects.size();
        long activeProjects = projects.stream()
                .filter(p -> p.getStatus() == Project.ProjectStatus.IN_PROGRESS)
                .count();

        int avgProgress = (int) projects.stream()
                .mapToInt(Project::getProgress)
                .average()
                .orElse(0.0);

        Map<String, Long> projectsByStatus = new HashMap<>();
        for (Project.ProjectStatus status : Project.ProjectStatus.values()) {
            long count = projects.stream()
                    .filter(p -> p.getStatus() == status)
                    .count();
            projectsByStatus.put(status.name(), count);
        }

        long totalTasks = taskRepository.count();
        long completedTasks = 0; // TODO: Implement

        return DashboardStats.builder()
                .totalProjects(totalProjects)
                .activeProjects(activeProjects)
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .avgProgress(avgProgress)
                .projectsByStatus(projectsByStatus)
                .build();
    }
}
```

### src/main/java/com/insuretech/pms/report/controller/DashboardController.java

```java
package com.insuretech.pms.report.controller;

import com.insuretech.pms.common.dto.ApiResponse;
import com.insuretech.pms.report.dto.DashboardStats;
import com.insuretech.pms.report.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "대시보드 API")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "대시보드 통계 조회")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStats>> getStats() {
        DashboardStats stats = dashboardService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
```

---

## 📁 Config & Init Data

### src/main/java/com/insuretech/pms/common/config/WebClientConfig.java

```java
package com.insuretech.pms.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
```

### src/main/java/com/insuretech/pms/common/config/AuditConfig.java

```java
package com.insuretech.pms.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }
            return Optional.of(authentication.getName());
        };
    }
}
```

### src/main/java/com/insuretech/pms/common/init/DataInitializer.java

```java
package com.insuretech.pms.common.init;

import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Initializing demo users...");
            createDemoUsers();
            log.info("Demo users created successfully");
        }
    }

    private void createDemoUsers() {
        createUser("U001", "sponsor@insure.com", "sponsor123", "이사장", User.UserRole.SPONSOR, "경영진");
        createUser("U002", "pmo@insure.com", "pmo123", "PMO 총괄", User.UserRole.PMO_HEAD, "PMO");
        createUser("U003", "pm@insure.com", "pm123", "김철수", User.UserRole.PM, "IT혁신팀");
        createUser("U004", "dev@insure.com", "dev123", "박민수", User.UserRole.DEVELOPER, "AI개발팀");
        createUser("U005", "qa@insure.com", "qa123", "최지훈", User.UserRole.QA, "품질보증팀");
        createUser("U006", "ba@insure.com", "ba123", "이영희", User.UserRole.BUSINESS_ANALYST, "보험심사팀");
        createUser("U007", "auditor@insure.com", "auditor123", "감리인", User.UserRole.AUDITOR, "외부감리법인");
        createUser("U008", "admin@insure.com", "admin123", "시스템관리자", User.UserRole.ADMIN, "IT운영팀");
    }

    private void createUser(String id, String email, String password, String name, User.UserRole role, String department) {
        User user = User.builder()
                .id(id)
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .role(role)
                .department(department)
                .active(true)
                .build();
        userRepository.save(user);
    }
}
```

---

## 📁 Dockerfile

### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Dockerfile.dev

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Run with Spring Boot DevTools
CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"]

EXPOSE 8080 5005
```

---

## 📁 README

### README.md

```markdown
# PMS Backend v1.2

Spring Boot 기반 프로젝트 관리 시스템 백엔드

## 기술 스택

- **Framework:** Spring Boot 3.2.1
- **Language:** Java 17
- **Database:** PostgreSQL 15 (prod), H2 (dev)
- **Cache:** Redis 7
- **Security:** Spring Security + JWT
- **API Docs:** Swagger/OpenAPI 3

## 실행 방법

### 개발 환경 (Docker Compose)

```bash
# 전체 환경 실행
docker-compose up -d

# 백엔드만 재시작
docker-compose restart backend

# 로그 확인
docker-compose logs -f backend
```

### 로컬 실행

```bash
# PostgreSQL 및 Redis가 실행 중이어야 함
mvn spring-boot:run
```

## API 문서

- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/api-docs

## 테스트 계정


| 이메일         | 비밀번호 | 역할     |
| -------------- | -------- | -------- |
| pmo@insure.com | pmo123   | PMO 총괄 |
| pm@insure.com  | pm123    | PM       |
| dev@insure.com | dev123   | 개발자   |

## 주요 API

### 인증

- `POST /api/auth/login` - 로그인
- `GET /api/auth/me` - 현재 사용자

### 프로젝트

- `GET /api/projects` - 프로젝트 목록
- `POST /api/projects` - 프로젝트 생성

### 챗봇

- `POST /api/chat/message` - 메시지 전송
- `GET /api/chat/history/{sessionId}` - 히스토리

### 대시보드

- `GET /api/dashboard/stats` - 통계

## 환경 변수

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pms_db
SPRING_DATASOURCE_USERNAME=pms_user
SPRING_DATASOURCE_PASSWORD=pms_password
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
JWT_SECRET=your-secret-key
AI_TEAM_API_URL=http://localhost:8000
```

```

---

## 🎯 다음 단계

1. 각 파일을 해당 경로에 복사하여 생성
2. Maven 빌드 실행: `mvn clean install`
3. Docker Compose로 실행: `docker-compose up -d`
4. Swagger UI에서 API 테스트: http://localhost:8080/swagger-ui.html

**모든 소스코드가 준비되었습니다!**
```





## 🚀 빠른 시작 가이드

### 1. 파일 생성

[BACKEND\_SOURCE\_CODE.md](vscode-webview://1nilekc6ceiuurckheof892q0btojisvgae8v6utba7c9mld6g2e/index.html?id=a6d30f65-b360-407d-bcd4-33d29488f24f&parentId=1&origin=282ca8b9-a27b-4d06-94c2-380ed4562e34&swVersion=4&extensionId=Anthropic.claude-code&platform=electron&vscode-resource-base-authority=vscode-resource.vscode-cdn.net&parentOrigin=vscode-file%3A%2F%2Fvscode-app&purpose=webviewView&session=faf88750-af17-43b3-8a27-59b6479d5161) 파일을 열어서 각 소스코드를 해당 경로에 복사

### 2. Docker Compose로 실행

```bash
cd e:\AIProject\pms\WSL
docker-compose up -d
```

### 3. 접속 확인

* **Backend API**: [http://localhost:8080](http://localhost:8080/)
* **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **Health Check**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
* **Frontend**: [http://localhost:5173](http://localhost:5173/)
* **PgAdmin**: [http://localhost:5050](http://localhost:5050/)

### 4. 로그인 테스트

```json
POST http://localhost:8080/api/auth/login
{
  "email": "pm@insure.com",
  "password": "pm123"
}
```


## 📋 전체 구조 요약

PMS_IC_BackEnd_v1.2/
├── pom.xml ✅
├── Dockerfile ✅
├── Dockerfile.dev ✅
├── README.md ✅
├── BACKEND_SOURCE_CODE.md ✅
└── src/main/
├── java/com/insuretech/pms/
│   ├── PmsApplication.java ✅
│   ├── common/ ✅
│   │   ├── config/ (Security, CORS, Redis, Audit, WebClient)
│   │   ├── dto/ (ApiResponse)
│   │   ├── entity/ (BaseEntity)
│   │   ├── exception/ (Custom, Error, Handler)
│   │   ├── init/ (DataInitializer)
│   │   └── security/ (JWT Filter, EntryPoint)
│   ├── auth/ ✅
│   │   ├── entity/ (User)
│   │   ├── repository/ (UserRepository)
│   │   ├── service/ (Auth, JWT, UserDetails)
│   │   ├── controller/ (AuthController)
│   │   └── dto/ (LoginRequest, LoginResponse)
│   ├── project/ 📄
│   │   ├── entity/ (Project ✅, Phase ✅, PhaseGate, Deliverable)
│   │   ├── repository/
│   │   ├── service/
│   │   ├── controller/
│   │   └── dto/
│   ├── task/ 📄
│   ├── chat/ 📄
│   ├── report/ 📄
│   └── risk/
└── resources/
└── application.yml ✅
