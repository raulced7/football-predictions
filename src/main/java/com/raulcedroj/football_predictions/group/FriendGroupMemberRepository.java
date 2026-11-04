package com.raulcedroj.football_predictions.group;

import com.raulcedroj.football_predictions.group.dto.GroupMemberCount;
import com.raulcedroj.football_predictions.group.dto.GroupMemberResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface FriendGroupMemberRepository extends JpaRepository<FriendGroupMemberEntity, Long> {

    boolean existsByFriendGroupIdAndUserId(Long friendGroupId, Long userId);

    long countByFriendGroupId(Long friendGroupId);

    void deleteByFriendGroupIdAndUserId(Long friendGroupId, Long userId);

    @Query("""
            SELECT new com.raulcedroj.football_predictions.group.dto.GroupMemberResponse(
                       u.id, u.displayName, m.joinedAt)
            FROM FriendGroupMemberEntity m
            JOIN UserEntity u ON u.id = m.userId
            WHERE m.friendGroup.id = :groupId
            ORDER BY m.joinedAt
            """)
    List<GroupMemberResponse> findMembers(@Param("groupId") Long groupId);

    @Query("""
            SELECT new com.raulcedroj.football_predictions.group.dto.GroupMemberCount(
                       m.friendGroup.id, COUNT(m))
            FROM FriendGroupMemberEntity m
            WHERE m.friendGroup.id IN :groupIds
            GROUP BY m.friendGroup.id
            """)
    List<GroupMemberCount> countMembersByGroupIds(@Param("groupIds") Collection<Long> groupIds);
}