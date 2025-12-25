package com.insuretech.pms.task.repository;

import com.insuretech.pms.task.entity.UserStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserStoryRepository extends JpaRepository<UserStory, String> {

    List<UserStory> findByProjectIdOrderByPriorityOrderAsc(String projectId);

    List<UserStory> findByProjectIdAndStatusOrderByPriorityOrderAsc(String projectId, UserStory.StoryStatus status);

    List<UserStory> findByProjectIdAndEpicOrderByPriorityOrderAsc(String projectId, String epic);

    @Query("SELECT MAX(u.priorityOrder) FROM UserStory u WHERE u.projectId = ?1 AND u.status = ?2")
    Integer findMaxPriorityOrderByProjectIdAndStatus(String projectId, UserStory.StoryStatus status);
}
