package com.diegoramos.mylifediary.modules.journal.service;

import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.modules.journal.domain.entity.Journal;
import com.diegoramos.mylifediary.modules.journal.domain.entity.JournalEntry;
import com.diegoramos.mylifediary.modules.journal.domain.enums.MoodTypes;
import com.diegoramos.mylifediary.modules.journal.dto.request.CreateJournalEntryRequest;
import com.diegoramos.mylifediary.modules.journal.dto.request.CreateJournalRequest;
import com.diegoramos.mylifediary.modules.journal.dto.request.LockJournalRequest;
import com.diegoramos.mylifediary.modules.journal.dto.request.UnlockJournalRequest;
import com.diegoramos.mylifediary.modules.journal.dto.response.JournalEntryResponseDTO;
import com.diegoramos.mylifediary.modules.journal.dto.response.JournalResponseDTO;
import com.diegoramos.mylifediary.modules.journal.repository.JournalEntryRepository;
import com.diegoramos.mylifediary.modules.journal.repository.JournalRepository;
import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import com.diegoramos.mylifediary.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    @Mock
    private JournalRepository journalRepository;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private JournalService journalService;

    private static void setBaseId(Object entity, UUID id) {
        try {
            Field field = entity.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void createJournal_whenLockedWithoutPassword_returnsFailure() {
        UUID userId = UUID.randomUUID();
        User user = User.create("john@example.com", "hash", "John", LocalDate.of(1990, 1, 1));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Result<JournalResponseDTO> result = journalService.createJournal(
                userId,
                new CreateJournalRequest("Meu Diário", true, null)
        );

        assertTrue(result.isFailure());
        assertEquals("JOURNAL_PASSWORD_REQUIRED", result.getError().code());
    }

    @Test
    void lockJournal_success_hashesPasswordAndLocksJournal() {
        UUID userId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        User user = User.create("john@example.com", "hash", "John", LocalDate.of(1990, 1, 1));
        Journal journal = Journal.create(user, "Meu Diário", false);
        setBaseId(journal, journalId);
        setBaseId(user, userId);

        when(journalRepository.findByIdAndUserId(journalId, userId)).thenReturn(Optional.of(journal));
        when(passwordEncoder.encode("segredo-forte")).thenReturn("argon-hash");
        when(journalRepository.save(any(Journal.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<JournalResponseDTO> result = journalService.lockJournal(
                userId,
                journalId,
                new LockJournalRequest("segredo-forte")
        );

        assertTrue(result.isSuccess());
        assertTrue(journal.isLocked());
        assertEquals("argon-hash", journal.getPasswordHash());
        verify(journalRepository).save(journal);
    }

    @Test
    void unlockJournal_withWrongPassword_returnsFailure() {
        UUID userId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        User user = User.create("john@example.com", "hash", "John", LocalDate.of(1990, 1, 1));
        Journal journal = Journal.create(user, "Meu Diário", false);
        setBaseId(user, userId);
        setBaseId(journal, journalId);
        journal.lockWithPasswordHash("argon-hash");

        when(journalRepository.findByIdAndUserId(journalId, userId)).thenReturn(Optional.of(journal));
        when(passwordEncoder.matches("senha-errada", "argon-hash")).thenReturn(false);

        Result<JournalResponseDTO> result = journalService.unlockJournal(
                userId,
                journalId,
                new UnlockJournalRequest("senha-errada")
        );

        assertTrue(result.isFailure());
        assertEquals("JOURNAL_INVALID_PASSWORD", result.getError().code());
        assertTrue(journal.isLocked());
    }

    @Test
    void unlockJournal_withCorrectPassword_unlocksAndClearsHash() {
        UUID userId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        User user = User.create("john@example.com", "hash", "John", LocalDate.of(1990, 1, 1));
        Journal journal = Journal.create(user, "Meu Diário", false);
        setBaseId(user, userId);
        setBaseId(journal, journalId);
        journal.lockWithPasswordHash("argon-hash");

        when(journalRepository.findByIdAndUserId(journalId, userId)).thenReturn(Optional.of(journal));
        when(passwordEncoder.matches("senha-correta", "argon-hash")).thenReturn(true);
        when(journalRepository.save(any(Journal.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<JournalResponseDTO> result = journalService.unlockJournal(
                userId,
                journalId,
                new UnlockJournalRequest("senha-correta")
        );

        assertTrue(result.isSuccess());
        assertFalse(journal.isLocked());
        assertNull(journal.getPasswordHash());
    }

    @Test
    void createEntry_whenJournalLocked_returnsFailure() {
        UUID userId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        User user = User.create("john@example.com", "hash", "John", LocalDate.of(1990, 1, 1));
        Journal journal = Journal.create(user, "Meu Diário", false);
        setBaseId(user, userId);
        setBaseId(journal, journalId);
        journal.lockWithPasswordHash("argon-hash");

        when(journalRepository.findByIdAndUserId(journalId, userId)).thenReturn(Optional.of(journal));

        Result<JournalEntryResponseDTO> result = journalService.createEntry(
                userId,
                journalId,
                new CreateJournalEntryRequest(LocalDate.of(2026, 5, 26), "Hoje foi bom", MoodTypes.HAPPY)
        );

        assertTrue(result.isFailure());
        assertEquals("JOURNAL_LOCKED", result.getError().code());
    }

    @Test
    void createEntry_whenDateAlreadyExists_returnsFailure() {
        UUID userId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        User user = User.create("john@example.com", "hash", "John", LocalDate.of(1990, 1, 1));
        Journal journal = Journal.create(user, "Meu Diário", false);
        setBaseId(user, userId);
        setBaseId(journal, journalId);

        LocalDate entryDate = LocalDate.of(2026, 5, 26);

        when(journalRepository.findByIdAndUserId(journalId, userId)).thenReturn(Optional.of(journal));
        when(journalEntryRepository.existsByJournalIdAndEntryDate(journalId, entryDate)).thenReturn(true);

        Result<JournalEntryResponseDTO> result = journalService.createEntry(
                userId,
                journalId,
                new CreateJournalEntryRequest(entryDate, "Hoje foi bom", MoodTypes.HAPPY)
        );

        assertTrue(result.isFailure());
        assertEquals("JOURNAL_ENTRY_ALREADY_EXISTS_FOR_DATE", result.getError().code());
    }

    @Test
    void createEntry_success_returnsSavedEntry() {
        UUID userId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        User user = User.create("john@example.com", "hash", "John", LocalDate.of(1990, 1, 1));
        Journal journal = Journal.create(user, "Meu Diário", false);
        setBaseId(user, userId);
        setBaseId(journal, journalId);

        LocalDate entryDate = LocalDate.of(2026, 5, 26);

        when(journalRepository.findByIdAndUserId(journalId, userId)).thenReturn(Optional.of(journal));
        when(journalEntryRepository.existsByJournalIdAndEntryDate(journalId, entryDate)).thenReturn(false);
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            setBaseId(entry, UUID.randomUUID());
            return entry;
        });

        Result<JournalEntryResponseDTO> result = journalService.createEntry(
                userId,
                journalId,
                new CreateJournalEntryRequest(entryDate, "Hoje foi bom", MoodTypes.HAPPY)
        );

        assertTrue(result.isSuccess());
        assertEquals(entryDate, result.getValue().entryDate());
        assertEquals(MoodTypes.HAPPY, result.getValue().mood());
    }
}

