package com.valuebet.backend.domain.repository;

import com.valuebet.backend.domain.model.ValueOpportunity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ValueOpportunityRepository extends JpaRepository<ValueOpportunity, Long>,
    JpaSpecificationExecutor<ValueOpportunity> {

    @Override
    @EntityGraph(attributePaths = {"event", "bookmaker"})
    Page<ValueOpportunity> findAll(Specification<ValueOpportunity> spec, Pageable pageable);
}
