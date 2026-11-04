package com.raulcedroj.football_predictions.group;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "friend_group_member")
public class FriendGroupMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "friend_group_id")
    private FriendGroupEntity friendGroup;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected FriendGroupMemberEntity() {
    }

    public FriendGroupMemberEntity(FriendGroupEntity friendGroup, Long userId) {
        this.friendGroup = friendGroup;
        this.userId = userId;
        this.joinedAt = Instant.now();
    }

    public Long getId() { return id; }
    public FriendGroupEntity getFriendGroup() { return friendGroup; }
    public Long getUserId() { return userId; }
    public Instant getJoinedAt() { return joinedAt; }
}