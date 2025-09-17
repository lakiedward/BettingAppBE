package com.valuebet.backend.security;

import com.valuebet.backend.config.JwtProperties;
import com.valuebet.backend.domain.model.AppUser;
import com.valuebet.backend.domain.model.UserRole;
import com.valuebet.backend.domain.repository.AppUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtTokenService implements JwtService {

    private static final String ROLE_CLAIM = "role";

    private final JwtProperties properties;
    private final AppUserRepository appUserRepository;

    @Override
    public Optional<Authentication> parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

            String email = claims.getSubject();
            String role = claims.get(ROLE_CLAIM, String.class);
            if (email == null || role == null) {
                return Optional.empty();
            }

            AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for token"));

            Collection<? extends GrantedAuthority> authorities = Collections.singleton(
                new SimpleGrantedAuthority("ROLE_" + role)
            );

            UserDetails userDetails = User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .build();

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                authorities
            );
            return Optional.of(authentication);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    @Override
    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.expiration());
        String role = userDetails.getAuthorities().stream()
            .findFirst()
            .map(GrantedAuthority::getAuthority)
            .map(auth -> auth.replace("ROLE_", ""))
            .orElse(UserRole.USER.name());

        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .claim(ROLE_CLAIM, role)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiry))
            .signWith(signingKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    private Key signingKey() {
        byte[] keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
