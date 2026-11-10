package com.raulcedroj.football_predictions.auth.code;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailCodeRepository extends JpaRepository<EmailCodeEntity, Long> {

    Optional<EmailCodeEntity> findByUserIdAndPurpose(Long userId, CodePurpose purpose);
}