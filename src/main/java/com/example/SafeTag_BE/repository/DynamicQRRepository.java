package com.example.SafeTag_BE.repository;

import com.example.SafeTag_BE.entity.DynamicQR;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DynamicQRRepository extends JpaRepository<DynamicQR, Long> {
    Optional<DynamicQR> findByQrValue(String qrValue);

}
