package com.example.SafeTag_BE.service;

import com.example.SafeTag_BE.dto.QrViewResponseDto;
import com.example.SafeTag_BE.entity.DynamicQR;
import com.example.SafeTag_BE.exception.QrNotFoundException;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import com.example.SafeTag_BE.entity.User;
import com.example.SafeTag_BE.repository.DynamicQRRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QrViewService {
    private final DynamicQRRepository qrRepo;
    private final RelayTicketService relayTicketService; // 일회성 티켓 발급용(내부)
    private final PermitService permitService;           // 스티커/허가 조회(내부)


    @Transactional(readOnly = true)
    public QrViewResponseDto view(Long qrId, Authentication auth){
        DynamicQR qr = qrRepo.findById(qrId)
                .orElseThrow(QrNotFoundException::new);

        boolean valid = qr.getExpiredAt() == null
                || qr.getExpiredAt().isAfter(LocalDateTime.now());

        User owner = qr.getUser(); // null 가능
        Long ownerId = (owner != null ? owner.getId():null);

        String vehicle = (owner != null && owner.getVehicleNumber() != null)
                ? owner.getVehicleNumber()
                : "UNKNOWN";
        String vehicleMask = maskVehicle(vehicle);

        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        // PUBLIC (비로그인/일반 사용자)
        if (!isAdmin) {
            RelayTicketService.Ticket call = relayTicketService.issue(qrId, "CALL");
            RelayTicketService.Ticket msg  = relayTicketService.issue(qrId, "MSG");
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
        // ADMIN (관리자) ? owner가 없을 수도 있으니 방어
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
                            .note("NO_USER") // or "UNASSIGNED_QR"
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
