package com.example.SafeTag_BE.service;

import com.example.SafeTag_BE.dto.AccessLogRequestDto;
import com.example.SafeTag_BE.entity.AccessLog;
import com.example.SafeTag_BE.repository.AccessLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessLogService {

    private final AccessLogRepository logRepository;

    public void saveLog(AccessLogRequestDto dto ){
        AccessLog log = AccessLog.builder()
                .qrId(dto.getQrId())
                .ip(dto.getIp())
                .userAgent(dto.getUserAgent())
                .scannedAt(LocalDateTime.now())
                .build();
        logRepository.save(log);
    }

    public List<AccessLog> getLogsByQrId(Long qrId){
        return logRepository.findByQrId(qrId);
    }
}
