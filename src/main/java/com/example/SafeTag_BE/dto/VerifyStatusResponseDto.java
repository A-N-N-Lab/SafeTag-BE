package com.example.SafeTag_BE.dto;

import com.example.SafeTag_BE.domain.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyStatusResponseDto {
    private Long verificationId;
    private VerificationStatus status; // PENDING / APPROVED / REJECTED
    private String reason;
    private LocalDateTime reviewedAt;
}
