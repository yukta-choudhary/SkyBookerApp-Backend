package com.skybooker.auth;

import com.skybooker.auth.entity.TokenBlacklist;
import com.skybooker.auth.repository.TokenBlacklistRepository;
import com.skybooker.auth.service.JwtService;
import com.skybooker.auth.service.impl.TokenBlacklistServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceImplTest {

    @Mock private TokenBlacklistRepository tokenBlacklistRepository;
    @Mock private JwtService jwtService;

    @InjectMocks
    private TokenBlacklistServiceImpl tokenBlacklistService;

    @Test
    @DisplayName("Should blacklist a token")
    void blacklist_success() {
        String token = "some-jwt-token";
        when(jwtService.extractExpiration(token)).thenReturn(new Date(System.currentTimeMillis() + 3600000));

        tokenBlacklistService.blacklist(token);

        verify(tokenBlacklistRepository).save(any(TokenBlacklist.class));
    }

    @Test
    @DisplayName("Should return true when token is blacklisted")
    void isBlacklisted_true() {
        when(tokenBlacklistRepository.existsByTokenValue("blacklisted-token")).thenReturn(true);

        assertThat(tokenBlacklistService.isBlacklisted("blacklisted-token")).isTrue();
    }

    @Test
    @DisplayName("Should return false when token is not blacklisted")
    void isBlacklisted_false() {
        when(tokenBlacklistRepository.existsByTokenValue("valid-token")).thenReturn(false);

        assertThat(tokenBlacklistService.isBlacklisted("valid-token")).isFalse();
    }

    @Test
    @DisplayName("Should cleanup expired tokens")
    void cleanupExpiredTokens() {
        tokenBlacklistService.cleanupExpiredTokens();

        verify(tokenBlacklistRepository).deleteByExpiresAtBefore(any());
    }
}
