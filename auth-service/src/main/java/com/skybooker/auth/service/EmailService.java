package com.skybooker.auth.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetToken);
}