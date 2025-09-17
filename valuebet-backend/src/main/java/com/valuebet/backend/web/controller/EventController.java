package com.valuebet.backend.web.controller;

import com.valuebet.backend.service.EventQueryService;
import com.valuebet.backend.web.dto.EventOddsDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventQueryService eventQueryService;

    @GetMapping("/{id}")
    public EventOddsDto getEvent(@PathVariable UUID id) {
        return eventQueryService.getEventOdds(id);
    }
}
