package com.raulcedroj.football_predictions.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "push_subscription")
public class PushSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 1000)
    private String endpoint;

    @Column(nullable = false, length = 200)
    private String p256dh;

    @Column(nullable = false, length = 50)
    private String auth;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PushSubscriptionEntity() {
    }

    public PushSubscriptionEntity(Long userId, String endpoint, String p256dh, String auth, Instant createdAt) {
        this.userId = userId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.createdAt = createdAt;
    }

    /** El mismo navegador puede pasar a otro usuario (cierra sesión y entra otro) o renovar sus claves. */
    void reassign(Long userId, String p256dh, String auth) {
        this.userId = userId;
        this.p256dh = p256dh;
        this.auth = auth;
    }

    public PushTarget target() {
        return new PushTarget(endpoint, p256dh, auth);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getEndpoint() { return endpoint; }
}
