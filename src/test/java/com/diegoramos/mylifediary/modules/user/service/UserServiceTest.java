package com.diegoramos.mylifediary.modules.user.service;

import com.diegoramos.mylifediary.common.exception.DomainException;
import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import com.diegoramos.mylifediary.modules.user.domain.enums.UserStatus;
import com.diegoramos.mylifediary.modules.user.dto.request.CreateUserRequest;
import com.diegoramos.mylifediary.modules.user.dto.request.UpdateEmailRequest;
import com.diegoramos.mylifediary.modules.user.dto.request.UpdatePasswordRequest;
import com.diegoramos.mylifediary.modules.user.dto.request.UpdateUserInfoRequest;
import com.diegoramos.mylifediary.modules.user.dto.response.UserResponseDTO;
import com.diegoramos.mylifediary.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Clock clock;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private final Instant now = Instant.parse("2026-04-20T00:00:00Z");

    @BeforeEach
    void setupClock() {
        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(java.time.ZoneOffset.UTC);
    }

    @Test
    void findAll_withSearch_usesRepositorySearch() {
        Page<User> page = new PageImpl<>(List.of(User.create("a@b.com", "h", "A B", LocalDate.now())));
        when(userRepository.findByFullNameContainingIgnoreCase(eq("term"), any())).thenReturn(page);

        Page<UserResponseDTO> result = userService.findAll("term", 0, 10);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).findByFullNameContainingIgnoreCase(eq("term"), any());
    }

    @Test
    void findAll_withoutSearch_callsFindAll() {
        Page<User> page = new PageImpl<>(List.of(User.create("a@b.com", "h", "A B", LocalDate.now())));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<UserResponseDTO> result = userService.findAll(null, 0, 10);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(any(Pageable.class));
    }

    @Test
    void findUsersByStatus_withoutSearch_callsFindAll() {
        Page<User> page = new PageImpl<>(List.of(User.create("x@y.com", "h", "X Y", LocalDate.now())));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<UserResponseDTO> result = userService.findUsersByStatus(null, 0, 10);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(any(Pageable.class));
    }

    @Test
    void findUsersByStatus_withSearch_callsRepository() {
        Page<User> page = new PageImpl<>(List.of(User.create("x@y.com", "h", "X Y", LocalDate.now())));
        when(userRepository.findByFullNameContainingIgnoreCase(eq("x"), any())).thenReturn(page);

        Page<UserResponseDTO> result = userService.findUsersByStatus("x", 0, 10);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).findByFullNameContainingIgnoreCase(eq("x"), any());
    }

    @Test
    void register_fastFailWhenEmailExists() {
        CreateUserRequest dto = new CreateUserRequest("Name", "a@b.com", "password123", null);
        when(userRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(true);

        Result<UserResponseDTO> result = userService.register(dto);

        assertTrue(result.isFailure());
        assertEquals("USER_EMAIL_ALREADY_EXISTS", result.getError().code());
    }

    @Test
    void register_successfulCreation() {
        CreateUserRequest dto = new CreateUserRequest("Name", "c@d.com", "password123", LocalDate.of(1990,1,1));
        when(userRepository.existsByEmailIgnoreCase("c@d.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash-1");
        User created = User.create("c@d.com", "hash-1", "Name", LocalDate.of(1990,1,1));
        when(userRepository.save(any())).thenReturn(created);

        Result<UserResponseDTO> result = userService.register(dto);

        assertTrue(result.isSuccess());
        assertEquals("c@d.com", result.getValue().email());
        verify(userRepository).save(userCaptor.capture());
        assertEquals("hash-1", /* saved password hash */ readPasswordHash(userCaptor.getValue()));
    }

    // Helper to access private passwordHash for assertion
    private static String readPasswordHash(User user) {
        try {
            java.lang.reflect.Field f = User.class.getDeclaredField("passwordHash");
            f.setAccessible(true);
            return (String) f.get(user);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void register_domainExceptionReturnedAsFailure() {
        CreateUserRequest dto = new CreateUserRequest("Name", " ", "password123", null);
        when(userRepository.existsByEmailIgnoreCase(" ")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash");

        Result<UserResponseDTO> result = userService.register(dto);

        assertTrue(result.isFailure());
        assertEquals("USER_INVALID_INPUT", result.getError().code());
    }

    @Test
    void register_dataIntegrityViolationReturnsEmailExists() {
        CreateUserRequest dto = new CreateUserRequest("Name", "e@f.com", "password123", null);
        when(userRepository.existsByEmailIgnoreCase("e@f.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        when(userRepository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

        Result<UserResponseDTO> result = userService.register(dto);

        assertTrue(result.isFailure());
        assertEquals("USER_EMAIL_ALREADY_EXISTS", result.getError().code());
    }

    @Test
    void changeEmail_userNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        Result<UserResponseDTO> result = userService.changeEmail(id, new UpdateEmailRequest("new@x.com"));

        assertTrue(result.isFailure());
        assertEquals("USER_NOT_FOUND", result.getError().code());
    }

    @Test
    void changeEmail_invalidEmail() {
        UUID id = UUID.randomUUID();
        User user = User.create("a@b.com", "h", "A B", LocalDate.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        Result<UserResponseDTO> result = userService.changeEmail(id, new UpdateEmailRequest(null));

        assertTrue(result.isFailure());
        assertEquals("USER_INVALID_EMAIL", result.getError().code());
        assertEquals("a@b.com", user.getEmail());
    }

    @Test
    void changeEmail_sameEmail() {
        UUID id = UUID.randomUUID();
        User user = User.create("same@ex.com", "h", "Same", LocalDate.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        Result<UserResponseDTO> result = userService.changeEmail(id, new UpdateEmailRequest("same@ex.com"));

        assertTrue(result.isFailure());
        assertEquals("USER_EMAIL_SAME", result.getError().code());
    }

    @Test
    void changeEmail_emailAlreadyExists() {
        UUID id = UUID.randomUUID();
        User user = User.create("old@ex.com", "h", "Old", LocalDate.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("taken@ex.com")).thenReturn(true);

        Result<UserResponseDTO> result = userService.changeEmail(id, new UpdateEmailRequest("taken@ex.com"));

        assertTrue(result.isFailure());
        assertEquals("USER_EMAIL_ALREADY_EXISTS", result.getError().code());
    }

    @Test
    void changeEmail_successful() {
        UUID id = UUID.randomUUID();
        User user = User.create("old@ex.com", "h", "Old", LocalDate.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("new@ex.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<UserResponseDTO> result = userService.changeEmail(id, new UpdateEmailRequest("  NEW@EX.com  "));

        assertTrue(result.isSuccess());
        assertEquals("new@ex.com", result.getValue().email());
    }

    @Test
    void changePassword_userNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        Result<UserResponseDTO> result = userService.changePassword(id, new UpdatePasswordRequest("12345678"));

        assertTrue(result.isFailure());
        assertEquals("USER_NOT_FOUND", result.getError().code());
    }

    @Test
    void changePassword_domainError() {
        UUID id = UUID.randomUUID();
        User user = User.create("p@q.com", "h", "P Q", LocalDate.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("badpass")).thenReturn(""); // will cause domain exception

        Result<UserResponseDTO> result = userService.changePassword(id, new UpdatePasswordRequest("badpass"));

        assertTrue(result.isFailure());
        assertEquals("USER_UPDATE_FAILED", result.getError().code());
    }

    @Test
    void changePassword_success() {
        UUID id = UUID.randomUUID();
        User user = User.create("p@q.com", "h", "P Q", LocalDate.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("goodpass")).thenReturn("new-hash");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<UserResponseDTO> result = userService.changePassword(id, new UpdatePasswordRequest("goodpass"));

        assertTrue(result.isSuccess());
        assertEquals("p@q.com", result.getValue().email());
    }

    @Test
    void changeProfileInfo_noUpdates() {
        UUID id = UUID.randomUUID();
        Result<UserResponseDTO> result = userService.changeProfileInfo(id, new UpdateUserInfoRequest(null, null));
        assertTrue(result.isFailure());
        assertEquals("USER_INFO_EMPTY_UPDATE", result.getError().code());
    }

    @Test
    void changeProfileInfo_invalidFullName() {
        UUID id = UUID.randomUUID();
        Result<UserResponseDTO> result = userService.changeProfileInfo(id, new UpdateUserInfoRequest(" ", LocalDate.now()));
        assertTrue(result.isFailure());
        assertEquals("USER_INVALID_FULL_NAME", result.getError().code());
    }

    @Test
    void changeProfileInfo_userNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        Result<UserResponseDTO> result = userService.changeProfileInfo(id, new UpdateUserInfoRequest("Maria", LocalDate.of(1995,5,10)));

        assertTrue(result.isFailure());
        assertEquals("USER_NOT_FOUND", result.getError().code());
    }

    @Test
    void changeProfileInfo_success() {
        UUID id = UUID.randomUUID();
        User user = User.create("z@x.com", "h", "Z X", LocalDate.of(1990,1,1));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<UserResponseDTO> result = userService.changeProfileInfo(id, new UpdateUserInfoRequest("  Maria Silva  ", LocalDate.of(1995,5,10)));

        assertTrue(result.isSuccess());
        assertEquals("Maria Silva", result.getValue().fullName());
    }

    @Test
    void deleteUser_activeRequestsDeletion() {
        UUID id = UUID.randomUUID();
        User user = User.create("a@b.com", "h", "A B", LocalDate.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<UserResponseDTO> result = userService.deleteUser(id);

        assertTrue(result.isSuccess());
        assertEquals(UserStatus.PENDING_DELETION, user.getStatus());
    }

    @Test
    void deleteUser_pendingDeletionReturnsFailure() {
        UUID id = UUID.randomUUID();
        User user = User.create("a@b.com", "h", "A B", LocalDate.now());
        user.requestDeletion(now);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        Result<UserResponseDTO> result = userService.deleteUser(id);

        assertTrue(result.isFailure());
        assertEquals("DELETION_ALREADY_REQUESTED", result.getError().code());
    }

    @Test
    void deleteUser_userNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        Result<UserResponseDTO> result = userService.deleteUser(id);

        assertTrue(result.isFailure());
        assertEquals("USER_NOT_FOUND", result.getError().code());
    }

    @Test
    void restoreUser_pendingDeletionRestores() {
        UUID id = UUID.randomUUID();
        User user = User.create("a@b.com", "h", "A B", LocalDate.now());
        user.requestDeletion(now);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<UserResponseDTO> result = userService.restoreUser(id);

        assertTrue(result.isSuccess());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void restoreUser_userNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        Result<UserResponseDTO> result = userService.restoreUser(id);

        assertTrue(result.isFailure());
        assertEquals("USER_NOT_FOUND", result.getError().code());
    }
}

