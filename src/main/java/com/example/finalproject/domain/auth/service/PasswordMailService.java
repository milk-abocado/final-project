package com.example.finalproject.domain.auth.service;

public interface PasswordMailService {
    void sendResetCode(String toEmail, String code);
    default void sendPasswordChangedNotice(String toEmail) {}
}