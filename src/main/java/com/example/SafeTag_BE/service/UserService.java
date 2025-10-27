package com.example.SafeTag_BE.service;

import com.example.SafeTag_BE.entity.User;
import com.example.SafeTag_BE.exception.ApiException;
import com.example.SafeTag_BE.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    public User create(String name, String username, String password,
                       String phoneNum, LocalDate birthDate,
                       String gender, String address, String vehicleNumber) {
        if (vehicleNumber == null || vehicleNumber.isBlank()) {
            throw new IllegalArgumentException("차량번호는 필수입니다.");
        }
        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhoneNum(phoneNum);
        user.setBirthDate(birthDate);
        user.setGender(gender);
        user.setAddress(address);
        user.setCarNumber(vehicleNumber.trim());
        //user.setRole(RoleConstants.ROLE_USER); // 권한 설정
        return userRepository.save(user);
    }

    // 사용자 ID(고유번호)로 조회
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND","사용자를 찾을 수 없습니다."));
    }

    // 사용자 username으로 조회
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException("NOT_FOUND","사용자를 찾을 수 없습니다."));
    }

    // 회원정보 수정
    public User updateUser(Long id, String name, String password,
                           String phoneNum, LocalDate birthDate,
                           String gender, String address) {
        User user = getUser(id);
        user.setName(name);
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setPhoneNum(phoneNum);
        user.setBirthDate(birthDate);
        user.setGender(gender);
        user.setAddress(address);
        return userRepository.save(user);
    }

    // 로그인
    public User login(String username, String password) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user;
            }
        }
        return null; // 로그인 실패
    }

    //회원탈퇴
    public void deleteUser(Long id) {
        User user = getUser(id); // 존재하지 않으면 예외 발생
        userRepository.deleteById(id);
    }

    @Transactional
    public void updateFcmToken(Long userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setFcmToken(fcmToken);
    }

}
