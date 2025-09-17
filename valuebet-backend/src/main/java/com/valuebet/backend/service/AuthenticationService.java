package com.valuebet.backend.service;

import com.valuebet.backend.config.JwtProperties;
import com.valuebet.backend.domain.model.AppUser;
import com.valuebet.backend.domain.model.UserRole;
import com.valuebet.backend.domain.repository.AppUserRepository;
import com.valuebet.backend.security.JwtService;
import com.valuebet.backend.web.dto.AuthResponseDto;
import com.valuebet.backend.web.dto.LoginRequestDto;
import com.valuebet.backend.web.dto.RegisterRequestDto;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final JwtProperties jwtProperties;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        String normalizedEmail = normalizeEmail(request.email());
        validatePassword(request.password());
        appUserRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
            throw new IllegalArgumentException("Email already registered");
        });

        AppUser user = AppUser.builder()
            .email(normalizedEmail)
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(UserRole.USER)
            .build();
        appUserRepository.save(user);

        return issueToken(user.getEmail());
    }

    public AuthResponseDto login(LoginRequestDto request) {
        String normalizedEmail = normalizeEmail(request.email());
        validatePassword(request.password());
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return buildResponse(userDetails);
    }

    private AuthResponseDto issueToken(String email) {
        AppUser saved = appUserRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalStateException("User not persisted"));
        UserDetails userDetails = org.springframework.security.core.userdetails.User
            .withUsername(saved.getEmail())
            .password(saved.getPasswordHash())
            .authorities("ROLE_" + saved.getRole().name())
            .build();
        return buildResponse(userDetails);
    }

    private AuthResponseDto buildResponse(UserDetails userDetails) {
        String token = jwtService.generateToken(userDetails);
        Instant expiresAt = Instant.now().plus(jwtProperties.expiration());
        return new AuthResponseDto(token, expiresAt);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
    }
}
