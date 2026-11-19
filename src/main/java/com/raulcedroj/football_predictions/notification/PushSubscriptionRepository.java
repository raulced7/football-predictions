package com.raulcedroj.football_predictions.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, Long> {

    Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);

    List<PushSubscriptionEntity> findByUserId(Long userId);

    long deleteByEndpointAndUserId(String endpoint, Long userId);
}
