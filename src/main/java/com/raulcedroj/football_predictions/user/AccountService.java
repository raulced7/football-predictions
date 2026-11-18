package com.raulcedroj.football_predictions.user;

import com.raulcedroj.football_predictions.auth.exception.WrongCurrentPasswordException;
import com.raulcedroj.football_predictions.avatar.AvatarService;
import com.raulcedroj.football_predictions.common.exception.NotFoundException;
import com.raulcedroj.football_predictions.group.FriendGroupEntity;
import com.raulcedroj.football_predictions.group.FriendGroupMemberRepository;
import com.raulcedroj.football_predictions.group.FriendGroupRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final FriendGroupRepository groupRepository;
    private final FriendGroupMemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarService avatarService;

    public AccountService(UserRepository userRepository, FriendGroupRepository groupRepository,
                          FriendGroupMemberRepository memberRepository, PasswordEncoder passwordEncoder,
                          AvatarService avatarService) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarService = avatarService;
    }

    /**
     * Borra la cuenta y todo lo suyo: pronósticos, membresías y códigos se borran en cascada en la
     * base de datos, y la foto de R2 cuando se confirma la transacción.
     *
     * <p>Sus grupos no desaparecen si hay más gente: pasan al miembro más antiguo. Solo se borran
     * los grupos en los que estaba solo.
     */
    @Transactional
    public void deleteAccount(Long userId, String password) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("No existe el usuario " + userId));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new WrongCurrentPasswordException();
        }

        for (FriendGroupEntity group : groupRepository.findByOwnerId(userId)) {
            memberRepository.findFirstByFriendGroupIdAndUserIdNotOrderByJoinedAtAsc(group.getId(), userId)
                    .ifPresentOrElse(
                            nextOwner -> group.transferOwnershipTo(nextOwner.getUserId()),
                            () -> groupRepository.delete(group));
        }

        avatarService.deleteAfterCommit(user.getAvatarUrl());
        userRepository.delete(user);
    }
}
