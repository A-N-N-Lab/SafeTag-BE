package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.dto.QrViewResponseDto;
import com.example.SafeTag_BE.service.QrViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/qrs")
@RequiredArgsConstructor
public class QrviewController {

    private final QrViewService qrViewService;

    // Authorization 유무 상관없이 접근 허용 (SecurityConfig에서 permitAll)
    @GetMapping("/{qrId}/view")
    public ResponseEntity<?>view(@PathVariable Long qrId, Authentication auth){
        QrViewResponseDto body = qrViewService.view(qrId, auth);
        // return ResponseEntity.ok(ApiResponse.success(ResponseCode.OK, body)); // 래퍼 사용 시
        return ResponseEntity.ok(body);
    }
}
