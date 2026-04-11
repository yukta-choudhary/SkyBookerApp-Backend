package com.skybooker.auth.service.impl;

import com.skybooker.auth.entity.TokenBlacklist;
import com.skybooker.auth.repository.TokenBlacklistRepository;
import com.skybooker.auth.service.JwtService;
import com.skybooker.auth.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtService jwtService;

    @Override
    public void blacklist(String token) {
        tokenBlacklistRepository.save(TokenBlacklist.builder()
                .tokenValue(token)
                .expiresAt(LocalDateTime.ofInstant(jwtService.extractExpiration(token).toInstant(), ZoneId.systemDefault()))
                .build());
    }

    @Override
    public boolean isBlacklisted(String token) {
        return tokenBlacklistRepository.existsByTokenValue(token);
    }

    @Override
    @Scheduled(cron = "0 */30 * * * *")
    public void cleanupExpiredTokens() {
        tokenBlacklistRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
