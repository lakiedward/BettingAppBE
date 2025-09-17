package com.valuebet.backend.domain.repository;

import com.valuebet.backend.domain.model.Event;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Optional<Event> findByExternalId(String externalId);
}
