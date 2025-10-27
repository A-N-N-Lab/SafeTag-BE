package com.example.SafeTag_BE.repository;


import com.example.SafeTag_BE.entity.FcmToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    List<FcmToken> findAllByUserIdAndActiveTrue(Long userId);
    Optional<FcmToken> findByToken(String token);

}
