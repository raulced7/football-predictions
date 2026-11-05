package com.raulcedroj.football_predictions.standings;

import com.raulcedroj.football_predictions.common.exception.NotFoundException;
import com.raulcedroj.football_predictions.competition.RoundRepository;
import com.raulcedroj.football_predictions.fixture.FixtureEntity;
import com.raulcedroj.football_predictions.fixture.FixtureRepository;
import com.raulcedroj.football_predictions.fixture.dto.FixtureResponse;
import com.raulcedroj.football_predictions.group.FriendGroupEntity;
import com.raulcedroj.football_predictions.group.GroupService;
import com.raulcedroj.football_predictions.standings.StandingsRepository.MemberPredictionRow;
import com.raulcedroj.football_predictions.standings.dto.GroupFixturePredictions;
import com.raulcedroj.football_predictions.standings.dto.MemberPrediction;
import com.raulcedroj.football_predictions.standings.dto.StandingEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StandingsService {

    private final StandingsRepository standingsRepository;
    private final GroupService groupService;
    private final RoundRepository roundRepository;
    private final FixtureRepository fixtureRepository;
    private final Clock clock;

    public StandingsService(StandingsRepository standingsRepository, GroupService groupService,
                            RoundRepository roundRepository, FixtureRepository fixtureRepository,
                            Clock clock) {
        this.standingsRepository = standingsRepository;
        this.groupService = groupService;
        this.roundRepository = roundRepository;
        this.fixtureRepository = fixtureRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<StandingEntry> general(Long userId, Long groupId) {
        groupService.getGroupForMember(groupId, userId);
        return standingsRepository.findGeneral(groupId);
    }

    @Transactional(readOnly = true)
    public List<StandingEntry> round(Long userId, Long groupId, Long roundId) {
        FriendGroupEntity group = groupService.getGroupForMember(groupId, userId);
        requireRoundOfGroup(group, roundId);
        return standingsRepository.findByRound(groupId, roundId);
    }

    @Transactional(readOnly = true)
    public List<GroupFixturePredictions> roundPredictions(Long userId, Long groupId, Long roundId) {
        FriendGroupEntity group = groupService.getGroupForMember(groupId, userId);
        requireRoundOfGroup(group, roundId);

        Map<Long, List<MemberPrediction>> predictionsByFixture = standingsRepository
                .findMemberPredictions(groupId, roundId).stream()
                .collect(Collectors.groupingBy(MemberPredictionRow::fixtureId,
                        Collectors.mapping(MemberPredictionRow::prediction, Collectors.toList())));

        Instant now = Instant.now(clock);
        return fixtureRepository.findByScoringRoundIdOrderByKickoffAtAsc(roundId).stream()
                .map(fixture -> toFixturePredictions(fixture,
                        predictionsByFixture.getOrDefault(fixture.getId(), List.of()), now))
                .toList();
    }

    private GroupFixturePredictions toFixturePredictions(FixtureEntity fixture,
                                                         List<MemberPrediction> predictions,
                                                         Instant now) {
        boolean revealed = fixture.hasStarted(now);
        return new GroupFixturePredictions(
                FixtureResponse.from(fixture),
                revealed,
                predictions.size(),
                revealed ? predictions : List.of());
    }

    private void requireRoundOfGroup(FriendGroupEntity group, Long roundId) {
        roundRepository.findById(roundId)
                .filter(round -> round.getSeason().getId().equals(group.getSeason().getId()))
                .orElseThrow(() -> new NotFoundException("No existe la jornada " + roundId + " en este grupo"));
    }
}