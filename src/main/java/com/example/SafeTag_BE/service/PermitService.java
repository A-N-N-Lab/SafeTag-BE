//package com.example.SafeTag_BE.service;
//
//import com.example.SafeTag_BE.entity.Sticker;
//import com.example.SafeTag_BE.enums.StickerType;
//import com.example.SafeTag_BE.enums.VerificationStatus;
//import com.example.SafeTag_BE.enums.VerificationType;
//import com.example.SafeTag_BE.repository.StickerRepository;
//import com.example.SafeTag_BE.repository.VerificationRepository;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//public class PermitService {
//
//    private final StickerRepository stickerRepo;
//    private final VerificationRepository verifyRepo;
//
//    @Getter
//    @AllArgsConstructor
//    public static class Result {
//        private final boolean resident;        // 거주민 스티커 보유 여부
//        private final boolean maternity;       // 임산부 인증 보유 여부
//        private final boolean disabled;        // 장애인 인증 보유 여부
//        private final String stickerNoMasked;  // 스티커 번호(마스킹)
//        private final String buildingUnit;     // 동/호 (원문 그대로; 마스킹은 ViewService에서)
//        private final String reason;           // 실패/비고 사유
//    }
//
//    public Result checkAll(Long userId) {
//        if (userId == null) {
//            return new Result(false, false, false, null, null, "NO_USER");
//        }
//
//        LocalDate today = LocalDate.now();
//
//        // 1) 거주민 스티커: 기간 유효 + 요약 정보
//        boolean resident = stickerRepo.hasValidResident(userId, StickerType.RESIDENT, today);
//
//        Optional<Sticker> latestResident = stickerRepo
//                .findLatestResident(userId, StickerType.RESIDENT, PageRequest.of(0, 1))
//                .stream().findFirst(); // 없으면 empty
//
//        String stickerNoMasked = latestResident
//                .map(Sticker::getStickerNo)        // <-- Sticker 엔티티의 필드명에 맞춰주세요
//                .map(this::maskStickerNo)
//                .orElse(null);
//
//        String buildingUnit = latestResident
//                .map(Sticker::getBuildingUnit)     // <-- Sticker 엔티티의 필드명에 맞춰주세요
//                .orElse(null);
//
//        // 2) 임산부/장애인 인증: 기간 유효
//        boolean maternity = verifyRepo.existsByUserIdAndTypeAndStatus(
//                userId, VerificationType.PREGNANT, VerificationStatus.APPROVED);
//
//        boolean disabled  = verifyRepo.existsByUserIdAndTypeAndStatus(
//                userId, VerificationType.DISABLED, VerificationStatus.APPROVED);
//
//
//        return new Result(resident, maternity, disabled, stickerNoMasked, buildingUnit, null);
//    }
//
//    /** 스티커 번호 뒤 3자리를 마스킹 */
//    private String maskStickerNo(String s) {
//        if (s == null || s.length() < 4) return s;
//        return s.substring(0, s.length() - 3) + "***";
//    }
//}
package com.example.SafeTag_BE.service;

import com.example.SafeTag_BE.enums.VerificationStatus;
import com.example.SafeTag_BE.enums.VerificationType;
import com.example.SafeTag_BE.repository.StickerRepository;
import com.example.SafeTag_BE.repository.VerificationRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PermitService {

    private final StickerRepository stickerRepo;
    private final VerificationRepository verifyRepo;

    @Getter
    @AllArgsConstructor
    public static class Result {
        private final boolean resident;        // 거주민 스티커 보유 여부
        private final boolean maternity;       // 임산부 인증 보유 여부
        private final boolean disabled;        // 장애인 인증 보유 여부
        private final String stickerNoMasked;  // 스티커 번호(마스킹)
        private final String buildingUnit;     // 동/호 (현재 엔티티엔 없음 → null)
        private final String reason;           // 실패/비고 사유
    }

    public Result checkAll(Long userId) {
        if (userId == null) {
            return new Result(false, false, false, null, null, "NO_USER");
        }

        LocalDate today = LocalDate.now();

        // 1) 거주민 스티커: 기간 유효 여부
        boolean resident = stickerRepo.hasValidSticker(userId, VerificationType.RESIDENT, today);

        // 2) 가장 최근 거주민 스티커 1개
        var latestList = stickerRepo.findLatestSticker(
                userId, VerificationType.RESIDENT, PageRequest.of(0, 1)
        );

        // 3) 임시 발급번호(stickerNo 대용) → ID 사용
        String stickerNo = latestList.stream()
                .map(s -> String.valueOf(s.getId()))
                .findFirst()
                .orElse(null);

        // 마스킹 적용
        String stickerNoMasked = maskStickerNo(stickerNo);

        // 4) buildingUnit은 현재 엔티티에 없으므로 null
        String buildingUnit = null;

        // 5) 임산부/장애인 인증: 승인 상태인지
        boolean maternity = verifyRepo.existsByUserIdAndTypeAndStatus(
                userId, VerificationType.PREGNANT, VerificationStatus.APPROVED);

        boolean disabled  = verifyRepo.existsByUserIdAndTypeAndStatus(
                userId, VerificationType.DISABLED, VerificationStatus.APPROVED);

        return new Result(resident, maternity, disabled, stickerNoMasked, buildingUnit, null);
    }

    /** 스티커 번호 뒤 3자리를 마스킹 */
    private String maskStickerNo(String s) {
        if (s == null || s.length() < 4) return s;
        return s.substring(0, s.length() - 3) + "***";
    }
}
