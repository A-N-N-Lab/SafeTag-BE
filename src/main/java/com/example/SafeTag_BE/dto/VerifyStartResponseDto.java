package com.example.SafeTag_BE.dto;

import com.example.SafeTag_BE.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyStartResponseDto {
    private Long verificationId;
    private VerificationStatus status; // PENDING
}
