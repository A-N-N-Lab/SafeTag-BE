//package com.example.SafeTag_BE.dto;
//
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//
//@Getter
//@NoArgsConstructor
//@AllArgsConstructor
//public class StickerIssueResponseDto {
//    private Long stickerId;
//    private String qr;
//    private LocalDateTime expiresAt;
//}

package com.example.SafeTag_BE.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter @NoArgsConstructor @AllArgsConstructor
public class StickerIssueResponseDto {
    private Long stickerId;
    private String qr;
    private LocalDateTime expiresAt;
    private String imageUrl;
    private String type; // VerificationType.name()
}
