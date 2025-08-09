package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.dto.AccessLogRequestDto;
import com.example.SafeTag_BE.entity.AccessLog;
import com.example.SafeTag_BE.service.AccessLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Access Log API", description = "QR 접속 로그 저장 및 조회 API")
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class AccessLogController {

    private final AccessLogService logService;

    @Operation(summary = "접속 로그 저장", description = "QR을 스캔한 사용자 정보를 로그로 저장합니다.")
    @PostMapping
    public ResponseEntity<?> saveLog(@RequestBody AccessLogRequestDto dto){
        logService.saveLog(dto);
        return ResponseEntity.ok("로그 저장 완료");
    }

    @Operation(summary = "QR별 접속 로그 조회", description = "QR ID를 기준으로 접속 로그를 조회합니다.")
    @GetMapping("/qrs/{qrId}/logs")
    public ResponseEntity<List<AccessLog>> getLogs(@PathVariable Long qrId){ // 그냥 id 인가 ?
        return ResponseEntity.ok(logService.getLogsByQrId(qrId));
    }
}
