package com.example.SafeTag_BE.service;

import com.example.SafeTag_BE.dto.QrViewResponseDto;
import com.example.SafeTag_BE.entity.DynamicQR;
import com.example.SafeTag_BE.entity.FcmToken;
import com.example.SafeTag_BE.entity.User;
import com.example.SafeTag_BE.exception.QrNotFoundException;
import com.example.SafeTag_BE.repository.DynamicQRRepository;
import com.example.SafeTag_BE.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrViewService {

    private final DynamicQRRepository qrRepo;
    private final RelayTicketService relayTicketService; // 일회성 티켓 발급(내부)
    private final PermitService permitService;           // 스티커/허가 조회(내부)

    private final FcmService fcmService;                 // FCM 발송
    private final FcmTokenRepository fcmTokenRepository; // 차주 FCM 토큰 조회

    @Transactional(readOnly = true)
    public QrViewResponseDto view(Long qrId, Authentication auth) {
        DynamicQR qr = qrRepo.findById(qrId)
                .orElseThrow(QrNotFoundException::new);

        boolean valid = qr.getExpiredAt() == null
                || qr.getExpiredAt().isAfter(LocalDateTime.now());

        User owner = qr.getUser(); // null 가능
        Long ownerId = (owner != null ? owner.getId() : null);

        String vehicle = (owner != null && owner.getCarNumber() != null)
                ? owner.getCarNumber()
                : "UNKNOWN";
        String vehicleMask = maskVehicle(vehicle);

        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin) {
            // 1) 티켓 발급
            RelayTicketService.Ticket call = relayTicketService.issue(qrId, "CALL");
            RelayTicketService.Ticket msg  = relayTicketService.issue(qrId, "MSG");

            // 2) 차주에게 푸시 (sessionId = qrId 문자열 사용)
            if (ownerId != null) {
                try {
                    List<FcmToken> tokens =
                            fcmTokenRepository.findAllByUserIdAndActiveTrue(ownerId);

                    if (!tokens.isEmpty()) {
                        final String sessionId = String.valueOf(qrId); // 통일해두ㅏ
                        for (FcmToken t : tokens) {
                            String tok = t.getToken();
                            if (tok != null && !tok.isBlank()) {
                                fcmService.sendCallRequest(
                                        tok,
                                        (owner != null ? owner.getName() : "차주"),
                                        sessionId
                                );
                            }
                        }
                        log.info("[FCM] 통화요청 푸시 완료 ownerId={}, qrId={}, tokens={}",
                                ownerId, qrId, tokens.size());
                    } else {
                        log.info("[FCM] 활성 토큰 없음 ownerId={}, qrId={}", ownerId, qrId);
                    }
                } catch (Exception e) {
                    log.warn("[FCM] 통화요청 발송 오류(무시) ownerId={}, qrId={}, err={}",
                            ownerId, qrId, e.toString());
                }
            } else {
                log.info("[FCM] QR에 연결된 소유자 없음 qrId={}", qrId);
            }

            // 3) 응답
            return QrViewResponseDto.builder()
                    .mode("PUBLIC")
                    .qrId(qrId)
                    .valid(valid)
                    .vehicleMask(vehicleMask)
                    .relay(QrViewResponseDto.RelayInfo.builder()
                            .callTicket(call.token())
                            .msgTicket(msg.token())
                            .ttlSec(call.ttlSec())
                            .build())
                    .build();
        }

        // admin
        if (ownerId == null) {
            return QrViewResponseDto.builder()
                    .mode("ADMIN")
                    .qrId(qrId)
                    .valid(valid)
                    .vehicleMask(vehicleMask)
                    .admin(QrViewResponseDto.AdminInfo.builder()
                            .resident(false)
                            .maternity(false)
                            .disabled(false)
                            .ownerMask(null)
                            .buildingUnitMask(null)
                            .note("NO_USER")
                            .build())
                    .build();
        }

        PermitService.Result p = permitService.checkAll(ownerId);
        return QrViewResponseDto.builder()
                .mode("ADMIN")
                .qrId(qrId)
                .valid(valid)
                .vehicleMask(vehicleMask)
                .admin(QrViewResponseDto.AdminInfo.builder()
                        .resident(p.isResident())
                        .maternity(p.isMaternity())
                        .disabled(p.isDisabled())
                        .stickerNoMasked(p.getStickerNoMasked())
                        .ownerMask(maskName(owner.getName()))
                        .buildingUnitMask(maskBuildingUnit(p.getBuildingUnit()))
                        .note(p.getReason())
                        .build())
                .build();
    }

    private String maskVehicle(String v) {
        if (v == null || v.length() < 4) return "****";
        int show = Math.min(4, v.length());
        return v.substring(0, show) + "***";
    }

    private String maskName(String n) {
        if (n == null || n.isBlank()) return null;
        if (n.length() == 1) return n;
        if (n.length() == 2) return n.charAt(0) + "*";
        return n.charAt(0) + "*" + n.substring(2);
    }

    private String maskBuildingUnit(String u) {
        if (u == null) return null;
        // 예: "101동 202호" -> "101동 2**호"
        return u.replaceAll("(\\d)(\\d)(\\d?호?)$", "$1**호");
    }
}
