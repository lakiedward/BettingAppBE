package com.valuebet.backend.domain.repository;

import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.OddsSnapshot;
import com.valuebet.backend.domain.model.Outcome;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OddsSnapshotRepository extends JpaRepository<OddsSnapshot, Long> {

    List<OddsSnapshot> findTop200ByEventIdOrderByCapturedAtDesc(UUID eventId);

    Optional<OddsSnapshot> findFirstByEventIdAndMarketTypeAndOutcomeAndClosingLineTrueOrderByCapturedAtDesc(
        UUID eventId,
        MarketType marketType,
        Outcome outcome
    );
}
