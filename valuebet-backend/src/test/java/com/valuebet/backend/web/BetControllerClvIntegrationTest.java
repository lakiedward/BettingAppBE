package com.valuebet.backend.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valuebet.backend.domain.model.BetResult;
import com.valuebet.backend.domain.model.Bookmaker;
import com.valuebet.backend.domain.model.Event;
import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import com.valuebet.backend.domain.repository.BetRepository;
import com.valuebet.backend.domain.repository.BookmakerRepository;
import com.valuebet.backend.domain.repository.EventRepository;
import com.valuebet.backend.domain.repository.OddsSnapshotRepository;
import com.valuebet.backend.domain.repository.ValueOpportunityRepository;
import com.valuebet.backend.test.AbstractPostgresContainerTest;
import com.valuebet.backend.web.dto.ClvSummaryDto;
import com.valuebet.backend.web.dto.CreateBetRequestDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BetControllerClvIntegrationTest extends AbstractPostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BetRepository betRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private BookmakerRepository bookmakerRepository;

    @Autowired
    private OddsSnapshotRepository oddsSnapshotRepository;

    @Autowired
    private ValueOpportunityRepository valueOpportunityRepository;

    @BeforeEach
    void setUp() {
        betRepository.deleteAll();
        oddsSnapshotRepository.deleteAll();
        valueOpportunityRepository.deleteAll();
        eventRepository.deleteAll();
        bookmakerRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void clvShouldConsiderClosingLineAndProfit() throws Exception {
        Event event = eventRepository.save(Event.builder()
            .externalId("test")
            .name("Test Event")
            .competition("League")
            .homeTeam("Home")
            .awayTeam("Away")
            .startTime(OffsetDateTime.now())
            .build());

        Bookmaker bookmaker = bookmakerRepository.save(Bookmaker.builder()
            .externalKey("sharp")
            .build());

        // Closing line snapshots
        oddsSnapshotRepository.saveAll(java.util.List.of(
            com.valuebet.backend.domain.model.OddsSnapshot.builder()
                .event(event)
                .marketType(MarketType.ONE_X_TWO)
                .outcome(Outcome.ONE)
                .bookmaker("sharp")
                .odds(BigDecimal.valueOf(2.20d))
                .capturedAt(Instant.now())
                .closingLine(true)
                .build(),
            com.valuebet.backend.domain.model.OddsSnapshot.builder()
                .event(event)
                .marketType(MarketType.ONE_X_TWO)
                .outcome(Outcome.DRAW)
                .bookmaker("sharp")
                .odds(BigDecimal.valueOf(3.30d))
                .capturedAt(Instant.now())
                .closingLine(true)
                .build(),
            com.valuebet.backend.domain.model.OddsSnapshot.builder()
                .event(event)
                .marketType(MarketType.ONE_X_TWO)
                .outcome(Outcome.TWO)
                .bookmaker("sharp")
                .odds(BigDecimal.valueOf(3.00d))
                .capturedAt(Instant.now())
                .closingLine(true)
                .build()
        ));

        CreateBetRequestDto bet1 = new CreateBetRequestDto(event.getId(), MarketType.ONE_X_TWO, Outcome.ONE,
            BigDecimal.valueOf(100), BigDecimal.valueOf(2.40), bookmaker.getId());
        CreateBetRequestDto bet2 = new CreateBetRequestDto(event.getId(), MarketType.ONE_X_TWO, Outcome.DRAW,
            BigDecimal.valueOf(50), BigDecimal.valueOf(3.10), bookmaker.getId());
        CreateBetRequestDto bet3 = new CreateBetRequestDto(event.getId(), MarketType.ONE_X_TWO, Outcome.TWO,
            BigDecimal.valueOf(75), BigDecimal.valueOf(2.80), bookmaker.getId());

        mockMvc.perform(post("/api/bets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bet1)))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/bets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bet2)))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/bets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bet3)))
            .andExpect(status().isCreated());

        // Update bet results: one win, one loss, one pending
        var bets = betRepository.findAll();
        bets.get(0).setResult(BetResult.WON);
        bets.get(1).setResult(BetResult.LOST);
        bets.get(2).setResult(BetResult.PENDING);
        betRepository.saveAll(bets);

        String response = mockMvc.perform(get("/api/users/me/clv"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        ClvSummaryDto summary = objectMapper.readValue(response, ClvSummaryDto.class);

        double expectedClv = (((2.20 / 2.40) - 1.0) + ((3.30 / 3.10) - 1.0) + ((3.00 / 2.80) - 1.0)) / 3.0;
        double totalStake = 100 + 50 + 75;
        double totalProfit = 100 * (2.40 - 1.0) - 50 + 0; // win first bet, lose second, pending third
        double expectedRoi = totalProfit / totalStake;

        assertThat(summary.betsCount()).isEqualTo(3);
        assertThat(summary.averageClv()).isCloseTo(expectedClv, within(1e-6));
        assertThat(summary.roi()).isCloseTo(expectedRoi, within(1e-6));
    }
}
