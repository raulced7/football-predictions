package com.raulcedroj.football_predictions.group;

import com.raulcedroj.football_predictions.competition.SeasonStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FriendGroupRepository extends JpaRepository<FriendGroupEntity, Long> {

    boolean existsByInviteCode(String inviteCode);

    long countByOwnerIdAndSeasonStatusNot(Long ownerId, SeasonStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM FriendGroupEntity g WHERE g.inviteCode = :inviteCode")
    Optional<FriendGroupEntity> findByInviteCodeForUpdate(@Param("inviteCode") String inviteCode);

    @EntityGraph(attributePaths = {"season", "season.competition"})
    Optional<FriendGroupEntity> findWithSeasonById(Long id);

    @Query("""
            SELECT g FROM FriendGroupEntity g
            JOIN FETCH g.season s
            JOIN FETCH s.competition
            WHERE EXISTS (SELECT 1 FROM FriendGroupMemberEntity m
                          WHERE m.friendGroup = g AND m.userId = :userId)
            ORDER BY g.createdAt DESC
            """)
    List<FriendGroupEntity> findAllByMemberUserId(@Param("userId") Long userId);
}