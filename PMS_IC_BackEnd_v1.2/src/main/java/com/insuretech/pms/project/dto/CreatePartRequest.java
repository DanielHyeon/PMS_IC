package com.insuretech.pms.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePartRequest {

    @NotBlank(message = "Part name is required")
    private String name;

    private String description;

    private String leaderId;

    private String leaderName;

    private LocalDate startDate;

    private LocalDate endDate;
}
