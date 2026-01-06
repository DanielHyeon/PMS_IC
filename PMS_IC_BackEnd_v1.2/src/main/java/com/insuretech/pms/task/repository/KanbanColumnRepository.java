package com.insuretech.pms.task.repository;

import com.insuretech.pms.task.entity.KanbanColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KanbanColumnRepository extends JpaRepository<KanbanColumn, String> {
    List<KanbanColumn> findAllByOrderByOrderNumAsc();
    List<KanbanColumn> findByProjectIdOrderByOrderNumAsc(String projectId);
}
