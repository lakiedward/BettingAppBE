package com.valuebet.backend.service;

import com.valuebet.backend.domain.model.Event;
import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.Outcome;
import com.valuebet.backend.domain.model.ValueOpportunity;
import com.valuebet.backend.domain.repository.ValueOpportunityRepository;
import com.valuebet.backend.web.dto.ValueBetSummaryDto;
import com.valuebet.backend.web.mapper.ValueBetSummaryMapper;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValueBetQueryService {

    private final ValueOpportunityRepository valueOpportunityRepository;
    private final ValueBetSummaryMapper valueBetSummaryMapper;

    public Page<ValueBetSummaryDto> findValueBets(ValueBetFilter filter, Pageable pageable) {
        Specification<ValueOpportunity> specification = buildSpecification(filter);
        return valueOpportunityRepository.findAll(specification, pageable)
            .map(valueBetSummaryMapper::toDto);
    }

    private Specification<ValueOpportunity> buildSpecification(ValueBetFilter filter) {
        return (root, query, cb) -> {
            Join<ValueOpportunity, Event> eventJoin = root.join("event");
            List<Predicate> predicates = new ArrayList<>();

            if (filter.league() != null && !filter.league().isBlank()) {
                predicates.add(cb.like(cb.lower(eventJoin.get("competition")), like(filter.league())));
            }
            if (filter.marketType() != null) {
                predicates.add(cb.equal(root.get("marketType"), filter.marketType()));
            }
            if (filter.outcome() != null) {
                predicates.add(cb.equal(root.get("outcome"), filter.outcome()));
            }
            if (filter.minEdge() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("edge"), BigDecimal.valueOf(filter.minEdge())));
            }
            if (filter.eventId() != null) {
                predicates.add(cb.equal(eventJoin.get("id"), filter.eventId()));
            }
            if (filter.startFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(eventJoin.get("startTime"), filter.startFrom()));
            }
            if (filter.startTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(eventJoin.get("startTime"), filter.startTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String like(String value) {
        return "%" + value.toLowerCase(Locale.ROOT) + "%";
    }

    @Builder
    public record ValueBetFilter(
        String league,
        MarketType marketType,
        Outcome outcome,
        Double minEdge,
        OffsetDateTime startFrom,
        OffsetDateTime startTo,
        UUID eventId
    ) {
    }
}
