package com.diegoramos.mylifediary.modules.journal.repository;

import com.diegoramos.mylifediary.modules.journal.domain.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    /**
     * Busca as entradas de um diário ordenadas
     * da mais recente para a mais antiga.
     *
     * @param journalId ID do diário
     * @param pageable  configuração de paginação
     * @return página contendo as entradas do diário
     */
    Page<JournalEntry> findByJournalIdOrderByEntryDateDesc(
            UUID journalId,
            Pageable pageable
    );

    /**
     * Busca uma entrada específica pertencente
     * a um determinado diário.
     * <p>
     * Essa validação ajuda a impedir acesso
     * a entradas que não pertencem ao diário informado.
     *
     * @param entryId   ID da entrada
     * @param journalId ID do diário
     * @return entrada encontrada ou vazio
     */
    Optional<JournalEntry> findByIdAndJournalId(
            UUID entryId,
            UUID journalId
    );

    /**
     * Busca uma entrada específica por diário e data.
     *
     * @param journalId ID do diário
     * @param entryDate data da entrada
     * @return entrada encontrada ou vazio
     */
    Optional<JournalEntry> findByJournalIdAndEntryDate(
            UUID journalId,
            LocalDate entryDate
    );

    /**
     * Verifica se uma entrada pertence
     * a um determinado diário.
     *
     * @param entryId   ID da entrada
     * @param journalId ID do diário
     * @return true caso exista
     */
    boolean existsByIdAndJournalId(
            UUID entryId,
            UUID journalId
    );

    /**
     * Verifica se uma entrada pertence
     * a um determinado diário e data.
     *
     * @param journalId ID do diário
     * @param entryDate data da entrada
     * @return true caso exista
     */
    boolean existsByJournalIdAndEntryDate(
            UUID journalId,
            LocalDate entryDate
    );

    /**
     * Retorna a quantidade total de entradas
     * cadastradas num diário.
     *
     * @param journalId ID do diário
     * @return quantidade total de entradas
     */
    long countByJournalId(UUID journalId);
}
