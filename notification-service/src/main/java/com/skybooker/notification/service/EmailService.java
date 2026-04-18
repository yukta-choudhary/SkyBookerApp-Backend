package com.skybooker.notification.service;

public interface EmailService {
    void sendEmail(String to, String subject, String htmlBody);
}
