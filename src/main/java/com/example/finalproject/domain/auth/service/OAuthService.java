package com.example.finalproject.domain.auth.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Component
public class OAuthService {

    private final RestTemplate rest;
    private final String kakaoUserInfoUri;
    private final String naverUserInfoUri;

    public OAuthService(
            RestTemplateBuilder builder,
            @Value("${oauth.kakao.userinfo-uri:https://kapi.kakao.com/v2/user/me}") String kakaoUserInfoUri,
            @Value("${oauth.naver.userinfo-uri:https://openapi.naver.com/v1/nid/me}") String naverUserInfoUri
    ) {
        this.rest = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.kakaoUserInfoUri = kakaoUserInfoUri;
        this.naverUserInfoUri = naverUserInfoUri;
    }

    public SocialUserInfo fetchUser(String provider, String accessToken) {
        OAuthProvider p = OAuthProvider.from(provider);
        return switch (p) {
            case KAKAO -> fetchKakao(accessToken);
            case NAVER -> fetchNaver(accessToken);
        };
    }

    private SocialUserInfo fetchKakao(String accessToken) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        h.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        try {
            ResponseEntity<Map> resp = rest.exchange(
                    kakaoUserInfoUri, HttpMethod.GET, new HttpEntity<>(h), Map.class);

            Map<?,?> body = resp.getBody();
            if (body == null) throw new IllegalStateException("empty kakao body");
            String id = String.valueOf(body.get("id"));

            String email = null, nickname = null;
            Object account = body.get("kakao_account");
            if (account instanceof Map<?,?> acc) {
                Object e = acc.get("email");
                if (e != null) email = String.valueOf(e);
                Object profile = acc.get("profile");
                if (profile instanceof Map<?,?> p) {
                    Object n = p.get("nickname");
                    if (n != null) nickname = String.valueOf(n);
                }
            }
            return new SocialUserInfo("kakao", id, email, nickname);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("KAKAO_TOKEN_INVALID", e);
        }
    }

    private SocialUserInfo fetchNaver(String accessToken) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        h.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        try {
            ResponseEntity<Map> resp = rest.exchange(
                    naverUserInfoUri, HttpMethod.GET, new HttpEntity<>(h), Map.class);

            Map<?,?> body = resp.getBody();
            if (body == null) throw new IllegalStateException("empty naver body");
            Object response = body.get("response");
            if (!(response instanceof Map<?,?> r)) throw new IllegalStateException("invalid naver body");
            String id = String.valueOf(r.get("id"));
            String email = r.get("email") != null ? String.valueOf(r.get("email")) : null;
            String nickname = r.get("nickname") != null ? String.valueOf(r.get("nickname")) : null;
            return new SocialUserInfo("naver", id, email, nickname);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("NAVER_TOKEN_INVALID", e);
        }
    }

    @Getter @AllArgsConstructor
    public static class SocialUserInfo {
        private final String provider;    // kakao | naver
        private final String providerId;  // string
        private final String email;       // may be null
        private final String nickname;    // may be null
    }

    public enum OAuthProvider {
        KAKAO, NAVER;
        public static OAuthProvider from(String s){
            return valueOf(s.trim().toUpperCase());
        }
    }
}