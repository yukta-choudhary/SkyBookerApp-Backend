package com.skybooker.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "token_blacklist")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenBlacklist {

    @Id
    @Column(name = "blacklist_id", nullable = false, updatable = false, length = 36)
    private String blacklistId;

    @Column(name = "token_value", nullable = false, unique = true, length = 1000)
    private String tokenValue;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    public void onCreate() {
        if (this.blacklistId == null) {
            this.blacklistId = UUID.randomUUID().toString();
        }
    }
}
