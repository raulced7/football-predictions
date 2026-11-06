package com.raulcedroj.football_predictions.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query("SELECT u FROM UserEntity u WHERE lower(u.email) = lower(:email)")
    Optional<UserEntity> findByEmail(@Param("email") String email);

    @Query("SELECT count(u) > 0 FROM UserEntity u WHERE lower(u.email) = lower(:email)")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT count(u) > 0 FROM UserEntity u WHERE lower(u.displayName) = lower(:displayName)")
    boolean existsByDisplayName(@Param("displayName") String displayName);
}