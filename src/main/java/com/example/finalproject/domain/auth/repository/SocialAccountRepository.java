package com.example.finalproject.domain.auth.repository;

import com.example.finalproject.domain.auth.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    // 기존에 쓰던 것 (소셜 최초 매핑용)
    Optional<SocialAccount> findByProviderAndProviderId(String provider, String providerId);

    //  user.id를 기준으로 조회
    Optional<SocialAccount> findByUser_IdAndProvider(Long userId, String provider);

    // (필요시)
    boolean existsByUser_IdAndProvider(Long userId, String provider);
    List<SocialAccount> findAllByUser_Id(Long userId);
}