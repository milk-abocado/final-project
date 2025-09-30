package com.example.finalproject.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmtpPasswordMailService implements PasswordMailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from:no-reply@final-project.local}")
    private String from;

    @Override
    public void sendResetCode(String toEmail, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(toEmail);
        msg.setSubject("[Final Project] Password reset code");
        msg.setText("인증코드: " + code + "\n유효시간: 10분");
        mailSender.send(msg);
    }

    @Override
    public void sendPasswordChangedNotice(String toEmail) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(toEmail);
        msg.setSubject("[Final Project] 비밀번호가 변경되었습니다");
        msg.setText("본인이 변경한 것이 아니라면 즉시 문의하세요.");
        mailSender.send(msg);
    }
}