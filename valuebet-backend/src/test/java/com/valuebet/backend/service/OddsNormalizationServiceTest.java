package com.valuebet.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.valuebet.backend.domain.model.Outcome;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OddsNormalizationServiceTest {

    private final OddsNormalizationService service = new OddsNormalizationService();

    @Test
    void removeVigShouldNormalizeToOne() {
        Map<Outcome, Double> implied = new EnumMap<>(Outcome.class);
        implied.put(Outcome.ONE, 0.45d);
        implied.put(Outcome.DRAW, 0.30d);
        implied.put(Outcome.TWO, 0.35d);

        Map<Outcome, Double> normalized = service.removeVig(implied);

        double sum = normalized.values().stream().mapToDouble(Double::doubleValue).sum();
        assertThat(sum).isCloseTo(1.0d, within(1e-9));
        assertThat(normalized.get(Outcome.ONE)).isCloseTo(0.409090909d, within(1e-9));
        assertThat(normalized.get(Outcome.DRAW)).isCloseTo(0.272727272d, within(1e-9));
        assertThat(normalized.get(Outcome.TWO)).isCloseTo(0.318181818d, within(1e-9));
    }
}
