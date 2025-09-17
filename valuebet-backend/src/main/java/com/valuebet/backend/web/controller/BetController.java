package com.valuebet.backend.web.controller;

import com.valuebet.backend.service.BetService;
import com.valuebet.backend.web.dto.BetResponseDto;
import com.valuebet.backend.web.dto.ClvSummaryDto;
import com.valuebet.backend.web.dto.CreateBetRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BetController {

    private final BetService betService;

    @PostMapping("/bets")
    @ResponseStatus(HttpStatus.CREATED)
    public BetResponseDto placeBet(@RequestBody CreateBetRequestDto request) {
        return betService.createBet(request);
    }

    @GetMapping("/users/me/clv")
    public ClvSummaryDto getClv() {
        return betService.calculateClvAndRoi();
    }
}
