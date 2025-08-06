package com.example.SafeTag_BE.service;


import com.example.SafeTag_BE.dto.CreateQrRequestDto;
import com.example.SafeTag_BE.dto.QrResponseDto;
import com.example.SafeTag_BE.entity.DynamicQR;
import com.example.SafeTag_BE.entity.SiteUser;
import com.example.SafeTag_BE.repository.DynamicQRRepository;
import com.example.SafeTag_BE.repository.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class QrService {

    private final DynamicQRRepository qrRepository;
    private  final UserRepository userRepository;

    public QrResponseDto createQr(CreateQrRequestDto dto){
        String qrValue = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        SiteUser user = userRepository.findById(dto.getUserId())
                .orElseThrow(()->new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));

        DynamicQR qr = DynamicQR.builder()
                .qrValue(qrValue)
                .user(user)
                .generatedAt(now)
                .expiredAt(now.plusMinutes(1))
                .build();

        qrRepository.save(qr);

        return new QrResponseDto(
                qr.getId(),
                qr.getQrValue(),
                qr.getExpiredAt().toString()
        );
    }

    public byte[] generateQrImage(String qrValue) throws Exception{
        QRCodeWriter qrCodeWriter= new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrValue, BarcodeFormat.QR_CODE,250,250);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix,"PNG",pngOutputStream);
        return pngOutputStream.toByteArray();
    }
    public DynamicQR getQrById(Long qrId) {
        return qrRepository.findById(qrId)
                .orElseThrow(() -> new IllegalArgumentException("QR ID 없음"));
    }

    public String getProxyPhoneNumber(String qrValue){
        DynamicQR qr = qrRepository.findByQrValue(qrValue)
                .orElseThrow(()-> new IllegalArgumentException("QR값이 유효하지 않음"));
        if(qr.getExpiredAt().isBefore(LocalDateTime.now())){
            throw new IllegalStateException("QR이 만료되었습니다.");
        }
        SiteUser user = qr.getUser();

        // 실제로는 전화중계 서버로 연결 ( 현재는 마스킹된 번호 반환)
        String maksedPhone = user.getPhoneNum().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1-****-$2");
        return maksedPhone;
    }


}
