package com.valuebet.backend.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {

    Optional<Authentication> parseToken(String token);

    String generateToken(UserDetails userDetails);
}
