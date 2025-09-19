//package com.example.finalproject.domain.common.mail;
//
//import jakarta.mail.MessagingException;
//import jakarta.mail.internet.MimeMessage;
//import lombok.RequiredArgsConstructor;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class SmtpMailSender implements MailSender {
//
//    private final JavaMailSender mail;
//
//    @Override
//    public void send(String to, String subject, String html) {
//        MimeMessage message = mail.createMimeMessage();
//        try {
//            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//            helper.setTo(to);
//            helper.setSubject(subject);
//            helper.setText(html, true);
//            mail.send(message);
//        } catch (MessagingException e) {
//            throw new IllegalStateException("메일 전송 실패", e);
//        }
//    }
//}