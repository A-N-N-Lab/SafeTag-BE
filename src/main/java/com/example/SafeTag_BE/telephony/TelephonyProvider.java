package com.example.SafeTag_BE.telephony;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface TelephonyProvider {
    boolean isMock();
    boolean verifySignature(HttpServletRequest req, String body);
    InboundEvent parseInbound(HttpServletRequest req);
    ResponseEntity<String> bridge (String fromAliasE164, String toRealE164);
    ResponseEntity<String> sayAndHangup(String messageKo);

}
