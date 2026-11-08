package com.raulcedroj.football_predictions.sync;

import com.raulcedroj.football_predictions.sync.provider.ExternalTeam;
import com.raulcedroj.football_predictions.team.TeamEntity;
import com.raulcedroj.football_predictions.team.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Crea o actualiza los equipos que llegan del proveedor. Se ejecuta dentro de la transacción de
 * {@link FixtureSyncWriter#apply}, por eso no lleva {@code @Transactional} propio.
 */
@Component
class TeamSyncWriter {

    private static final Logger log = LoggerFactory.getLogger(TeamSyncWriter.class);

    private final TeamRepository teamRepository;

    TeamSyncWriter(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /** Devuelve los equipos indexados por su id en el proveedor. */
    Map<String, TeamEntity> sync(Collection<ExternalTeam> externalTeams) {
        Map<String, ExternalTeam> byExternalId = externalTeams.stream()
                .collect(Collectors.toMap(ExternalTeam::externalId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        Map<String, TeamEntity> teams = teamRepository.findByExternalIdIn(byExternalId.keySet()).stream()
                .collect(Collectors.toMap(TeamEntity::getExternalId, Function.identity(), (a, b) -> a, HashMap::new));

        List<ExternalTeam> missing = byExternalId.values().stream()
                .filter(team -> !teams.containsKey(team.externalId()))
                .toList();
        if (!missing.isEmpty()) {
            linkOrCreate(missing, teams);
        }

        int updated = 0;
        for (ExternalTeam external : byExternalId.values()) {
            if (teams.get(external.externalId()).updateFromProvider(external.externalId(), external.name(),
                    external.shortName(), external.tla(), external.crestUrl())) {
                updated++;
            }
        }
        if (updated > 0) {
            log.info("{} teams created or updated from the provider", updated);
        }
        return teams;
    }

    /**
     * Los equipos creados antes de tener datos del proveedor (a mano o por la migración V6) se vinculan
     * por nombre en lugar de duplicarse. Primero se busca por el nombre completo y luego por el corto.
     */
    private void linkOrCreate(List<ExternalTeam> missing, Map<String, TeamEntity> teams) {
        Set<String> names = missing.stream()
                .flatMap(team -> Stream.of(team.name(), team.shortName()))
                .filter(Objects::nonNull)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        Map<String, TeamEntity> unlinkedByName = teamRepository.findUnlinkedByLowerNameIn(names).stream()
                .collect(Collectors.toMap(team -> team.getName().toLowerCase(Locale.ROOT), Function.identity()));

        for (ExternalTeam external : missing) {
            TeamEntity team = unlinkedByName.remove(external.name().toLowerCase(Locale.ROOT));
            if (team == null && external.shortName() != null) {
                team = unlinkedByName.remove(external.shortName().toLowerCase(Locale.ROOT));
            }
            if (team == null) {
                team = teamRepository.save(new TeamEntity(external.externalId(), external.name(),
                        external.shortName(), external.tla(), external.crestUrl()));
            }
            teams.put(external.externalId(), team);
        }
    }
}