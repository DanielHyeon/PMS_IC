package com.insuretech.pms.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String sessionId;
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