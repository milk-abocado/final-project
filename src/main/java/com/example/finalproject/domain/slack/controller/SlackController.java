package com.example.finalproject.domain.slack.controller;

import com.example.finalproject.domain.slack.service.SlackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor

public class SlackController {
    private final SlackService slackService;

    // 테스트 코드 http://localhost:8080/slack-test
    // 호출 시 슬랙의 deliverynotifier-test 채널로 테스트 메세지 전송
    @GetMapping("/slack-test")
    public String testSlack(){
        slackService.sendMessage("알림 테스트");
        return "메세지 전송이 완료되었습니다";
    }
}
