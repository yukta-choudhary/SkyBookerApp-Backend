package com.skybooker.auth.service;

public interface EmailService {
    void sendPasswordResetEmail(String to, String fullName, String resetLink);
}
