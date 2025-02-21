package com.example.SafeTag_BE.repository;

import com.example.SafeTag_BE.entity.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<SiteUser, Long> {
	Optional<SiteUser> findByUsername(String username);
}
