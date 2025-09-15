package com.example.SafeTag_BE.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OtpIssueResponseDto {
    private String otp;
    private LocalDateTime expiresAt;
}
