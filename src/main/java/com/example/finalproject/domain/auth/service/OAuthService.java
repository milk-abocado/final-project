package com.example.finalproject.domain.auth.service;

import com.example.finalproject.domain.auth.exception.AuthApiException;
import com.example.finalproject.domain.auth.exception.AuthErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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

    // ── OAuth 클라이언트 설정 ──
    @Value("${oauth.kakao.client-id}")      private String kakaoClientId;
    @Value("${oauth.kakao.client-secret:}") private String kakaoClientSecret;
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

    // ========================= 예외 매핑 헬퍼 =========================
    private static AuthApiException socialParam(String msg) {
        return new AuthApiException(AuthErrorCode.SOCIAL_PARAM_INVALID, msg);
    }

    // 인가 URL 만들기
    public String buildAuthorizeUrl(String provider, String state) {
        final OAuthProvider p;
        try {
            p = OAuthProvider.from(provider);
        } catch (Exception e) {
            throw socialParam("provider 값이 올바르지 않습니다: " + provider);
        }

        return switch (p) {
            case KAKAO -> {
                if (isBlank(kakaoClientId) || isBlank(kakaoRedirectUri))
                    throw socialParam("kakao 클라이언트 설정 누락(client-id/redirect-uri)");
                yield UriComponentsBuilder.fromHttpUrl(kakaoAuthorizeUri)
                        .queryParam("response_type", "code")
                        .queryParam("client_id", kakaoClientId)
                        .queryParam("redirect_uri", kakaoRedirectUri)
                        .queryParam("scope", kakaoScope)
                        .queryParam("state", state)
                        .build(true).toUriString();
            }
            case NAVER -> {
                if (isBlank(naverClientId) || isBlank(naverRedirectUri))
                    throw socialParam("naver 클라이언트 설정 누락(client-id/redirect-uri)");
                var b = UriComponentsBuilder.fromHttpUrl(naverAuthorizeUri)
                        .queryParam("response_type", "code")
                        .queryParam("client_id", naverClientId)
                        .queryParam("redirect_uri", naverRedirectUri)
                        .queryParam("state", state);
                if (!isBlank(naverScope)) b.queryParam("scope", naverScope);
                yield b.build(true).toUriString();
            }
        };
    }

    // code → token 교환
    public TokenResponse exchangeCodeForToken(String provider, String code, String state) {
        final OAuthProvider p;
        try {
            p = OAuthProvider.from(provider);
        } catch (Exception e) {
            throw socialParam("provider 값이 올바르지 않습니다: " + provider);
        }
        if (isBlank(code)) throw socialParam("authorization code 가 비어있습니다.");

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
                    if (!isBlank(kakaoClientSecret)) form.add("client_secret", kakaoClientSecret);
                    form.add("redirect_uri", kakaoRedirectUri);
                    ResponseEntity<TokenResponse> resp = rest.exchange(
                            kakaoTokenUri, HttpMethod.POST, new HttpEntity<>(form, h), TokenResponse.class);
                    yield nonNullBody(resp, "kakao token 교환 실패");
                }
                case NAVER -> {
                    form.add("client_id", naverClientId);
                    form.add("client_secret", naverClientSecret);
                    form.add("redirect_uri", naverRedirectUri);
                    form.add("state", state != null ? state : "");
                    ResponseEntity<TokenResponse> resp = rest.exchange(
                            naverTokenUri, HttpMethod.POST, new HttpEntity<>(form, h), TokenResponse.class);
                    yield nonNullBody(resp, "naver token 교환 실패");
                }
            };
        } catch (RestClientResponseException e) {
            throw socialParam("token 교환 실패(" + p.name().toLowerCase() + "): " + e.getRawStatusCode());
        }
    }

    // 액세스 토큰으로 유저 정보
    public SocialUserInfo fetchUser(String provider, String accessToken) {
        final OAuthProvider p;
        try {
            p = OAuthProvider.from(provider);
        } catch (Exception e) {
            throw socialParam("provider 값이 올바르지 않습니다: " + provider);
        }
        if (isBlank(accessToken)) throw socialParam("access token 이 비어있습니다.");

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
            Map<?, ?> body = nonNullBody(resp, "kakao 사용자정보 응답 없음/형식 오류");
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
            throw socialParam("유효하지 않은 kakao access token");
        }
    }

    private SocialUserInfo fetchNaver(String accessToken) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        h.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        try {
            ResponseEntity<Map> resp = rest.exchange(
                    naverUserInfoUri, HttpMethod.GET, new HttpEntity<>(h), Map.class);
            Map<?, ?> body = nonNullBody(resp, "naver 사용자정보 응답 없음/형식 오류");
            Object response = body.get("response");
            if (!(response instanceof Map<?, ?> r))
                throw socialParam("naver 사용자정보 형식이 올바르지 않습니다.");
            String id = String.valueOf(r.get("id"));
            String email = r.get("email") != null ? String.valueOf(r.get("email")) : null;
            String nickname = r.get("nickname") != null ? String.valueOf(r.get("nickname")) : null;
            return new SocialUserInfo("naver", id, email, nickname);
        } catch (RestClientResponseException e) {
            throw socialParam("유효하지 않은 naver access token");
        }
    }

    private static <T> T nonNullBody(ResponseEntity<T> resp, String stageMsg) {
        if (resp == null || resp.getBody() == null) {
            throw socialParam(stageMsg);
        }
        return resp.getBody();
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    // DTOs
    @Getter @AllArgsConstructor
    public static class SocialUserInfo {
        private final String provider;
        private final String providerId;
        private final String email;
        private final String nickname;
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
