package com.example.SafeTag_BE.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QrResponseDto {
    private Long qrId;
    private String qrValue;
    private String expiredAt;

}
