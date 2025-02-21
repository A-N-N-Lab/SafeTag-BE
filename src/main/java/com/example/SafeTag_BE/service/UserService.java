package com.example.SafeTag_BE.service;

import com.example.SafeTag_BE.dto.UserResponseDto;
import com.example.SafeTag_BE.entity.SiteUser;
import com.example.SafeTag_BE.exception.DataNotFoundException;
import com.example.SafeTag_BE.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 🔥 회원가입
     */
    public SiteUser create(String username, String email, String password, String gender, String phoneNum) {
        SiteUser user = new SiteUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setGender(gender);
        user.setPhoneNum(phoneNum);
        this.userRepository.save(user);
        return user;
    }

    /**
     * 🔥 사용자 정보 가져오기
     */
    public SiteUser getUser(String username) {
        Optional<SiteUser> siteUser = this.userRepository.findByUsername(username);
        if (siteUser.isPresent()) {
            return siteUser.get();
        } else {
            throw new DataNotFoundException("사용자를 찾을 수 없습니다.");
        }
    }

    /**
     * 🔥 회원정보 수정
     */
    public SiteUser updateUser(String username, String email, String password, String gender, String phoneNum, String carNumber, String apartmentInfo) {
        SiteUser user = getUser(username);
        user.setEmail(email);
        if (password != null && !password.isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setGender(gender);
        user.setPhoneNum(phoneNum);
        user.setCarNumber(carNumber);
        user.setApartmentInfo(apartmentInfo);
        this.userRepository.save(user);
        return user;
    }

    /**
     * 🔥 로그인
     */
    public UserResponseDto login(String username, String password) {
        Optional<SiteUser> optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isPresent()) {
            SiteUser user = optionalUser.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return new UserResponseDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getGender(),
                        user.getPhoneNum()
                );
            }
        }

        // 로그인 실패 시 null 반환
        return null;
    }
}
