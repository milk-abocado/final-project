//package com.example.finalproject.domain.auth.repository;
//
//import com.example.finalproject.domain.auth.entity.SocialAccount;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.Optional;
//
//public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
//    boolean existsByProviderAndProviderId(String provider, String providerId);
//    Optional<SocialAccount> findByProviderAndProviderId(String provider, String providerId);
//    Optional<SocialAccount> findByUserIdAndProvider(Long userId, String provider);
//}