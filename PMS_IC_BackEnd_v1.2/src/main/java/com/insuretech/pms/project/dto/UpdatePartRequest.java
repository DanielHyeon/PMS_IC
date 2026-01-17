package com.insuretech.pms.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePartRequest {

    private String name;

    private String description;

    private String leaderId;

    private String leaderName;

    private String status;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer progress;
}
