package com.example.SafeTag_BE.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AdminCreateDto {

    @NotEmpty(message = "필수항목입니다.")
    @Size(min = 2, max = 25)
    private String name;

    @NotEmpty(message = "필수항목입니다.")
    @Size(min = 3, max = 25)
    private String username;

    @NotEmpty(message = "필수항목입니다.")
    private String password1;

    @NotEmpty(message = "필수항목입니다.")
    private String password2;

    @NotEmpty(message = "필수항목입니다.")
    @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식은 '010-0000-0000' 입니다.")
    private String phoneNum;

    private LocalDate birthDate;

    @NotEmpty(message = "필수 선택 항목입니다.")
    @Pattern(regexp = "^(남성|여성)$", message = "성별은 '남성' 또는 '여성'만 입력 가능합니다.")
    private String gender;

    @NotEmpty(message = "필수항목입니다.")
    private String company;
}
