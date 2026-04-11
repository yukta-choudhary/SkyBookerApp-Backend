package com.skybooker.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "password_reset_tokens",
        indexes = {
                @Index(name = "idx_reset_token", columnList = "token", unique = true),
                @Index(name = "idx_reset_user", columnList = "user_id"),
                @Index(name = "idx_reset_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @Column(name = "reset_id", nullable = false, updatable = false, length = 36)
    private String resetId;

    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    @Builder.Default
    private boolean used = false;

    @PrePersist
    public void onCreate() {
        if (this.resetId == null || this.resetId.isBlank()) {
            this.resetId = UUID.randomUUID().toString();
        }
    }
}