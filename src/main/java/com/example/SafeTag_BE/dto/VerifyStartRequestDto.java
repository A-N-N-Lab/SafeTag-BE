package com.example.SafeTag_BE.dto;

import com.example.SafeTag_BE.domain.VerificationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import java.util.List;

@Getter
public class VerifyStartRequestDto {
    @NotNull
    private VerificationType type;

    @NotEmpty(message = "최소 한 개의 증빙 파일이 필요합니다.")
    private List<@NotBlank(message = "파일 ID는 비어 있을 수 없습니다.") String> fileIds;
}
