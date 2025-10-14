package com.example.SafeTag_BE.dto;

import com.example.SafeTag_BE.enums.StickerType;
import com.example.SafeTag_BE.enums.VerificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StickerIssueRequestDto {

    @NotBlank
    private String vehicleNumber;

    @NotNull
    private StickerType stickerType;

    // optional: null 허용
    private Integer validDays;
}
