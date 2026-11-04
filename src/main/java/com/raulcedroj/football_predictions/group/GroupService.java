package com.raulcedroj.football_predictions.group;

import com.raulcedroj.football_predictions.common.exception.BusinessRuleException;
import com.raulcedroj.football_predictions.common.exception.ForbiddenException;
import com.raulcedroj.football_predictions.common.exception.NotFoundException;
import com.raulcedroj.football_predictions.competition.SeasonEntity;
import com.raulcedroj.football_predictions.competition.SeasonRepository;
import com.raulcedroj.football_predictions.competition.SeasonStatus;
import com.raulcedroj.football_predictions.group.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private static final int MAX_CODE_ATTEMPTS = 5;

    private final FriendGroupRepository groupRepository;
    private final FriendGroupMemberRepository memberRepository;
    private final SeasonRepository seasonRepository;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final GroupProperties properties;

    public GroupService(FriendGroupRepository groupRepository,
                        FriendGroupMemberRepository memberRepository,
                        SeasonRepository seasonRepository,
                        InviteCodeGenerator inviteCodeGenerator,
                        GroupProperties properties) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.seasonRepository = seasonRepository;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.properties = properties;
    }

    @Transactional
    public GroupResponse create(Long userId, CreateGroupRequest request) {
        SeasonEntity season = seasonRepository.findWithCompetitionById(request.seasonId())
                .orElseThrow(() -> new NotFoundException("No existe la temporada " + request.seasonId()));

        if (season.getStatus() == SeasonStatus.FINISHED) {
            throw new BusinessRuleException("No se pueden crear grupos de una temporada terminada");
        }
        if (groupRepository.countByOwnerIdAndSeasonStatusNot(userId, SeasonStatus.FINISHED)
                >= properties.maxOwnedPerUser()) {
            throw new BusinessRuleException("Has alcanzado el máximo de "
                    + properties.maxOwnedPerUser() + " grupos activos");
        }

        FriendGroupEntity group = groupRepository.save(
                new FriendGroupEntity(request.name().trim(), generateUniqueInviteCode(), season, userId));
        memberRepository.save(new FriendGroupMemberEntity(group, userId));

        return GroupResponse.from(group, 1);
    }

    @Transactional
    public GroupResponse join(Long userId, JoinGroupRequest request) {
        String inviteCode = request.inviteCode().trim().toUpperCase(Locale.ROOT);
        FriendGroupEntity group = groupRepository.findByInviteCodeForUpdate(inviteCode)
                .orElseThrow(() -> new NotFoundException("El código de invitación no es válido"));

        long members = memberRepository.countByFriendGroupId(group.getId());

        if (memberRepository.existsByFriendGroupIdAndUserId(group.getId(), userId)) {
            return GroupResponse.from(group, members);
        }
        if (group.isReadOnly()) {
            throw new BusinessRuleException("La temporada de este grupo ya ha terminado");
        }
        if (members >= properties.maxMembers()) {
            throw new BusinessRuleException("El grupo está completo");
        }

        memberRepository.save(new FriendGroupMemberEntity(group, userId));
        return GroupResponse.from(group, members + 1);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> findMyGroups(Long userId) {
        List<FriendGroupEntity> groups = groupRepository.findAllByMemberUserId(userId);
        if (groups.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> memberCounts = memberRepository
                .countMembersByGroupIds(groups.stream().map(FriendGroupEntity::getId).toList()).stream()
                .collect(Collectors.toMap(GroupMemberCount::groupId, GroupMemberCount::count));

        return groups.stream()
                .map(group -> GroupResponse.from(group, memberCounts.getOrDefault(group.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDetailResponse findDetail(Long userId, Long groupId) {
        FriendGroupEntity group = findGroupForMember(groupId, userId);
        List<GroupMemberResponse> members = memberRepository.findMembers(groupId);
        return new GroupDetailResponse(GroupResponse.from(group, members.size()), members);
    }

    @Transactional
    public void leave(Long userId, Long groupId) {
        FriendGroupEntity group = findGroupForMember(groupId, userId);

        if (group.isOwnedBy(userId)) {
            throw new BusinessRuleException("El dueño no puede salir del grupo; puede eliminarlo");
        }
        if (group.isReadOnly()) {
            throw new BusinessRuleException("La temporada de este grupo ya ha terminado");
        }
        memberRepository.deleteByFriendGroupIdAndUserId(groupId, userId);
    }

    @Transactional
    public void delete(Long userId, Long groupId) {
        FriendGroupEntity group = findGroupForMember(groupId, userId);

        if (!group.isOwnedBy(userId)) {
            throw new ForbiddenException("Solo el dueño puede eliminar el grupo");
        }
        groupRepository.delete(group);
    }

    private FriendGroupEntity findGroupForMember(Long groupId, Long userId) {
        return groupRepository.findWithSeasonById(groupId)
                .filter(group -> memberRepository.existsByFriendGroupIdAndUserId(groupId, userId))
                .orElseThrow(() -> new NotFoundException("No existe el grupo " + groupId));
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = inviteCodeGenerator.generate();
            if (!groupRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("No se ha podido generar un código de invitación único");
    }
}