package com.example.SafeTag_BE.service;

import com.example.SafeTag_BE.domain.VerificationStatus;
import com.example.SafeTag_BE.domain.VerificationType;
import com.example.SafeTag_BE.entity.Verification;
import com.example.SafeTag_BE.exception.ApiException;
import com.example.SafeTag_BE.repository.UserRepository;
import com.example.SafeTag_BE.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OcrService {

    private final VerificationRepository verificationRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.base-dir:./uploads}")
    private String uploadBaseDir;

    @Value("${app.ocr.base-url:http://localhost:8000}")
    private String ocrBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public void sendToWorker(Long verificationId, String fileId, VerificationType type) {
        try {
            // FastAPI /ocr 호출
            String url = ocrBaseUrl + "/ocr";
            FileSystemResource resource = new FileSystemResource(uploadBaseDir + "/" + fileId);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<Map> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new IllegalStateException("OCR 서버 응답이 유효하지 않습니다.");
            }

            Object textObj = resp.getBody().get("text");
            String text = textObj == null ? "" : String.valueOf(textObj);

            // 타입별 키워드 + 최소 길이로 승인/반려 판정
            boolean approve = isApprovedByType(text, type) && text.strip().length() > 10;

            applyDecision(verificationId, approve, approve ? "OCR 승인" : "OCR 반려: 문서 인식 실패");

        } catch (Exception e) {
            applyDecision(verificationId, false, "OCR 호출 실패: " + e.getMessage());
        }
    }

    private boolean isApprovedByType(String text, VerificationType type) {
        String t = text == null ? "" : text.toLowerCase();

        return switch (type) {
            case RESIDENT -> (
                    t.contains("주민등록") || t.contains("등본") ||
                            t.contains("resident") || t.contains("address") || t.contains("registration")
            );
            case PREGNANT -> (
                    t.contains("임산부") || t.contains("산모수첩") || t.contains("진단서") ||
                            t.contains("pregnancy") || t.contains("obstetric") || t.contains("maternity")
            );
            case DISABLED -> (
                    t.contains("장애인") || t.contains("복지카드") || t.contains("장애인등록증") ||
                            t.contains("disability") || t.contains("welfare") || t.contains("handicap")
            );
        };
    }

    private void applyDecision(Long verificationId, boolean approve, String reason) {
        Verification v = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "인증 요청을 찾을 수 없습니다."));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName(); // 로그인한 계정 아이디
        var reviewer = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "리뷰어를 찾을 수 없습니다."));

        v.setReviewer(reviewer);
        v.setStatus(approve ? VerificationStatus.APPROVED : VerificationStatus.REJECTED);
        v.setReason(reason);
        v.setReviewedAt(LocalDateTime.now());
    }
}
