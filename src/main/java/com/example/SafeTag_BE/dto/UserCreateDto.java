package com.example.SafeTag_BE.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserCreateDto {

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

    @NotEmpty(message = "필수 선택 항목입니다.")
    @Pattern(regexp = "^(남성|여성)$", message = "성별은 '남성' 또는 '여성'만 입력 가능합니다.")
    private String gender;

    private LocalDate birthDate;

    @NotEmpty(message = "필수항목입니다.")
    private String address;

    @Schema(description = "차량번호", example = "12가3456")
    @NotEmpty(message = "차량번호는 필수항목입니다.")
    @Pattern(
            regexp = "^[0-9]{2,3}[가-힣][0-9]{4}$|^[가-힣]{2}[0-9]{2}[가-힣][0-9]{4}$",
            message = "차량번호 형식이 올바르지 않습니다. 예) 12가3456, 서울12가3456"
    )
    private String vehicleNumber;

}
