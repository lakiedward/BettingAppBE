package com.valuebet.backend.scheduler;

import com.valuebet.backend.config.ValuebetProperties;
import com.valuebet.backend.service.OddsIngestionService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OddsScheduler {

    private final OddsIngestionService oddsIngestionService;
    private final ValuebetProperties valuebetProperties;

    // @Scheduled(fixedDelayString = "PT15S")
    public void captureOdds() {
        Duration horizon = valuebetProperties.ingestionHorizon();
        try {
            oddsIngestionService.ingestUpcomingOdds(horizon);
        } catch (Exception ex) {
            log.error("Failed to ingest odds", ex);
        }
    }
}
