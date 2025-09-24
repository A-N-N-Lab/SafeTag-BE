package com.example.SafeTag_BE.repository;

import com.example.SafeTag_BE.entity.CallSession;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallSessionRepository extends JpaRepository<CallSession, Long> {
    Optional<CallSession> findBySessionUuid(String sessionUuid);

}
