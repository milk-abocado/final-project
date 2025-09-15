package com.example.finalproject.domain.auth.controller;


import com.example.finalproject.config.OAuthProps;
import com.example.finalproject.domain.auth.dto.SocialProviderLoginRequest;
import com.example.finalproject.domain.auth.service.AuthService;
import com.example.finalproject.domain.auth.service.OAuthCodeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthProps props;
    private final OAuthCodeClient codeClient;
    private final AuthService authService;

    @GetMapping("/kakao/login")
    public ResponseEntity<Void> kakaoLogin() {
        String authUrl = "https://kauth.kakao.com/oauth/authorize"
                + "?response_type=code"
                + "&client_id=" + url(props.getKakao().getClientId())
                + "&redirect_uri=" + url(props.getKakao().getRedirectUri())
                + (StringUtils.hasText(props.getKakao().getScope())
                ? "&scope=" + url(props.getKakao().getScope()) : "");
        return ResponseEntity.status(302).location(URI.create(authUrl)).build();
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<Map<String, Object>> kakaoCallback(@RequestParam String code) {
        String accessToken = codeClient.exchangeKakaoCode(code);
        var req = SocialProviderLoginRequest.builder()
                .provider("kakao")
                .accessToken(accessToken)
                .build();
        return ResponseEntity.ok(authService.socialLoginByAccessToken(req));
    }

    @GetMapping("/naver/login")
    public ResponseEntity<Void> naverLogin() {
        String state = UUID.randomUUID().toString(); // 간단 예시(실서비스는 CSRF 대비 보관/검증 권장)
        String authUrl = "https://nid.naver.com/oauth2.0/authorize"
                + "?response_type=code"
                + "&client_id=" + url(props.getNaver().getClientId())
                + "&redirect_uri=" + url(props.getNaver().getRedirectUri())
                + "&state=" + url(state);
        return ResponseEntity.status(302).location(URI.create(authUrl)).build();
    }

    @GetMapping("/naver/callback")
    public ResponseEntity<Map<String, Object>> naverCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state
    ) {
        String accessToken = codeClient.exchangeNaverCode(code, state);
        var req = SocialProviderLoginRequest.builder()
                .provider("naver")
                .accessToken(accessToken)
                .build();
        return ResponseEntity.ok(authService.socialLoginByAccessToken(req));
    }

    private static String url(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}