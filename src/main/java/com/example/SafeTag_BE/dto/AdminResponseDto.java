package com.example.SafeTag_BE.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class AdminResponseDto {
    private Long id;
    private String name;
    private String username;
    private String phoneNum;
    private LocalDate birthDate;
    private String gender;
    private String company;
}
