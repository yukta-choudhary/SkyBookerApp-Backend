package com.skybooker.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {
    private Jwt jwt = new Jwt();
    private String frontendBaseUrl;
    private PasswordReset passwordReset = new PasswordReset();
    private Mail mail = new Mail();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessTokenExpirationMs;
        private long refreshTokenExpirationMs;
    }

    @Getter
    @Setter
    public static class PasswordReset {
        private long expirationMinutes;
    }

    @Getter
    @Setter
    public static class Mail {
        private String from;
    }
}
