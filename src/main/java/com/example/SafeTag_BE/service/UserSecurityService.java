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

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		//일반 사용자 조회
		Optional<User> optionalUser = userRepository.findByUsername(username);
		if (optionalUser.isPresent()) {
			User user = optionalUser.get();
			List<GrantedAuthority> authorities = List.of(
					new SimpleGrantedAuthority(user.getRole()) // ROLE_USER
			);
			return new org.springframework.security.core.userdetails.User(
					user.getUsername(), user.getPassword(), authorities
			);
		}

		//관리자 조회
		Optional<Admin> optionalAdmin = adminRepository.findByUsername(username);
		if (optionalAdmin.isPresent()) {
			Admin admin = optionalAdmin.get();
			List<GrantedAuthority> authorities = List.of(
					new SimpleGrantedAuthority(admin.getRole()) // ROLE_ADMIN
			);
			return new org.springframework.security.core.userdetails.User(
					admin.getUsername(), admin.getPassword(), authorities
			);
		}

		//예외 처리
		throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
	}

	// 추출된 회원번호(id)로 사용자 정보 조회
	public UserDetails loadUserById(Long id) {
		com.example.SafeTag_BE.entity.User user = userRepository.findById(id)
				.orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

		return new org.springframework.security.core.userdetails.User(
				user.getUsername(),
				user.getPassword(),
				List.of(new SimpleGrantedAuthority(user.getRole()))
		);
	}

}
