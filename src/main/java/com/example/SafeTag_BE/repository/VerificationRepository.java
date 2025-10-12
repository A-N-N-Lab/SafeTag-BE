package com.example.SafeTag_BE.repository;


import com.example.SafeTag_BE.enums.VerificationStatus;
import com.example.SafeTag_BE.enums.VerificationType;
import com.example.SafeTag_BE.entity.Verification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationRepository extends JpaRepository<Verification, Long> {
    // 동일 타입 중복 방지: userId + type + status
    boolean existsByUserIdAndTypeAndStatus(Long userId,
            VerificationType type,
            VerificationStatus status);

    // 본인 요청만 조회할 때 사용
    Optional<Verification> findByIdAndUserId(Long id, Long userId);

}
