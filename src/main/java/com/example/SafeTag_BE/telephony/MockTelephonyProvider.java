package com.example.SafeTag_BE.telephony;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix="relay", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockTelephonyProvider  implements  TelephonyProvider{

    @Override
    public boolean isMock(){
        return true;
    }

    @Override
    public boolean verifySignature(HttpServletRequest req, String body){
        return true;
    }

    @Override
    public InboundEvent parseInbound(HttpServletRequest req){
        String called = req.getParameter("To");
        String caller = req.getParameter("From");
        return new InboundEvent(called, caller);
    }

    @Override
    public ResponseEntity<String> bridge(String fromAliasE164, String toRealE164){
        String twiml =
                "<Response><Say language=\"ko-KR\">목업 브리지</Say>" +
                        "<Dial callerId=\"" + fromAliasE164 + "\"><Number>" + toRealE164 + "</Number></Dial></Response>";
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(twiml);
    }


    @Override
    public ResponseEntity<String> sayAndHangup(String messageKo) {
        String twiml = "<Response><Say language=\"ko-KR\">" + messageKo + "</Say><Hangup/></Response>";
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(twiml);
    }



}
