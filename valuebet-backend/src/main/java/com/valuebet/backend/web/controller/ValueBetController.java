package com.valuebet.backend.web.controller;

import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import com.valuebet.backend.service.ValueBetQueryService;
import com.valuebet.backend.service.ValueBetQueryService.ValueBetFilter;
import com.valuebet.backend.web.dto.ValueBetSummaryDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/value-bets")
@RequiredArgsConstructor
public class ValueBetController {

    private final ValueBetQueryService valueBetQueryService;

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
}
