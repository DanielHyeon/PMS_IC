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