package com.example.finalproject.domain.auth.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Map;

@Component
public class OAuthService {

    // ── HTTP 및 사용자정보 API 기본 설정 ─────────────────────────────────────
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

    // ── OAuth 클라이언트 설정(토큰/인가/리다이렉트) ───────────────────────────
    @Value("${oauth.kakao.client-id}")      private String kakaoClientId;
    @Value("${oauth.kakao.client-secret:}") private String kakaoClientSecret; // 없을 수도 있음
    @Value("${oauth.kakao.redirect-uri}")   private String kakaoRedirectUri;
    @Value("${oauth.kakao.authorize-uri:https://kauth.kakao.com/oauth/authorize}")
    private String kakaoAuthorizeUri;
    @Value("${oauth.kakao.token-uri:https://kauth.kakao.com/oauth/token}")
    private String kakaoTokenUri;
    @Value("${oauth.kakao.scope:}")         private String kakaoScope;

    @Value("${oauth.naver.client-id}")      private String naverClientId;
    @Value("${oauth.naver.client-secret}")  private String naverClientSecret;
    @Value("${oauth.naver.redirect-uri}")   private String naverRedirectUri;
    @Value("${oauth.naver.authorize-uri:https://nid.naver.com/oauth2.0/authorize}")
    private String naverAuthorizeUri;
    @Value("${oauth.naver.token-uri:https://nid.naver.com/oauth2.0/token}")
    private String naverTokenUri;
    @Value("${oauth.naver.scope:}")         private String naverScope;

    // ── 인가 URL 생성(프론트/브라우저를 인가 페이지로 보낼 때 사용) ───────────
    public String buildAuthorizeUrl(String provider, String state) {
        OAuthProvider p = OAuthProvider.from(provider);
        return switch (p) {
            case KAKAO -> UriComponentsBuilder.fromHttpUrl(kakaoAuthorizeUri)
                    .queryParam("response_type", "code")
                    .queryParam("client_id", kakaoClientId)
                    .queryParam("redirect_uri", kakaoRedirectUri)
                    .queryParam("scope", kakaoScope)
                    .queryParam("state", state)
                    .build(true).toUriString();
            case NAVER -> {
                var b = UriComponentsBuilder.fromHttpUrl(naverAuthorizeUri)
                        .queryParam("response_type", "code")
                        .queryParam("client_id", naverClientId)
                        .queryParam("redirect_uri", naverRedirectUri)
                        .queryParam("state", state);
                if (naverScope != null && !naverScope.isBlank()) {
                    b.queryParam("scope", naverScope);
                }
                yield b.build(true).toUriString();
            }
        };
    }

    // ── 콜백의 code를 access_token으로 교환 ──────────────────────────────────
    public TokenResponse exchangeCodeForToken(String provider, String code, String state) {
        OAuthProvider p = OAuthProvider.from(provider);

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        h.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);

        try {
            return switch (p) {
                case KAKAO -> {
                    form.add("client_id", kakaoClientId);
                    if (kakaoClientSecret != null && !kakaoClientSecret.isBlank()) {
                        form.add("client_secret", kakaoClientSecret);
                    }
                    form.add("redirect_uri", kakaoRedirectUri);
                    ResponseEntity<TokenResponse> resp = rest.exchange(
                            kakaoTokenUri, HttpMethod.POST, new HttpEntity<>(form, h), TokenResponse.class);
                    yield resp.getBody();
                }
                case NAVER -> {
                    form.add("client_id", naverClientId);
                    form.add("client_secret", naverClientSecret);
                    form.add("redirect_uri", naverRedirectUri);
                    form.add("state", state != null ? state : "");
                    ResponseEntity<TokenResponse> resp = rest.exchange(
                            naverTokenUri, HttpMethod.POST, new HttpEntity<>(form, h), TokenResponse.class);
                    yield resp.getBody();
                }
            };
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("TOKEN_EXCHANGE_FAILED", e);
        }
    }

    // ── 액세스 토큰으로 사용자 정보 조회(기존 로직 유지) ───────────────────────
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

            Map<?, ?> body = resp.getBody();
            if (body == null) throw new IllegalStateException("empty kakao body");
            String id = String.valueOf(body.get("id"));

            String email = null, nickname = null;
            Object account = body.get("kakao_account");
            if (account instanceof Map<?, ?> acc) {
                Object e = acc.get("email");
                if (e != null) email = String.valueOf(e);
                Object profile = acc.get("profile");
                if (profile instanceof Map<?, ?> p) {
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

            Map<?, ?> body = resp.getBody();
            if (body == null) throw new IllegalStateException("empty naver body");
            Object response = body.get("response");
            if (!(response instanceof Map<?, ?> r)) throw new IllegalStateException("invalid naver body");
            String id = String.valueOf(r.get("id"));
            String email = r.get("email") != null ? String.valueOf(r.get("email")) : null;
            String nickname = r.get("nickname") != null ? String.valueOf(r.get("nickname")) : null;
            return new SocialUserInfo("naver", id, email, nickname);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("NAVER_TOKEN_INVALID", e);
        }
    }
    // ── DTOs ──────────────────────────────────────────────────────────────
    @Getter @AllArgsConstructor
    public static class SocialUserInfo {
        private final String provider;    // kakao | naver
        private final String providerId;  // string
        private final String email;       // may be null
        private final String nickname;    // may be null
    }

    @Getter @NoArgsConstructor
    public static class TokenResponse {
        private String access_token;
        private String token_type;
        private Long   expires_in;
        private String refresh_token;
        private String scope;
        private String id_token;
    }

    public enum OAuthProvider {
        KAKAO, NAVER;
        public static OAuthProvider from(String s) {
            return valueOf(s.trim().toUpperCase());
        }
    }
}