package com.example.SafeTag_BE.repository;

import com.example.SafeTag_BE.entity.DynamicQR;
import com.example.SafeTag_BE.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DynamicQRRepository extends JpaRepository<DynamicQR, Long> {
    Optional<DynamicQR> findByQrValue(String qrValue);
    Optional<DynamicQR> findTop1ByUserOrderByGeneratedAtDesc(User user);
}
