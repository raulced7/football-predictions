package com.raulcedroj.football_predictions.auth.code;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "email_code")
public class EmailCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CodePurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected EmailCodeEntity() {
    }

    EmailCodeEntity(Long userId, CodePurpose purpose, String codeHash, Instant createdAt, Instant expiresAt) {
        this.userId = userId;
        this.purpose = purpose;
        renew(codeHash, createdAt, expiresAt);
    }

    /** Sustituye el código por uno nuevo y reinicia los intentos. */
    void renew(String codeHash, Instant createdAt, Instant expiresAt) {
        this.codeHash = codeHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.attempts = 0;
    }

    boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    /** Devuelve los intentos fallidos acumulados. */
    int registerFailedAttempt() {
        return ++attempts;
    }

    String getCodeHash() { return codeHash; }
    Instant getCreatedAt() { return createdAt; }
}