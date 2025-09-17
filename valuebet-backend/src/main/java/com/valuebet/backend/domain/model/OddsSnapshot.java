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
@Table(name = "odds_snapshot")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OddsSnapshot {

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

    @Column(name = "bookmaker", nullable = false, length = 100)
    private String bookmaker;

    @Column(name = "odds", nullable = false, precision = 8, scale = 3)
    private BigDecimal odds;

    @Column(name = "implied_probability", precision = 6, scale = 4)
    private BigDecimal impliedProbability;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "closing_line", nullable = false)
    private boolean closingLine;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (capturedAt == null) {
            capturedAt = now;
        }
        createdAt = now;
    }
}
