package com.example.SafeTag_BE.service;

import com.example.SafeTag_BE.entity.Admin;
import com.example.SafeTag_BE.entity.User;
import com.example.SafeTag_BE.repository.AdminRepository;
import com.example.SafeTag_BE.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserSecurityService implements UserDetailsService {

	private final UserRepository userRepository;
	private final AdminRepository adminRepository;

	//로그인 시 사용
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// 일반 사용자 조회
		Optional<User> optionalUser = userRepository.findByUsername(username);
		if (optionalUser.isPresent()) {
			User user = optionalUser.get();
			List<GrantedAuthority> authorities = List.of(
					new SimpleGrantedAuthority(user.getRole())  // ROLE_USER
			);
			return new org.springframework.security.core.userdetails.User(
					user.getUsername(), user.getPassword(), authorities
			);
		}

		// 관리자 조회
		Optional<Admin> optionalAdmin = adminRepository.findByUsername(username);
		if (optionalAdmin.isPresent()) {
			Admin admin = optionalAdmin.get();
			List<GrantedAuthority> authorities = List.of(
					new SimpleGrantedAuthority(admin.getRole())  // ROLE_ADMIN
			);
			return new org.springframework.security.core.userdetails.User(
					admin.getUsername(), admin.getPassword(), authorities
			);
		}

		throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
	}

	// userId 기반 인증 (토큰 해석 시 사용)
	public UserDetails loadUserById(Long id) {
		// 일반 사용자 확인
		Optional<User> optionalUser = userRepository.findById(id);
		if (optionalUser.isPresent()) {
			User user = optionalUser.get();
			return new org.springframework.security.core.userdetails.User(
					user.getUsername(),
					user.getPassword(),
					List.of(new SimpleGrantedAuthority(user.getRole()))
			);
		}

		// 관리자 확인
		Optional<Admin> optionalAdmin = adminRepository.findById(id);
		if (optionalAdmin.isPresent()) {
			Admin admin = optionalAdmin.get();
			return new org.springframework.security.core.userdetails.User(
					admin.getUsername(),
					admin.getPassword(),
					List.of(new SimpleGrantedAuthority(admin.getRole()))
			);
		}

		throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
	}
}
