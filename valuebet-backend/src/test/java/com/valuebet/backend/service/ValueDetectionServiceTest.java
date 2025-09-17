package com.valuebet.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.valuebet.backend.domain.model.Bookmaker;
import com.valuebet.backend.domain.model.Event;
import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import com.valuebet.backend.domain.model.ValueOpportunity;
import com.valuebet.backend.domain.repository.BookmakerRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ValueDetectionServiceTest {

    private final BookmakerRepository bookmakerRepository = Mockito.mock(BookmakerRepository.class);
    private final ValueDetectionService service = new ValueDetectionService(bookmakerRepository);

    @BeforeEach
    void setup() {
        Bookmaker bookmaker = Bookmaker.builder()
            .id(42L)
            .externalKey("sharp")
            .build();
        Mockito.when(bookmakerRepository.getReferenceById(42L)).thenReturn(bookmaker);
    }

    @Test
    void detectShouldComputeEdgeAndBetDownLines() {
        Event event = Event.builder()
            .id(UUID.randomUUID())
            .startTime(OffsetDateTime.now())
            .build();

        double odds = 2.40d;
        double pTrue = 0.50d;

        ValueOpportunity opportunity = service.detect(
            event,
            MarketType.ONE_X_TWO,
            Outcome.ONE,
            odds,
            42L,
            pTrue
        );

        assertThat(opportunity.getEdge()).isEqualByComparingTo(BigDecimal.valueOf(0.20d).setScale(4));
        assertThat(opportunity.getMinOddsEv0()).isEqualByComparingTo(BigDecimal.valueOf(2.0d).setScale(3));
        assertThat(opportunity.getMinOddsEv2()).isEqualByComparingTo(BigDecimal.valueOf(2.04d).setScale(3));
        assertThat(opportunity.getKellyFraction()).isEqualByComparingTo(BigDecimal.valueOf(0.1d).setScale(4));
        assertThat(opportunity.getBookmaker().getExternalKey()).isEqualTo("sharp");
    }
}
