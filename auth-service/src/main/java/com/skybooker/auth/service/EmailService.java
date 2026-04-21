package com.skybooker.auth.service;

public interface EmailService {
    void sendPasswordResetEmail(String to, String fullName, String resetLink);
    void sendPasswordResetOtp(String to, String fullName, String otp, long expirationMinutes);
}
