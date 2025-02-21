package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.dto.MyPageUpdateDto;
import com.example.SafeTag_BE.entity.SiteUser;
import com.example.SafeTag_BE.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RequiredArgsConstructor
@RestController  // 🔥 @Controller → @RestController 로 변경
@RequestMapping("/api/mypage")
@Tag(name = "MyPage API", description = "마이페이지 정보 조회 및 수정 API")
public class MyPageController {

    private final UserService userService;

    /**
     * 🔥 마이페이지 정보 조회 API
     */
    @GetMapping("")
    @Operation(summary = "마이페이지 조회", description = "현재 로그인한 사용자의 마이페이지 정보를 조회합니다.")
    public ResponseEntity<SiteUser> myPageView(Principal principal) {
        SiteUser user = userService.getUser(principal.getName());
        return ResponseEntity.ok(user);
    }

    /**
     * 🔥 마이페이지 정보 수정 Form 데이터 로드
     */
    @GetMapping("/edit")
    @Operation(summary = "마이페이지 수정 폼 데이터", description = "마이페이지 수정 폼에 기존 데이터를 로드합니다.")
    public ResponseEntity<MyPageUpdateDto> myPageForm(Principal principal) {
        SiteUser user = userService.getUser(principal.getName());
        MyPageUpdateDto form = new MyPageUpdateDto();
        form.setUsername(user.getUsername());
        form.setEmail(user.getEmail());
        form.setGender(user.getGender());
        form.setPhoneNum(user.getPhoneNum());
        form.setCarNumber(user.getCarNumber());
        form.setApartmentInfo(user.getApartmentInfo());
        return ResponseEntity.ok(form);
    }

    /**
     * 🔥 마이페이지 정보 수정 API
     */
    @PutMapping("/edit")
    @Operation(summary = "마이페이지 수정", description = "마이페이지 정보를 수정합니다.")
    public ResponseEntity<String> updateMyPage(@RequestBody MyPageUpdateDto myPageUpdateForm, Principal principal) {
        try {
            userService.updateUser(
                    principal.getName(),
                    myPageUpdateForm.getEmail(),
                    myPageUpdateForm.getPassword(),
                    myPageUpdateForm.getGender(),
                    myPageUpdateForm.getPhoneNum(),
                    myPageUpdateForm.getCarNumber(),
                    myPageUpdateForm.getApartmentInfo()
            );
            return ResponseEntity.ok("수정 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("수정 실패: " + e.getMessage());
        }
    }
}
