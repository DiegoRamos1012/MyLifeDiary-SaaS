package com.diegoramos.mylifediary.modules.journal.service;

import com.diegoramos.mylifediary.common.exception.DomainException;
import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.modules.journal.domain.entity.Journal;
import com.diegoramos.mylifediary.modules.journal.domain.entity.JournalEntry;
import com.diegoramos.mylifediary.modules.journal.dto.request.CreateJournalEntryRequest;
import com.diegoramos.mylifediary.modules.journal.dto.request.CreateJournalRequest;
import com.diegoramos.mylifediary.modules.journal.dto.request.LockJournalRequest;
import com.diegoramos.mylifediary.modules.journal.dto.request.UnlockJournalRequest;
import com.diegoramos.mylifediary.modules.journal.dto.request.UpdateJournalEntryRequest;
import com.diegoramos.mylifediary.modules.journal.dto.response.JournalEntryResponseDTO;
import com.diegoramos.mylifediary.modules.journal.dto.response.JournalResponseDTO;
import com.diegoramos.mylifediary.modules.journal.repository.JournalEntryRepository;
import com.diegoramos.mylifediary.modules.journal.repository.JournalRepository;
import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import com.diegoramos.mylifediary.modules.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de aplicação do módulo de diário.
 *
 * <p>Orquestra validações de acesso, regras de bloqueio por senha e operações
 * de notas diárias. Erros esperados de negócio são retornados via
 * {@link Result#failure(String, String)}.</p>
 */
@Service
@Transactional
public class JournalService {
    private final JournalRepository journalRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public JournalService(JournalRepository journalRepository,
                          JournalEntryRepository journalEntryRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.journalRepository = journalRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cria um diário para o usuário informado.
     *
     * <p>Quando {@code dto.isLocked()} for verdadeiro, exige senha e já salva
     * o diário trancado com hash Argon gerado pelo {@link PasswordEncoder}.</p>
     *
     * @param userId id do usuário dono do diário
     * @param dto    dados de criação do diário
     * @return resultado com o diário criado ou falha de negócio
     */
    public Result<JournalResponseDTO> createJournal(UUID userId, CreateJournalRequest dto) {
        Optional<User> maybeUser = userRepository.findById(userId);
        if (maybeUser.isEmpty()) {
            return Result.failure("JOURNAL_USER_NOT_FOUND", "Usuário não encontrado");
        }

        if (dto.isLocked() && (dto.password() == null || dto.password().isBlank())) {
            return Result.failure("JOURNAL_PASSWORD_REQUIRED", "Informe uma senha para trancar o diário");
        }

        try {
            Journal journal = Journal.create(maybeUser.get(), dto.title(), false);
            if (dto.isLocked()) {
                journal.lockWithPasswordHash(passwordEncoder.encode(dto.password()));
            }
            Journal saved = journalRepository.save(journal);
            return Result.success(JournalResponseDTO.from(saved));
        } catch (DomainException ex) {
            return Result.failure("JOURNAL_INVALID_INPUT", ex.getMessage());
        }
    }

    /**
     * Tranca um diário com senha.
     *
     * @param userId    id do usuário dono do diário
     * @param journalId id do diário
     * @param dto       senha em texto puro para gerar hash
     * @return resultado com o diário trancado ou falha de negócio
     */
    public Result<JournalResponseDTO> lockJournal(UUID userId, UUID journalId, LockJournalRequest dto) {
        Optional<Journal> maybeJournal = journalRepository.findByIdAndUserId(journalId, userId);
        if (maybeJournal.isEmpty()) {
            return Result.failure("JOURNAL_NOT_FOUND", "Diário não encontrado");
        }

        Journal journal = maybeJournal.get();
        if (journal.isLocked()) {
            return Result.failure("JOURNAL_ALREADY_LOCKED", "O diário já está trancado");
        }

        try {
            journal.lockWithPasswordHash(passwordEncoder.encode(dto.password()));
            Journal saved = journalRepository.save(journal);
            return Result.success(JournalResponseDTO.from(saved));
        } catch (DomainException ex) {
            return Result.failure("JOURNAL_INVALID_INPUT", ex.getMessage());
        }
    }

    /**
     * Destranca um diário validando a senha atual.
     *
     * @param userId    id do usuário dono do diário
     * @param journalId id do diário
     * @param dto       senha em texto puro para validação do hash
     * @return resultado com o diário destrancado ou falha de negócio
     */
    public Result<JournalResponseDTO> unlockJournal(UUID userId, UUID journalId, UnlockJournalRequest dto) {
        Optional<Journal> maybeJournal = journalRepository.findByIdAndUserId(journalId, userId);
        if (maybeJournal.isEmpty()) {
            return Result.failure("JOURNAL_NOT_FOUND", "Diário não encontrado");
        }

        Journal journal = maybeJournal.get();
        if (!journal.isLocked()) {
            return Result.failure("JOURNAL_NOT_LOCKED", "O diário já está destrancado");
        }

        String storedHash = journal.getPasswordHash();
        if (storedHash == null || !passwordEncoder.matches(dto.password(), storedHash)) {
            return Result.failure("JOURNAL_INVALID_PASSWORD", "Senha inválida");
        }

        try {
            journal.unlock();
            Journal saved = journalRepository.save(journal);
            return Result.success(JournalResponseDTO.from(saved));
        } catch (DomainException ex) {
            return Result.failure("JOURNAL_INVALID_INPUT", ex.getMessage());
        }
    }

    /**
     * Cria uma nota diária no diário informado.
     *
     * @param userId    id do usuário dono do diário
     * @param journalId id do diário
     * @param dto       dados da nota diária
     * @return resultado com a nota criada ou falha de negócio
     */
    public Result<JournalEntryResponseDTO> createEntry(UUID userId, UUID journalId, CreateJournalEntryRequest dto) {
        Result<Journal> journalResult = getUnlockedJournal(userId, journalId);
        if (journalResult.isFailure()) {
            return Result.failure(journalResult.getError());
        }

        if (journalEntryRepository.existsByJournalIdAndEntryDate(journalId, dto.entryDate())) {
            return Result.failure("JOURNAL_ENTRY_ALREADY_EXISTS_FOR_DATE", "Já existe uma nota para essa data");
        }

        try {
            JournalEntry entry = JournalEntry.create(journalResult.getValue(), dto.entryDate(), dto.content(), dto.mood());
            JournalEntry saved = journalEntryRepository.save(entry);
            return Result.success(JournalEntryResponseDTO.from(saved));
        } catch (DomainException ex) {
            return Result.failure("JOURNAL_ENTRY_INVALID_INPUT", ex.getMessage());
        }
    }

    /**
     * Atualiza conteúdo e humor de uma nota existente.
     *
     * @param userId    id do usuário dono do diário
     * @param journalId id do diário
     * @param entryId   id da nota
     * @param dto       novos dados da nota
     * @return resultado com a nota atualizada ou falha de negócio
     */
    public Result<JournalEntryResponseDTO> updateEntry(UUID userId,
                                                       UUID journalId,
                                                       UUID entryId,
                                                       UpdateJournalEntryRequest dto) {
        Result<Journal> journalResult = getUnlockedJournal(userId, journalId);
        if (journalResult.isFailure()) {
            return Result.failure(journalResult.getError());
        }

        Optional<JournalEntry> maybeEntry = journalEntryRepository.findByIdAndJournalId(entryId, journalId);
        if (maybeEntry.isEmpty()) {
            return Result.failure("JOURNAL_ENTRY_NOT_FOUND", "Nota do diário não encontrada");
        }

        try {
            JournalEntry entry = maybeEntry.get();
            entry.update(dto.content(), dto.mood());
            JournalEntry saved = journalEntryRepository.save(entry);
            return Result.success(JournalEntryResponseDTO.from(saved));
        } catch (DomainException ex) {
            return Result.failure("JOURNAL_ENTRY_INVALID_INPUT", ex.getMessage());
        }
    }

    /**
     * Exclui uma nota de diário.
     *
     * @param userId    id do usuário dono do diário
     * @param journalId id do diário
     * @param entryId   id da nota
     * @return sucesso vazio ou falha de negócio
     */
    public Result<Void> deleteEntry(UUID userId, UUID journalId, UUID entryId) {
        Result<Journal> journalResult = getUnlockedJournal(userId, journalId);
        if (journalResult.isFailure()) {
            return Result.failure(journalResult.getError());
        }

        Optional<JournalEntry> maybeEntry = journalEntryRepository.findByIdAndJournalId(entryId, journalId);
        if (maybeEntry.isEmpty()) {
            return Result.failure("JOURNAL_ENTRY_NOT_FOUND", "Nota do diário não encontrada");
        }

        journalEntryRepository.delete(maybeEntry.get());
        return Result.success(null);
    }

    /**
     * Busca uma nota específica por id.
     *
     * @param userId    id do usuário dono do diário
     * @param journalId id do diário
     * @param entryId   id da nota
     * @return nota encontrada ou falha de negócio
     */
    @Transactional(readOnly = true)
    public Result<JournalEntryResponseDTO> findEntry(UUID userId, UUID journalId, UUID entryId) {
        Optional<Journal> maybeJournal = journalRepository.findByIdAndUserId(journalId, userId);
        if (maybeJournal.isEmpty()) {
            return Result.failure("JOURNAL_NOT_FOUND", "Diário não encontrado");
        }

        Optional<JournalEntry> maybeEntry = journalEntryRepository.findByIdAndJournalId(entryId, journalId);
        return maybeEntry.map(journalEntry -> Result.success(JournalEntryResponseDTO.from(journalEntry)))
                .orElseGet(() -> Result.failure("JOURNAL_ENTRY_NOT_FOUND", "Nota do diário não encontrada"));

    }

    /**
     * Lista notas de um diário de forma paginada (mais recentes primeiro).
     *
     * @param userId    id do usuário dono do diário
     * @param journalId id do diário
     * @param page      número da página (base 0)
     * @param size      tamanho da página
     * @return página de notas ou falha de negócio
     */
    @Transactional(readOnly = true)
    public Result<Page<JournalEntryResponseDTO>> listEntries(UUID userId, UUID journalId, int page, int size) {
        Optional<Journal> maybeJournal = journalRepository.findByIdAndUserId(journalId, userId);
        if (maybeJournal.isEmpty()) {
            return Result.failure("JOURNAL_NOT_FOUND", "Diário não encontrado");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<JournalEntryResponseDTO> entries = journalEntryRepository
                .findByJournalIdOrderByEntryDateDesc(journalId, pageable)
                .map(JournalEntryResponseDTO::from);

        return Result.success(entries);
    }

    /**
     * Valida existência e estado de desbloqueio de um diário para operações de escrita.
     */
    private Result<Journal> getUnlockedJournal(UUID userId, UUID journalId) {
        Optional<Journal> maybeJournal = journalRepository.findByIdAndUserId(journalId, userId);
        if (maybeJournal.isEmpty()) {
            return Result.failure("JOURNAL_NOT_FOUND", "Diário não encontrado");
        }

        Journal journal = maybeJournal.get();
        if (journal.isLocked()) {
            return Result.failure("JOURNAL_LOCKED", "O diário está trancado");
        }

        return Result.success(journal);
    }
}
