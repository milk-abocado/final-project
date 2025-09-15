package com.example.finalproject.domain.auth.service;

import com.example.finalproject.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsersRepository usersRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var u = usersRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // 권한 매핑은 프로젝트에 맞게 변경(예: u.getRole(), u.getRoles() 등)
        // 기본값으로 ROLE_USER 하나만 부여(필요 시 수정)
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        return new org.springframework.security.core.userdetails.User(
                u.getEmail(),          // principal(=username)
                u.getPassword(),       // 반드시 인코딩된 해시
                true, true, true, true,
                authorities
        );
    }
}
