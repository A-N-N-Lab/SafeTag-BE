//package com.example.SafeTag_BE.repository;
//
//import com.example.SafeTag_BE.entity.Sticker;
//import com.example.SafeTag_BE.enums.StickerType;
//import io.lettuce.core.dynamic.annotation.Param;
//import java.time.LocalDate;
//import java.util.List;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//
//public interface StickerRepository extends JpaRepository<Sticker, Long> {
//
//    //  userId + type 기준으로 현재 유효한 스티커 존재 여부 확인
//    //  validFrom <= 오늘 <= validTo 인 경우 true
//
//    @Query("""
//        select (count(s) > 0)
//        from Sticker s
//        where s.user.id = :userId
//          and s.type = :type
//          and s.validFrom <= :today
//          and s.validTo >= :today
//    """)
//    boolean hasValidResident(
//            @Param("userId") Long userId,
//            @Param("type") StickerType type,
//            @Param("today") LocalDate today
//    );
//
//
//    // userId + type 기준으로 최신 스티커 1개 조회 (issuedAt 최신순)
//
//    @Query("""
//        select s
//        from Sticker s
//        where s.user.id = :userId
//          and s.type = :type
//        order by s.issuedAt desc
//    """)
//    List<Sticker> findLatestResident(
//            @Param("userId") Long userId,
//            @Param("type") StickerType type,
//            Pageable pageable
//    );
//}

package com.example.SafeTag_BE.repository;

import com.example.SafeTag_BE.entity.Sticker;
import com.example.SafeTag_BE.enums.VerificationType;
import com.example.SafeTag_BE.enums.VerificationType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // ← 이거!

public interface StickerRepository extends JpaRepository<Sticker, Long> {

    @Query("""
        select (count(s) > 0)
        from Sticker s
        where s.userId = :userId
          and s.type = :type
          and s.issuedAt <= :today
          and s.expiresAt >= :today
    """)
    boolean hasValidSticker(
            @Param("userId") Long userId,
            @Param("type") VerificationType type,
            @Param("today") LocalDate today
    );

    @Query("""
        select s
        from Sticker s
        where s.userId = :userId
          and s.type = :type
        order by s.issuedAt desc
    """)
    List<Sticker> findLatestSticker(
            @Param("userId") Long userId,
            @Param("type") VerificationType type,
            Pageable pageable
    );
}
