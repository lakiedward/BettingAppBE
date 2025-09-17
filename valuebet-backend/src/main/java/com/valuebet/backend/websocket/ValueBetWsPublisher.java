package com.valuebet.backend.websocket;

import com.valuebet.backend.domain.model.ValueOpportunity;
import com.valuebet.backend.web.dto.ValueBetSummaryDto;
import com.valuebet.backend.web.mapper.ValueBetSummaryMapper;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValueBetWsPublisher {

    private static final String DESTINATION = "/ws/value-bets";

    private final SimpMessagingTemplate messagingTemplate;
    private final ValueBetSummaryMapper valueBetSummaryMapper;

    public void publish(Collection<ValueOpportunity> opportunities) {
        if (opportunities == null || opportunities.isEmpty()) {
            return;
        }
        List<ValueBetSummaryDto> payload = opportunities.stream()
            .map(valueBetSummaryMapper::toDto)
            .collect(Collectors.toList());
        messagingTemplate.convertAndSend(DESTINATION, payload);
    }
}
