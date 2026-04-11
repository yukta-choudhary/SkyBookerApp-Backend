package com.skybooker.auth.repository;

import com.skybooker.auth.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, String> {
    boolean existsByTokenValue(String tokenValue);
    void deleteByExpiresAtBefore(java.time.LocalDateTime time);
}
