package com.example.SafeTag_BE.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QrViewResponseDto {
    private String mode;    // public이냐 admin이냐
    private Long qrId;
    private boolean valid; //qr 만료 여부
    private String vehicleMask; //마스킹된 차량 표기

    private RelayInfo relay;  // public 전용
    private AdminInfo admin; // admin 전용

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RelayInfo {
        private String callTicket; // api/calls/relay에 보낼 일회성 토큰
        private String msgTicket; // api/messgaes/relay에 보낼 일회성 토큰
        private int ttlSec;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AdminInfo {
        private boolean resident; // 거주민 스티커 보유
        private boolean maternity; //임산부 허가
        private boolean disabled; // 장애인 허가
        private String stickerNoMasked; //스티커 번호 마스킹(선택)
        private String ownerMask; // ex) 김*영 등 (선택)
        private String buildingUnitMask; // ex) 101ehd 2**호 등 (선택)
        private String note;
    }

}
