package com.diegoramos.mylifediary.modules.journal.repository;

import com.diegoramos.mylifediary.modules.journal.domain.entity.Journal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalRepository extends JpaRepository<Journal, UUID> {
	Optional<Journal> findByIdAndUserId(UUID journalId, UUID userId);
}

