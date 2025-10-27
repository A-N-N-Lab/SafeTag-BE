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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QrService {

    private final DynamicQRRepository qrRepository;
    private final UserRepository userRepository;

    private static final int QR_SIZE = 250;
    /** QR 유효기간(초) */
    private static final long TTL_SECONDS = 60L;
    /** 회전 방지 가드(남은 시간이 이 값보다 크면 기존 QR 재사용) */
    private static final long ROTATE_GUARD_SECONDS = 10L;

    private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;

    /* ========================= 발급/회전 ========================= */

    @Transactional
    public DynamicQR issueOrRotate(Long userId) {
        return issueOrRotate(userId, false);
    }

    @Transactional
    public DynamicQR issueOrRotate(Long userId, boolean force) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        LocalDateTime now = LocalDateTime.now();
        Optional<DynamicQR> latestOpt = qrRepository.findTop1ByUserOrderByGeneratedAtDesc(user);

        if (!force && latestOpt.isPresent()) {
            DynamicQR latest = latestOpt.get();
            long left = secondsLeft(latest, now);
            // 만료 안됐고 남은 시간이 가드보다 크면 기존 유지
            if (left > ROTATE_GUARD_SECONDS) {
                return latest;
            }
        }
        return saveNewQr(user, now);
    }

    /* ========================= 조회/검증 ========================= */

    @Transactional(readOnly = true)
    public Map<String, Object> meta(Long qrId) {
        DynamicQR qr = getValidByIdOrGone(qrId);

        LocalDateTime now = LocalDateTime.now();
        long ttl = Math.max(0L, Duration.between(now, qr.getExpiredAt()).getSeconds());

        // 컨트롤러/라즈파이가 쓰기 편하도록 ISO-8601(UTC)도 함께 제공
        Instant expUtc = toUtcInstant(qr.getExpiredAt());
        Instant genUtc = toUtcInstant(qr.getGeneratedAt());

        return Map.of(
                "qrId", qr.getId(),
                "qrValue", qr.getQrValue(),
                "generatedAt", String.valueOf(qr.getGeneratedAt()),   // 기존 호환
                "expiredAt", String.valueOf(qr.getExpiredAt()),       // 기존 호환
                "generatedAtIso", genUtc != null ? ISO_INSTANT.format(genUtc) : null,
                "expiredAtIso", expUtc != null ? ISO_INSTANT.format(expUtc) : null,
                "ttlSeconds", ttl
        );
    }

    @Transactional(readOnly = true)
    public DynamicQR getValidByIdOrGone(Long qrId) {
        DynamicQR qr = qrRepository.findById(qrId)
                .orElseThrow(() -> new IllegalArgumentException("QR ID 없음"));
        if (isExpired(qr, LocalDateTime.now())) {
            throw new QrGoneException("QR_EXPIRED");
        }
        return qr;
    }

    @Transactional(readOnly = true)
    public DynamicQR getQrById(Long qrId) {
        return qrRepository.findById(qrId)
                .orElseThrow(() -> new IllegalArgumentException("QR ID 없음"));
    }

    @Transactional(readOnly = true)
    public String getProxyPhoneNumber(String qrValue) {
        DynamicQR qr = qrRepository.findByQrValue(qrValue)
                .orElseThrow(() -> new IllegalArgumentException("QR값이 유효하지 않음"));

        if (isExpired(qr, LocalDateTime.now())) {
            throw new IllegalStateException("QR이 만료되었습니다.");
        }
        // 010-****-1234 형태로 마스킹
        return qr.getUser().getPhoneNum().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1-****-$2");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> validate(String code) {
        DynamicQR qr = qrRepository.findByQrValue(code)
                .orElseThrow(() -> new IllegalArgumentException("NOT_FOUND"));

        if (isExpired(qr, LocalDateTime.now())) {
            return Map.of("valid", false, "reason", "EXPIRED");
        }
        return Map.of("valid", true, "ownerId", qr.getUser().getId(), "qrId", qr.getId());
    }

    /* ========================= 생성/이미지 ========================= */

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

        return new QrResponseDto(qr.getId(), qr.getQrValue(), qr.getExpiredAt().toString());
    }

    @Transactional(readOnly = true)
    public byte[] generateQrImage(String qrValue) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(qrValue, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        }
    }

    /* ========================= 내부 유틸 ========================= */

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
        if (qr.getExpiredAt() == null) return 0L;
        long s = Duration.between(now, qr.getExpiredAt()).getSeconds();
        return Math.max(0L, s);
    }

    private String newQrValue() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** LocalDateTime(시스템 타임존 가정) → UTC Instant 변환 */
    private Instant toUtcInstant(LocalDateTime ldt) {
        if (ldt == null) return null;
        // 서버 시스템 타임존을 기준으로 잡고 UTC Instant로 변환
        return ldt.atZone(ZoneId.systemDefault()).toInstant();
    }
}
