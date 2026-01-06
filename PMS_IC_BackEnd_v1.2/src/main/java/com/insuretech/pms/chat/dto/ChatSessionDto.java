package com.insuretech.pms.chat.dto;

import com.insuretech.pms.chat.entity.ChatSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionDto {
    private String id;
    private String userId;
    private String title;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long messageCount; // 메시지 개수 (선택적)

    public static ChatSessionDto from(ChatSession session) {
        return ChatSessionDto.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .title(session.getTitle())
                .active(session.getActive())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}


