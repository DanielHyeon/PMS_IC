package com.insuretech.pms.education.repository;

import com.insuretech.pms.education.entity.EducationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EducationSessionRepository extends JpaRepository<EducationSession, String> {
    List<EducationSession> findByEducationIdOrderByScheduledAtDesc(String educationId);
    List<EducationSession> findByStatusOrderByScheduledAtAsc(EducationSession.SessionStatus status);
}
