package com.insuretech.pms.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WipValidationResult {

    private boolean valid;
    private WipViolationType violationType;
    private String affectedColumn;
    private int currentWipCount;
    private int limit;
    private List<String> suggestions;
    private String message;

    public enum WipViolationType {
        COLUMN_SOFT_LIMIT,
        COLUMN_HARD_LIMIT,
        SPRINT_CONWIP_LIMIT,
        PERSONAL_WIP_LIMIT,
        NONE
    }

    public static WipValidationResult valid() {
        return WipValidationResult.builder()
                .valid(true)
                .violationType(WipViolationType.NONE)
                .suggestions(List.of())
                .message("WIP validation passed")
                .build();
    }

    public static WipValidationResult invalid(WipViolationType type, String column, int current, int limit, String message) {
        return WipValidationResult.builder()
                .valid(false)
                .violationType(type)
                .affectedColumn(column)
                .currentWipCount(current)
                .limit(limit)
                .message(message)
                .suggestions(List.of())
                .build();
    }
}
