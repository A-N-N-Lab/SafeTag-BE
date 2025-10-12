package com.example.SafeTag_BE.service;

import com.example.SafeTag_BE.enums.VerificationStatus;
import com.example.SafeTag_BE.dto.VerifyStartRequestDto;
import com.example.SafeTag_BE.dto.VerifyStartResponseDto;
import com.example.SafeTag_BE.dto.VerifyStatusResponseDto;
import com.example.SafeTag_BE.entity.User;
import com.example.SafeTag_BE.entity.Verification;
import com.example.SafeTag_BE.exception.ApiException;
import com.example.SafeTag_BE.repository.UserRepository;
import com.example.SafeTag_BE.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationRepository verificationRepository;
    private final UserRepository userRepository;
    @Lazy private final OcrService ocrService;

    @Transactional
    public VerifyStartResponseDto startVerification(Long userId, VerifyStartRequestDto req) {
        boolean exists = verificationRepository.existsByUserIdAndTypeAndStatus(
                userId, req.getType(), VerificationStatus.PENDING);
        if (exists) throw new ApiException("DUPLICATE_REQUEST", "이미 동일 타입의 심사 대기 요청이 존재합니다.");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "사용자를 찾을 수 없습니다."));

        String fileId = (req.getFileIds() != null && !req.getFileIds().isEmpty())
                ? req.getFileIds().get(0) : null;

        Verification v = Verification.builder()
                .user(user)
                .type(req.getType())
                .status(VerificationStatus.PENDING)
                .fileId(fileId)
                .build();

        Verification saved = verificationRepository.save(v);

        // 비동기 OCR
        ocrService.sendToWorker(saved.getId(), fileId, req.getType());

        return new VerifyStartResponseDto(saved.getId(), saved.getStatus());
    }

    @Transactional(readOnly = true)
    public VerifyStatusResponseDto getVerificationStatus(Long verificationId, Long userIdOrNullIfAdmin) {
        Verification v = (userIdOrNullIfAdmin == null)
                ? verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "인증 요청을 찾을 수 없습니다."))
                : verificationRepository.findByIdAndUserId(verificationId, userIdOrNullIfAdmin)
                        .orElseThrow(() -> new ApiException("FORBIDDEN", "요청을 찾을 수 없거나 권한이 없습니다."));

        return new VerifyStatusResponseDto(
                v.getId(),
                v.getStatus(),
                v.getReason(),
                v.getReviewedAt()
        );
    }

    @Transactional
    public void approveVerification(Long verificationId, Long adminUserId, String reason) {
        Verification v = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "인증 요청을 찾을 수 없습니다."));
        User reviewer = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "관리자 계정을 찾을 수 없습니다."));

        v.setReviewer(reviewer);
        v.setStatus(VerificationStatus.APPROVED);
        v.setReason(reason);
        v.setReviewedAt(LocalDateTime.now());
    }

    @Transactional
    public void rejectVerification(Long verificationId, Long adminUserId, String reason) {
        Verification v = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "인증 요청을 찾을 수 없습니다."));
        User reviewer = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "관리자 계정을 찾을 수 없습니다."));

        v.setReviewer(reviewer);
        v.setStatus(VerificationStatus.REJECTED);
        v.setReason(reason);
        v.setReviewedAt(LocalDateTime.now());
    }
}
