package com.valuebet.backend.web.controller;

import com.valuebet.backend.config.ValuebetProperties;
import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import com.valuebet.backend.integration.odds.OddsProviderClient;
import com.valuebet.backend.integration.odds.ProviderOddsDto;
import com.valuebet.backend.service.OddsIngestionService;
import com.valuebet.backend.service.ValueBetQueryService;
import com.valuebet.backend.service.ValueBetQueryService.ValueBetFilter;
import com.valuebet.backend.web.dto.ValueBetSummaryDto;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/value-bets")
@RequiredArgsConstructor
@Slf4j
public class ValueBetController {

    private final ValueBetQueryService valueBetQueryService;
    private final OddsProviderClient oddsProviderClient;
    private final OddsIngestionService oddsIngestionService;
    private final ValuebetProperties valuebetProperties;

    @GetMapping
    public Page<ValueBetSummaryDto> getValueBets(
        @RequestParam(required = false) String league,
        @RequestParam(required = false) MarketType marketType,
        @RequestParam(required = false) Outcome outcome,
        @RequestParam(required = false) Double minEdge,
        @RequestParam(required = false) UUID eventId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTo,
        Pageable pageable
    ) {
        ValueBetFilter filter = ValueBetFilter.builder()
            .league(league)
            .marketType(marketType)
            .outcome(outcome)
            .minEdge(minEdge)
            .eventId(eventId)
            .startFrom(startFrom)
            .startTo(startTo)
            .build();
        return valueBetQueryService.findValueBets(filter, pageable);
}

    @GetMapping("/test-api")
    public ResponseEntity<String> testApiConnection() {
        try {
            Duration testHorizon = Duration.ofHours(24);
            List<ProviderOddsDto> odds = oddsProviderClient.fetchUpcomingOdds(testHorizon);
            return ResponseEntity.ok("API working! Fetched " + odds.size() + " odds entries");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("API Error: " + e.getMessage());
        }
    }

    @PostMapping("/trigger-odds-fetch")
    public ResponseEntity<Map<String, Object>> triggerOddsFetch() {
        log.info("=== MANUAL ODDS FETCH STARTED ===");
        long startTime = System.currentTimeMillis();

        try {
            Duration horizon = valuebetProperties.ingestionHorizon();
            log.info("Fetching odds with horizon: {}", horizon);

            var result = oddsIngestionService.ingestUpcomingOdds(horizon);
            long duration = System.currentTimeMillis() - startTime;

            log.info("=== MANUAL ODDS FETCH COMPLETED in {}ms ===", duration);
            log.info("Markets processed: {}", result.size());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("Fetched %d markets in %dms", result.size(), duration));
            response.put("marketsProcessed", result.size());
            response.put("duration", duration);
            response.put("timestamp", Instant.now());

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("=== MANUAL ODDS FETCH FAILED after {}ms ===", duration, ex);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed: " + ex.getMessage());
            errorResponse.put("duration", duration);
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
