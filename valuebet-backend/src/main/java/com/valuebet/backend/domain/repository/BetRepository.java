package com.valuebet.backend.domain.repository;

import com.valuebet.backend.domain.model.Bet;
import com.valuebet.backend.domain.model.BetResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BetRepository extends JpaRepository<Bet, Long> {

    List<Bet> findByResultIn(List<BetResult> results);
}
