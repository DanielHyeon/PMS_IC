package com.insuretech.pms.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String sessionId;
    
    @NotBlank(message = "메시지는 필수입니다")
    @Size(min = 1, max = 2000, message = "메시지는 1자 이상 2000자 이하여야 합니다")
    private String message;
    
    private List<MessageContext> context;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageContext {
        private String role;
        private String content;
    }
}