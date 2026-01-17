package com.insuretech.pms.rfp.dto;

import lombok.Data;

@Data
public class UpdateRfpRequest {
    private String title;
    private String content;
    private String status;
}
