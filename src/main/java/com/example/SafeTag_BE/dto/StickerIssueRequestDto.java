package com.example.SafeTag_BE.dto;

import com.example.SafeTag_BE.domain.VerificationType;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class StickerIssueRequestDto {

    @NotBlank
    private String carNumber;
    @NotNull
    private VerificationType stickerType; // RESIDENT | PREGNANT | DISABLED
    private Integer validDays;
    @JsonAlias({"user_id"})
    private Long userId;
    @JsonAlias({"image_url"})
    private String imageUrl;
}
