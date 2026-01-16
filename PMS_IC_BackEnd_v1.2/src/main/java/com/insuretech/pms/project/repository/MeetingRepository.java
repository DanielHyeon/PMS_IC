package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, String> {
    List<Meeting> findByProjectIdOrderByScheduledAtDesc(String projectId);
}
