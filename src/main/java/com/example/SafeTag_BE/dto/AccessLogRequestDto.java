package com.example.SafeTag_BE.dto;

import lombok.Data;

@Data
public class AccessLogRequestDto {
    private Long qrId;
    private String ip;
    private String userAgent;
}
