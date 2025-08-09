package com.example.SafeTag_BE.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
public class Admin {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String phoneNum;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    private String company;

    public String getRole() {
        return "ROLE_ADMIN";
    }
}
