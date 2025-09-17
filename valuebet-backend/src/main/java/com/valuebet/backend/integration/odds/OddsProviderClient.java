package com.valuebet.backend.integration.odds;

import java.time.Duration;
import java.util.List;

public interface OddsProviderClient {

    List<ProviderOddsDto> fetchUpcomingOdds(Duration horizon);
}
