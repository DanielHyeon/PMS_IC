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