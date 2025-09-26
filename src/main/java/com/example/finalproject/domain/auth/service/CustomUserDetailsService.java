package com.example.finalproject.domain.auth.service;

import com.example.finalproject.config.CustomUserPrincipal;
import com.example.finalproject.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsersRepository usersRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 이메일 정규화
        String norm = email == null ? null : email.trim().toLowerCase(Locale.ROOT);

        var u = usersRepository
                // deleted 플래그가 있으면 findByEmailIgnoreCaseAndDeletedFalse 로 교체
                .findByEmailIgnoreCase(norm)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // ROLE 매핑 (예: USER → ROLE_USER)
        String roleName = u.getRole() != null ? "ROLE_" + u.getRole().name() : "ROLE_USER";
        var authorities = List.of(new SimpleGrantedAuthority(roleName));

        // enabled/locked 등은 엔티티 상황에 맞게
        boolean enabled = true;
        boolean accountNonExpired = true;
        boolean accountNonLocked = true;
        boolean credentialsNonExpired = true;

        return new CustomUserPrincipal(
                u.getId(),
                u.getEmail(),
                u.getPassword(),
                authorities,
                enabled,
                accountNonExpired,
                accountNonLocked,
                credentialsNonExpired
        );
    }
}