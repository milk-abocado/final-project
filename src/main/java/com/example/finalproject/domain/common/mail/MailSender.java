package com.example.finalproject.domain.common.mail;

public interface MailSender {
    void send(String to, String subject, String html);
}