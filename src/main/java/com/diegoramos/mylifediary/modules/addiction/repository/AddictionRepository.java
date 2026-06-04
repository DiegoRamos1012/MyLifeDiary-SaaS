package com.diegoramos.mylifediary.modules.addiction.repository;

import com.diegoramos.mylifediary.modules.addiction.domain.entity.Addiction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA de {@link Addiction}.
 */
@Repository
public interface AddictionRepository extends JpaRepository<Addiction, UUID> {
    Optional<Addiction> findByIdAndUserId(UUID addictionId, UUID userId);
    Page<Addiction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    boolean existsByIdAndUserId(UUID addictionId, UUID userId);
}


