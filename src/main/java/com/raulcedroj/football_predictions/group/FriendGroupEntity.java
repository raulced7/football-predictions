package com.raulcedroj.football_predictions.group;

import com.raulcedroj.football_predictions.competition.SeasonEntity;
import com.raulcedroj.football_predictions.competition.SeasonStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "friend_group")
public class FriendGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "invite_code", nullable = false, unique = true, length = 12)
    private String inviteCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id")
    private SeasonEntity season;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FriendGroupEntity() {
    }

    public FriendGroupEntity(String name, String inviteCode, SeasonEntity season, Long ownerId) {
        this.name = name;
        this.inviteCode = inviteCode;
        this.season = season;
        this.ownerId = ownerId;
        this.createdAt = Instant.now();
    }

    public boolean isOwnedBy(Long userId) {
        return ownerId.equals(userId);
    }

    public boolean isReadOnly() {
        return season.getStatus() == SeasonStatus.FINISHED;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getInviteCode() { return inviteCode; }
    public SeasonEntity getSeason() { return season; }
    public Long getOwnerId() { return ownerId; }
    public Instant getCreatedAt() { return createdAt; }
}