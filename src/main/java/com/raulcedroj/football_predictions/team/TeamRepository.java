package com.raulcedroj.football_predictions.team;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {

    Optional<TeamEntity> findByNameIgnoreCase(String name);

    List<TeamEntity> findByExternalIdIn(Collection<String> externalIds);

    /** Equipos creados antes de vincularse al proveedor, buscados por cualquiera de sus nombres. */
    @Query("""
            SELECT t FROM TeamEntity t
            WHERE t.externalId IS NULL AND lower(t.name) IN :names
            """)
    List<TeamEntity> findUnlinkedByLowerNameIn(@Param("names") Collection<String> lowerCaseNames);
}