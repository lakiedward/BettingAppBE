package com.valuebet.backend.domain.repository;

import com.valuebet.backend.domain.model.Bookmaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmakerRepository extends JpaRepository<Bookmaker, Long> {

    Optional<Bookmaker> findByExternalKey(String externalKey);
}
