package com.insuretech.pms.education.repository;

import com.insuretech.pms.education.entity.EducationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EducationHistoryRepository extends JpaRepository<EducationHistory, String> {
    List<EducationHistory> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    List<EducationHistory> findByParticipantIdOrderByCreatedAtDesc(String participantId);
    List<EducationHistory> findByCompletionStatusOrderByCreatedAtDesc(EducationHistory.CompletionStatus status);
}
