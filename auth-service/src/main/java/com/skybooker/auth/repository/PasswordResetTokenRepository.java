package com.skybooker.auth.repository;

import com.skybooker.auth.entity.PasswordResetToken;
import com.skybooker.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findAllByUser(User user);

    List<PasswordResetToken> findAllByUserAndUsedFalse(User user);

    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    void deleteByExpiresAtBefore(LocalDateTime time);

    void deleteByUser(User user);
}