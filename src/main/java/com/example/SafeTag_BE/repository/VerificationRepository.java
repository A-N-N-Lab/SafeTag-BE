package com.example.SafeTag_BE.repository;

import com.example.SafeTag_BE.domain.VerificationStatus;
import com.example.SafeTag_BE.domain.VerificationType;
import com.example.SafeTag_BE.entity.Verification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationRepository extends JpaRepository<Verification, Long> {

    // 사용자 이력 ,관리자 대기 목록
    Page<Verification> findByUser_Id(Long userId, Pageable pageable);
    Page<Verification> findByStatus(VerificationStatus status, Pageable pageable);

    // 사용자별 최근 1건(상태/타입 무관)
    Optional<Verification> findTopByUser_IdOrderByCreatedAtDesc(Long userId);

    // 사용자 + 타입 기준 최근 1건
    Optional<Verification> findTopByUser_IdAndTypeOrderByCreatedAtDesc(Long userId, VerificationType type);

    // 중복 요청 방지
    boolean existsByUser_IdAndTypeAndStatus(Long userId, VerificationType type, VerificationStatus status);

    // 사용자 소유 단건 조회(권한 체크용)
    Optional<Verification> findByIdAndUser_Id(Long id, Long userId);

    // 상태 + 타입 필터
    Page<Verification> findByStatusAndType(VerificationStatus status, VerificationType type, Pageable pageable);

    // 심사자(관리자)별 내가 처리한 건 조회
    Page<Verification> findByReviewer_Id(Long adminId, Pageable pageable);

}
