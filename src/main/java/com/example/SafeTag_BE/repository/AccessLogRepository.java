package com.example.SafeTag_BE.repository;


import com.example.SafeTag_BE.entity.AccessLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessLogRepository extends JpaRepository<AccessLog,Long > {
    List<AccessLog> findByQrId(Long qrId);
}
