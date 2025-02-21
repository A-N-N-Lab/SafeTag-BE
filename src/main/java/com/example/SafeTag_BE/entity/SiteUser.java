package com.example.SafeTag_BE.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class SiteUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String gender;

    @Column(unique = true, nullable = false)
    private String phoneNum;

    @Column(unique = true, nullable = true)
    private String carNumber;

    @Column(nullable = true)
    private String apartmentInfo;
}
