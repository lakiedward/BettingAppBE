package com.valuebet.backend.service;

import com.valuebet.backend.domain.model.Bet;
import com.valuebet.backend.domain.model.BetResult;
import com.valuebet.backend.domain.model.Bookmaker;
import com.valuebet.backend.domain.model.Event;
import com.valuebet.backend.domain.model.MarketType;
import com.valuebet.backend.domain.model.OddsSnapshot;
import com.valuebet.backend.domain.model.Outcome;
import com.valuebet.backend.domain.repository.BetRepository;
import com.valuebet.backend.domain.repository.BookmakerRepository;
import com.valuebet.backend.domain.repository.EventRepository;
import com.valuebet.backend.domain.repository.OddsSnapshotRepository;
import com.valuebet.backend.web.dto.BetResponseDto;
import com.valuebet.backend.web.dto.ClvSummaryDto;
import com.valuebet.backend.web.dto.CreateBetRequestDto;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BetService {

    private static final EnumSet<BetResult> CLV_STATUSES = EnumSet.of(BetResult.PENDING, BetResult.WON, BetResult.LOST);

    private final BetRepository betRepository;
    private final EventRepository eventRepository;
    private final BookmakerRepository bookmakerRepository;
    private final OddsSnapshotRepository oddsSnapshotRepository;

    public BetResponseDto createBet(CreateBetRequestDto request) {
        validateRequest(request);
        Event event = eventRepository.findById(request.eventId())
            .orElseThrow(() -> new EntityNotFoundException("Event not found: " + request.eventId()));
        Bookmaker bookmaker = null;
        if (request.bookmakerId() != null) {
            bookmaker = bookmakerRepository.findById(request.bookmakerId())
                .orElseThrow(() -> new EntityNotFoundException("Bookmaker not found: " + request.bookmakerId()));
        }

        Bet bet = Bet.builder()
            .event(event)
            .marketType(request.marketType())
            .outcome(request.outcome())
            .stake(scaleMoney(request.stake()))
            .oddsTaken(scaleOdds(request.oddsTaken()))
            .bookmaker(bookmaker)
            .result(BetResult.PENDING)
            .build();

        Bet saved = betRepository.save(bet);
        return toDto(saved);
    }

    public ClvSummaryDto calculateClvAndRoi() {
        List<Bet> bets = betRepository.findByResultIn(List.copyOf(CLV_STATUSES));
        if (bets.isEmpty()) {
            return new ClvSummaryDto(0.0d, 0.0d, 0);
        }

        double clvSum = 0.0d;
        int clvCount = 0;
        BigDecimal totalStake = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;

        for (Bet bet : bets) {
            BigDecimal stake = optionalMoney(bet.getStake());
            BigDecimal oddsTaken = optionalOdds(bet.getOddsTaken());
            totalStake = totalStake.add(stake);

            double profit = calculateProfit(bet.getResult(), stake, oddsTaken);
            totalProfit = totalProfit.add(BigDecimal.valueOf(profit));

            Optional<OddsSnapshot> closingLine = oddsSnapshotRepository
                .findFirstByEventIdAndMarketTypeAndOutcomeAndClosingLineTrueOrderByCapturedAtDesc(
                    bet.getEvent().getId(),
                    bet.getMarketType(),
                    bet.getOutcome()
                );
            if (closingLine.isPresent() && closingLine.get().getOdds() != null && oddsTaken.doubleValue() > 0.0d) {
                double closingOdds = closingLine.get().getOdds().doubleValue();
                double clv = closingOdds / oddsTaken.doubleValue() - 1.0d;
                clvSum += clv;
                clvCount++;
            }
        }

        double averageClv = clvCount == 0 ? 0.0d : clvSum / clvCount;
        double roi = totalStake.compareTo(BigDecimal.ZERO) == 0
            ? 0.0d
            : totalProfit.doubleValue() / totalStake.doubleValue();

        return new ClvSummaryDto(averageClv, roi, bets.size());
    }

    private void validateRequest(CreateBetRequestDto request) {
        if (request.eventId() == null) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (request.marketType() == null) {
            throw new IllegalArgumentException("marketType is required");
        }
        if (request.outcome() == null) {
            throw new IllegalArgumentException("outcome is required");
        }
        if (request.stake() == null || request.stake().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("stake must be positive");
        }
        if (request.oddsTaken() == null || request.oddsTaken().compareTo(BigDecimal.ONE) <= 0) {
            throw new IllegalArgumentException("oddsTaken must be greater than 1.0");
        }
    }

    private BetResponseDto toDto(Bet bet) {
        return new BetResponseDto(
            bet.getId(),
            bet.getEvent().getId(),
            bet.getMarketType(),
            bet.getOutcome(),
            bet.getStake(),
            bet.getOddsTaken(),
            bet.getBookmaker() != null ? bet.getBookmaker().getId() : null,
            bet.getResult(),
            bet.getCreatedAt()
        );
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleOdds(BigDecimal value) {
        return value.setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal optionalMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal optionalOdds(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
    }

    private double calculateProfit(BetResult result, BigDecimal stake, BigDecimal oddsTaken) {
        if (result == BetResult.WON) {
            return stake.doubleValue() * (oddsTaken.doubleValue() - 1.0d);
        }
        if (result == BetResult.LOST) {
            return -stake.doubleValue();
        }
        return 0.0d;
    }
}
