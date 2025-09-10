package com.example.finalproject.domain.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
class SmtpMailSender {
    private final JavaMailSender sender;


    @org.springframework.beans.factory.annotation.Value("${mail.from:#{null}}")
    private String from;


    @org.springframework.beans.factory.annotation.Value("${mail.from-name:FinalProject}")
    private String fromName;


    public void send(String to, String subject, String text){
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            if(from != null) msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            sender.send(msg);
        } catch(Exception e){
            throw new RuntimeException("Mail send failed", e);
        }
    }


    public void sendHtml(String to, String subject, String html){
        try {
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            if(from != null){
                helper.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()));
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(mime);
        } catch(MessagingException | java.io.UnsupportedEncodingException e){
            throw new RuntimeException("HTML mail send failed", e);
        }
    }
}