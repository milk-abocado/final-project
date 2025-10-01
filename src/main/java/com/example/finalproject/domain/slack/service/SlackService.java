package com.example.finalproject.domain.slack.service;

import com.example.finalproject.domain.slack.exception.SlackErrorCode;
import com.example.finalproject.domain.slack.exception.SlackException;
import com.slack.api.Slack;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SlackService {

    @Value("${slack.token}")
    private String token;

    @Value("${slack.ownerChannel}")
    private String ownerChannel;

    @Value("${slack.userChannel}")
    private String userChannel;

    @Value("${slack.allUserChannel}")
    private String allUserChannel;

    //사장님 전용 알림
    public void sendOwnerMessage(String text) {
        sendMessage(ownerChannel, text);
    }

    //사용자 전용 알림
    public void sendUserMessage(String text) {
        sendMessage(userChannel, text);
    }

    // 내부 공통 메서드
    private void sendMessage(String channel, String text) {
        try {
            Slack slack = Slack.getInstance();
            slack.methods(token).chatPostMessage(req -> req
                    .channel(channel)
                    .text(text)
            );
        } catch (Exception e) {
            throw new SlackException(SlackErrorCode.SEND_MESSAGE_FAILED,
                    "슬랙 메시지 전송에 실패했습니다.", e);
        }
    }

    //전체 알림
    public void sendAllUserMessage(String text) {
        sendMessage(allUserChannel, text);
    }
}
