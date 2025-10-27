//package com.example.SafeTag_BE.dto;
//
//import com.example.SafeTag_BE.enums.StickerType;
//import com.example.SafeTag_BE.enums.VerificationType;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//@Getter
//@NoArgsConstructor
//public class StickerIssueRequestDto {
//
//    @NotBlank
//    private String vehicleNumber;
//
//    @NotNull
//    private StickerType stickerType;
//
//    // optional: null 허용
//    private Integer validDays;
//}

package com.example.SafeTag_BE.dto;

import com.example.SafeTag_BE.enums.VerificationType;
import com.example.SafeTag_BE.enums.VerificationType;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class StickerIssueRequestDto {

    @NotBlank
    private String carNumber;

    @NotNull
    private VerificationType stickerType; // RESIDENT | PREGNANT | DISABLED

    // optional
    private Integer validDays;

    // optional: 토큰으로 대체 가능
    @JsonAlias({"user_id"})
    private Long userId;

    // optional: 템플릿 기본값으로 대체 가능
    @JsonAlias({"image_url"})
    private String imageUrl;
}
