package com.example.SafeTag_BE.repository;

import com.example.SafeTag_BE.entity.DynamicQR;
import com.example.SafeTag_BE.entity.User;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DynamicQRRepository extends JpaRepository<DynamicQR, Long> {
    Optional<DynamicQR> findByQrValue(String qrValue);
    Optional<DynamicQR> findTop1ByUserOrderByGeneratedAtDesc(User user);

    @Modifying
    @Query("delete from DynamicQR q where q.expiredAt < :cut")
    int deleteAllExpired(@Param("cut") LocalDateTime cut);
}
