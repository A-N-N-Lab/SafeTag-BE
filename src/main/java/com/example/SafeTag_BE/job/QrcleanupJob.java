package com.example.SafeTag_BE.job;

import com.example.SafeTag_BE.repository.DynamicQRRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class QrcleanupJob {

    private final DynamicQRRepository repo;

    // 5분마다 만료된 QR 청소
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void cleanup(){
        int n = repo.deleteAllExpired(LocalDateTime.now().minusSeconds(60));
    }

}
