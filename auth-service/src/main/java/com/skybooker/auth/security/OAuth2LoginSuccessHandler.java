package com.skybooker.auth.security;

import com.skybooker.auth.entity.User;
import com.skybooker.auth.enums.AuthProvider;
import com.skybooker.auth.enums.Role;
import com.skybooker.auth.repository.UserRepository;
import com.skybooker.auth.service.JwtService;
import com.skybooker.auth.config.AppProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        org.springframework.security.core.Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        User user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(User.builder()
                .userId(UUID.randomUUID().toString())
                .fullName(name)
                .email(email)
                .phone(null)
                .passwordHash(null)
                .role(Role.PASSENGER)
                .provider(AuthProvider.GOOGLE)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build()));

        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getUserId(), "ACCESS");
        String refreshToken = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getUserId(), "REFRESH");

        String redirectUrl = appProperties.getFrontendBaseUrl()
                + "/#/oauth-success?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                + "&refreshToken=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}
