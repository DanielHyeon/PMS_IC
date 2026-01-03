package com.insuretech.pms.chat.repository;

import com.insuretech.pms.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    
    /**
     * 세션의 최근 메시지 조회 (최적화된 쿼리)
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.session.id = :sessionId ORDER BY m.createdAt ASC")
    List<ChatMessage> findRecentMessagesBySessionId(@Param("sessionId") String sessionId, Pageable pageable);
    
    /**
     * 세션의 메시지 개수 조회
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.session.id = :sessionId")
    long countBySessionId(@Param("sessionId") String sessionId);
}