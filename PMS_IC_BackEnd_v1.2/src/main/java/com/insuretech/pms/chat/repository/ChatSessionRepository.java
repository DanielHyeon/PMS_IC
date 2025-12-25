package com.insuretech.pms.chat.repository;

import com.insuretech.pms.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    List<ChatSession> findByUserIdAndActiveTrueOrderByCreatedAtDesc(String userId);
}