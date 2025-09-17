package com.valuebet.backend.web.controller;

import com.valuebet.backend.service.AuthenticationService;
import com.valuebet.backend.web.dto.AuthResponseDto;
import com.valuebet.backend.web.dto.LoginRequestDto;
import com.valuebet.backend.web.dto.RegisterRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponseDto register(@RequestBody RegisterRequestDto request) {
        return authenticationService.register(request);
    }

    @PostMapping("/login")
    public AuthResponseDto login(@RequestBody LoginRequestDto request) {
        return authenticationService.login(request);
    }
}
