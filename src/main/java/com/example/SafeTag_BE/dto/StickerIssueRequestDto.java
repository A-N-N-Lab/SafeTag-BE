package com.example.SafeTag_BE.dto;

import com.example.SafeTag_BE.domain.VerificationType;
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
    private VerificationType stickerType; // RESIDENT | PREGNANT | DISABLED

    // optional: null 허용
    private Integer validDays;
}
