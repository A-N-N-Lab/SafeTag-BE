package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.dto.CreateQrRequestDto;
import com.example.SafeTag_BE.entity.DynamicQR;
import com.example.SafeTag_BE.service.QrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "QR API", description = "QR 생성, 이미지 조회, 중계 호출 관련 API")
@RestController
@RequestMapping("/api/qrs")
@RequiredArgsConstructor
public class QrController {

    private final QrService qrService;

    @Operation(summary = "QR 생성", description = "사용자 ID를 받아 QR 코드를 생성합니다.")
    @PostMapping
    public ResponseEntity<?> createQr(@RequestBody CreateQrRequestDto dto ){
        return ResponseEntity.ok(qrService.createQr(dto));
    }

    @Operation(summary = "QR 이미지 조회", description = "QR ID를 기반으로 이미지(PNG)를 반환합니다.")
    @GetMapping("/{qrId}/image")
    public ResponseEntity<byte[]> getQrImage(@PathVariable Long qrId)throws Exception{
        DynamicQR qr = qrService.getQrById(qrId); // 직접 메서드 추가해야함 (추후)
        byte[] imageBytes = qrService.generateQrImage(qr.getQrValue());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }


    @Operation(summary = "QR 중계 전화번호 요청", description = "QR 값을 기반으로 중계용 전화번호를 반환합니다.")
    @GetMapping("/proxy-call")
    public ResponseEntity<String> proxyCall(@RequestParam String qrValue){
        try {
            String phone = qrService.getProxyPhoneNumber(qrValue);
            return ResponseEntity.ok("중계 연결용 전화번호: " + phone);
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "QR 유효성 확인", description = "QR code 문자열로 유효성/만료를 확인합니다.")
    @GetMapping("/{code}")
    public ResponseEntity<?> validateByCode(@PathVariable String code) {
        try {
            return ResponseEntity.ok(qrService.validate(code));
        } catch (IllegalArgumentException e) {
            // NOT_FOUND 등
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("valid", false, "reason", e.getMessage()));
        }
    }


}
