package com.skybooker.auth.service.impl;

import com.skybooker.auth.config.AppProperties;
import com.skybooker.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final AppProperties appProperties;

    @Override
    public void sendPasswordResetEmail(String to, String fullName, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(appProperties.getMail().getFrom());
        message.setTo(to);
        message.setSubject("SkyBooker Password Reset");
        message.setText("""
                Hello %s,

                We received a request to reset your SkyBooker account password.

                Reset your password using the link below:
                %s

                This link will expire in %d minutes.

                If you did not request this, please ignore this email.

                Regards,
                SkyBooker Team
                """.formatted(fullName, resetLink, appProperties.getPasswordReset().getExpirationMinutes()));
        javaMailSender.send(message);
    }
}
