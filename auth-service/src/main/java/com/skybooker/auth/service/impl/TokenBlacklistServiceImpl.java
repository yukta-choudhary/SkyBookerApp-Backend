package com.skybooker.auth.service.impl;

import com.skybooker.auth.service.JwtService;
import com.skybooker.auth.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    @Override
    public void blacklist(String token) {
        Date expiration = jwtService.extractExpiration(token);
        long remainingMs = expiration.getTime() - System.currentTimeMillis();

        if (remainingMs > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + token, "1",
                    Duration.ofMillis(remainingMs)
            );
            log.info("Token blacklisted in Redis with TTL={}s", remainingMs / 1000);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    @Override
    public void cleanupExpiredTokens() {
        // No-op: Redis TTL auto-expires blacklisted tokens
        log.debug("Token cleanup is handled by Redis TTL — no action needed");
    }
}
