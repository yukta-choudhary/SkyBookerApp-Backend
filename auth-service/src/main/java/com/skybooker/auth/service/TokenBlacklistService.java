package com.skybooker.auth.service;

public interface TokenBlacklistService {
    void blacklist(String token);
    boolean isBlacklisted(String token);
    void cleanupExpiredTokens();
}
