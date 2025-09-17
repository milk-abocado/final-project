package com.example.finalproject.domain.auth.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;

import java.time.Duration;
import java.util.Map;

@Component
public class OAuthCodeClient {

    private final RestTemplate rest;

    private final String kakaoTokenUri;
    private final String kakaoClientId;
    private final String kakaoClientSecret;
    private final String kakaoRedirectUri;

    private final String naverTokenUri;
    private final String naverClientId;
    private final String naverClientSecret;
    private final String naverRedirectUri;

    // ★ 단 하나의 생성자만 유지하세요 (여기엔 @Autowired 없어도 됩니다)
    public OAuthCodeClient(
            RestTemplateBuilder builder,
            @Value("${oauth.kakao.token-uri:https://kauth.kakao.com/oauth/token}") String kakaoTokenUri,
            @Value("${oauth.kakao.client-id}") String kakaoClientId,
            @Value("${oauth.kakao.client-secret:}") String kakaoClientSecret,
            @Value("${oauth.kakao.redirect-uri}") String kakaoRedirectUri,
            @Value("${oauth.naver.token-uri:https://nid.naver.com/oauth2.0/token}") String naverTokenUri,
            @Value("${oauth.naver.client-id}") String naverClientId,
            @Value("${oauth.naver.client-secret}") String naverClientSecret,
            @Value("${oauth.naver.redirect-uri}") String naverRedirectUri
    ) {
        this.rest = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.kakaoTokenUri = kakaoTokenUri;
        this.kakaoClientId = kakaoClientId;
        this.kakaoClientSecret = kakaoClientSecret;
        this.kakaoRedirectUri = kakaoRedirectUri;
        this.naverTokenUri = naverTokenUri;
        this.naverClientId = naverClientId;
        this.naverClientSecret = naverClientSecret;
        this.naverRedirectUri = naverRedirectUri;
    }
    public String exchangeKakaoCode(String code) {
        return exchangeKakaoCodeForToken(code);
    }

    public String exchangeNaverCode(String code, String state) {
        return exchangeNaverCodeForToken(code, state);
    }

    public String exchangeKakaoCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var body = new LinkedMultiValueMap<String, String>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", kakaoClientId);
        if (kakaoClientSecret != null && !kakaoClientSecret.isBlank()) {
            body.add("client_secret", kakaoClientSecret);
        }
        body.add("redirect_uri", kakaoRedirectUri);
        body.add("code", code);

        var req = new HttpEntity<>(body, headers);
        ResponseEntity<Map> resp = rest.postForEntity(kakaoTokenUri, req, Map.class);
        Object at = resp.getBody() == null ? null : resp.getBody().get("access_token");
        if (at == null) throw new IllegalStateException("kakao access_token missing");
        return String.valueOf(at);
    }

    public String exchangeNaverCodeForToken(String code, String state) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var body = new LinkedMultiValueMap<String, String>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", naverClientId);
        body.add("client_secret", naverClientSecret);
        body.add("redirect_uri", naverRedirectUri);
        body.add("code", code);
        if (state != null) body.add("state", state);

        var req = new HttpEntity<>(body, headers);
        ResponseEntity<Map> resp = rest.postForEntity(naverTokenUri, req, Map.class);
        Object at = resp.getBody() == null ? null : resp.getBody().get("access_token");
        if (at == null) throw new IllegalStateException("naver access_token missing");
        return String.valueOf(at);
    }
}