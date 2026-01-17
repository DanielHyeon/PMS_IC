package com.insuretech.pms.rfp.dto;

import lombok.Data;

@Data
public class CreateRfpRequest {
    private String title;
    private String content;
    private String status;
    private String processingStatus;
}
