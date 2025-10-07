package com.example.SafeTag_BE.service;

import com.example.SafeTag_BE.exception.QrGoneException;
import com.example.SafeTag_BE.dto.CreateQrRequestDto;
import com.example.SafeTag_BE.dto.QrResponseDto;
import com.example.SafeTag_BE.entity.DynamicQR;
import com.example.SafeTag_BE.entity.User;
import com.example.SafeTag_BE.repository.DynamicQRRepository;
import com.example.SafeTag_BE.repository.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QrService {

    private final DynamicQRRepository qrRepository;
    private final UserRepository userRepository;

    private static final int QR_SIZE = 250;        // PNG 픽셀 크기
    private static final long TTL_SECONDS = 60L;   // 60초 만료


    //사용자 기준 최신 QR을 가져오되, 없거나 만료가 임박(<=10s)하면 새로 발급
    @Transactional
    public DynamicQR issueOrRotate(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));

        LocalDateTime now = LocalDateTime.now();
        Optional<DynamicQR> latestOpt = qrRepository.findTop1ByUserOrderByGeneratedAtDesc(user);

        if (latestOpt.isEmpty()) {
            return saveNewQr(user, now);
        }

        DynamicQR latest = latestOpt.get();
        if (isExpired(latest, now) || secondsLeft(latest, now) <= 10) {
            return saveNewQr(user, now);
        }

        return latest;
    }


    // QR 메타 정보(만료시각, TTL)를 반환. 만료 시 QrGoneException → 410으로 매핑할 것.
    @Transactional(readOnly = true)
    public Map<String, Object> meta(Long qrId) {
        DynamicQR qr = getValidByIdOrGone(qrId);
        long ttl = Duration.between(LocalDateTime.now(), qr.getExpiredAt()).toSeconds();
        return Map.of(
                "qrId", qr.getId(),
                "qrValue", qr.getQrValue(), // 필요 시 노출 제거/마스킹
                "generatedAt", String.valueOf(qr.getGeneratedAt()),
                "expiredAt", String.valueOf(qr.getExpiredAt()),
                "ttlSeconds", ttl
        );
    }


     // ID로 조회 + 만료 체크. 만료면 410용 QrGoneException.
    @Transactional(readOnly = true)
    public DynamicQR getValidByIdOrGone(Long qrId) {
        DynamicQR qr = qrRepository.findById(qrId)
                .orElseThrow(() -> new IllegalArgumentException("QR ID 없음"));
        if (isExpired(qr, LocalDateTime.now())) {
            throw new QrGoneException("QR_EXPIRED");
        }
        return qr;
    }

    //  기존 기능 유지(필요시 호출)
    @Transactional
    public QrResponseDto createQr(CreateQrRequestDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));

        LocalDateTime now = LocalDateTime.now();
        DynamicQR qr = DynamicQR.builder()
                .qrValue(newQrValue())
                .user(user)
                .generatedAt(now)
                .expiredAt(now.plusSeconds(TTL_SECONDS))
                .build();

        qrRepository.save(qr);

        return new QrResponseDto(
                qr.getId(),
                qr.getQrValue(),
                qr.getExpiredAt().toString()
        );
    }

     // QR PNG 이미지 바이트 생성

    @Transactional(readOnly = true)
    public byte[] generateQrImage(String qrValue) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(qrValue, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        }
    }


     // 단순 조회(만료 체크 없음). 이미지/메타에는 getValidByIdOrGone() 사용 권장.
    @Transactional(readOnly = true)
    public DynamicQR getQrById(Long qrId) {
        return qrRepository.findById(qrId)
                .orElseThrow(() -> new IllegalArgumentException("QR ID 없음"));
    }


     // 중계용 마스킹 전화번호 조회(만료 시 IllegalStateException)

    @Transactional(readOnly = true)
    public String getProxyPhoneNumber(String qrValue) {
        DynamicQR qr = qrRepository.findByQrValue(qrValue)
                .orElseThrow(() -> new IllegalArgumentException("QR값이 유효하지 않음"));

        if (isExpired(qr, LocalDateTime.now())) {
            throw new IllegalStateException("QR이 만료되었습니다.");
        }

        User user = qr.getUser();
        // 예: 010-****-5678
        return user.getPhoneNum().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1-****-$2");
    }


     // QR 문자열로 유효성 검증

    @Transactional(readOnly = true)
    public Map<String, Object> validate(String code) {
        DynamicQR qr = qrRepository.findByQrValue(code)
                .orElseThrow(() -> new IllegalArgumentException("NOT_FOUND"));

        if (isExpired(qr, LocalDateTime.now())) {
            return Map.of("valid", false, "reason", "EXPIRED");
        }
        return Map.of(
                "valid", true,
                "ownerId", qr.getUser().getId(),
                "qrId", qr.getId()
        );
    }

    private DynamicQR saveNewQr(User user, LocalDateTime now) {
        DynamicQR qr = DynamicQR.builder()
                .qrValue(newQrValue())
                .user(user)
                .generatedAt(now)
                .expiredAt(now.plusSeconds(TTL_SECONDS))
                .build();
        return qrRepository.save(qr);
    }

    private boolean isExpired(DynamicQR qr, LocalDateTime now) {
        return qr.getExpiredAt() != null && qr.getExpiredAt().isBefore(now);
    }

    private long secondsLeft(DynamicQR qr, LocalDateTime now) {
        return Math.max(0, Duration.between(now, qr.getExpiredAt()).toSeconds());
    }

    private String newQrValue() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}

