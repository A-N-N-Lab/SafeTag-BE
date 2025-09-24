package com.example.SafeTag_BE.service;

import com.example.SafeTag_BE.entity.Admin;
import com.example.SafeTag_BE.exception.ApiException;
import com.example.SafeTag_BE.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    public Admin create(String name, String username, String password,
                        String phoneNum, LocalDate birthDate,
                        String gender, String company) {
        Admin admin = new Admin();
        admin.setName(name);
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setPhoneNum(phoneNum);
        admin.setBirthDate(birthDate);
        admin.setGender(gender);
        admin.setCompany(company);
        //admin.setRole(RoleConstants.ROLE_ADMIN); // 권한 설정
        return adminRepository.save(admin);
    }

    // 관리자 ID(고유번호)로 조회
    public Admin getAdmin(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND","관리자를 찾을 수 없습니다."));
    }

    // 관리자 username으로 조회
    public Admin getAdminByUsername(String username) {
        return adminRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException("NOT_FOUND","관리자를 찾을 수 없습니다."));
    }

    // 관리자 정보 수정
    public Admin updateAdmin(Long id, String name, String password,
                             String phoneNum, LocalDate birthDate,
                             String gender, String company) {
        Admin admin = getAdmin(id);
        admin.setName(name);
        if (password != null && !password.isEmpty()) {
            admin.setPassword(passwordEncoder.encode(password));
        }
        admin.setPhoneNum(phoneNum);
        admin.setBirthDate(birthDate);
        admin.setGender(gender);
        admin.setCompany(company);
        return adminRepository.save(admin);
    }

    // 로그인
    public Admin login(String username, String password) {
        Optional<Admin> optionalAdmin = adminRepository.findByUsername(username);
        if (optionalAdmin.isPresent()) {
            Admin admin = optionalAdmin.get();
            if (passwordEncoder.matches(password, admin.getPassword())) {
                return admin;
            }
        }
        return null; // 로그인 실패
    }

    // 관리자 탈퇴
    public void deleteAdmin(Long id) {
        Admin admin = getAdmin(id); // 존재하지 않으면 예외 발생
        adminRepository.delete(admin);
    }

}
