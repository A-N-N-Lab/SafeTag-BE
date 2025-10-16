package com.example.SafeTag_BE.dto;

import com.example.SafeTag_BE.domain.VerificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StickerIssueResponseDto {
    private Long stickerId;
    private String qr;
    private LocalDateTime expiresAt;
    private String imageUrl;
    private String type;
}
