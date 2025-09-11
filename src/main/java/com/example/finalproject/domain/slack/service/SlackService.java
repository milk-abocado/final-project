package com.example.finalproject.domain.slack.service;

import com.example.finalproject.domain.slack.exception.SlackException;
import com.slack.api.Slack;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class SlackService {
    @Value("${slack.token}")
    private String token;

    @Value("${slack.channel}")
    private String channel;

    //지정된 slack 채널로 메세지 전송
    public void sendMessage(String text){
        try{
            Slack slack = Slack.getInstance();
            slack.methods(token).chatPostMessage(req -> req
                    .channel(channel)
                    .text(text)
            );
        }catch(Exception e){
            throw new SlackException("메세지 전송을 실패하였습니다.",e);
        }
    }
}