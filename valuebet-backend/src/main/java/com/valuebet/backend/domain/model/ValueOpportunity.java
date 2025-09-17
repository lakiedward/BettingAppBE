package com.valuebet.backend.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "value_opportunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValueOpportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false, columnDefinition = "market_type")
    private MarketType marketType;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", columnDefinition = "outcome")
    private Outcome outcome;

    @Column(name = "line", precision = 8, scale = 3)
    private BigDecimal line;

    @Column(name = "odds", nullable = false, precision = 8, scale = 3)
    private BigDecimal odds;

    @Column(name = "true_probability", nullable = false, precision = 6, scale = 4)
    private BigDecimal trueProbability;

    @Column(name = "edge", nullable = false, precision = 6, scale = 4)
    private BigDecimal edge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookmaker_id")
    private Bookmaker bookmaker;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "min_odds_ev_0", precision = 8, scale = 3)
    private BigDecimal minOddsEv0;

    @Column(name = "min_odds_ev_2", precision = 8, scale = 3)
    private BigDecimal minOddsEv2;

    @Column(name = "kelly_fraction", precision = 6, scale = 4)
    private BigDecimal kellyFraction;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }
}
