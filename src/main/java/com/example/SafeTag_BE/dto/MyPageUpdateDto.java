package com.example.SafeTag_BE.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MyPageUpdateDto {

    @NotEmpty(message = "사용자ID는 필수항목입니다.")
    private String username;

    @NotEmpty(message = "이메일은 필수항목입니다.")
    @Email(message = "올바른 이메일 형식을 입력해주세요.")
    private String email;

    private String password;

    @NotEmpty(message = "성별은 필수 선택 항목입니다.")
    @Pattern(regexp = "^(남성|여성)$", message = "성별은 '남성' 또는 '여성'만 입력 가능합니다.")
    private String gender;

    @NotEmpty(message = "전화번호는 필수항목입니다.")
    @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식은 '010-1234-5678' 이어야 합니다.")
    private String phoneNum;

    private String carNumber;

    private String apartmentInfo;
}
