package com.example.finalproject.domain.common.oauth;

import lombok.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Component
public class SocialOAuthClient {

    private final RestTemplate rest;

    public SocialOAuthClient(RestTemplateBuilder builder) {
        this.rest = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    public SocialUserInfo getKakaoUser(String accessToken) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        h.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        try {
            ResponseEntity<Map> resp = rest.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.GET, new HttpEntity<>(h), Map.class);

            Map<?,?> body = resp.getBody();
            if (body == null) throw new IllegalStateException("empty kakao body");
            // id (number), kakao_account.email, kakao_account.profile.nickname
            String id = String.valueOf(body.get("id"));

            String email = null, nickname = null;
            Object kakaoAccount = body.get("kakao_account");
            if (kakaoAccount instanceof Map<?,?> acc) {
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

    public SocialUserInfo getNaverUser(String accessToken) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        h.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        try {
            ResponseEntity<Map> resp = rest.exchange(
                    "https://openapi.naver.com/v1/nid/me",
                    HttpMethod.GET, new HttpEntity<>(h), Map.class);

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
        private final String provider;   // kakao | naver
        private final String providerId; // string
        private final String email;      // may be null (동의 범위에 따라)
        private final String nickname;   // may be null
    }
}