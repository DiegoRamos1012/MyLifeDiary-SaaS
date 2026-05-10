package com.diegoramos.mylifediary.modules.user.domain.entity;

import com.diegoramos.mylifediary.common.exception.DomainException;
import com.diegoramos.mylifediary.modules.user.domain.enums.UserStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    private static User createValidUser() {
        return User.create("diego@example.com", "hash-123", "Diego Ramos", LocalDate.of(1990, 1, 2));
    }

    private static String readPasswordHash(User user) {
        try {
            Field field = User.class.getDeclaredField("passwordHash");
            field.setAccessible(true);
            return (String) field.get(user);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to read passwordHash", ex);
        }
    }

    @Test
    void createShouldReturnUserWithNormalizedValues() {
        User user = User.create("  Diego@Example.com ", "hash-123", "  Diego Ramos  ", LocalDate.of(1990, 1, 2));

        assertEquals("diego@example.com", user.getEmail());
        assertEquals("Diego Ramos", user.getFullName());
        assertEquals(LocalDate.of(1990, 1, 2), user.getBirthDate());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals("hash-123", readPasswordHash(user));
    }

    @Test
    void createShouldThrowWhenEmailIsInvalid() {
        DomainException exception = assertThrows(
                DomainException.class,
                () -> User.create(" ", "hash-123", "Diego Ramos", LocalDate.of(1990, 1, 2))
        );

        assertEquals("Erro: e-mail não pode estar vazio", exception.getMessage());
    }

    @Test
    void createShouldThrowWhenPasswordIsInvalid() {
        DomainException exception = assertThrows(
                DomainException.class,
                () -> User.create("diego@example.com", "", "Diego Ramos", LocalDate.of(1990, 1, 2))
        );

        assertEquals("Erro: senha inválida", exception.getMessage());
    }

    @Test
    void createShouldThrowWhenFullNameIsInvalid() {
        DomainException exception = assertThrows(
                DomainException.class,
                () -> User.create("diego@example.com", "hash-123", null, LocalDate.of(1990, 1, 2))
        );

        assertEquals("Erro: nome não pode estar vazio", exception.getMessage());
    }

    @Test
    void changeEmailShouldNormalizeValue() {
        User user = createValidUser();

        user.changeEmail("  NEW@Example.com  ");

        assertEquals("new@example.com", user.getEmail());
    }

    @Test
    void changeEmailShouldThrowWithoutChangingCurrentValue() {
        User user = createValidUser();

        DomainException exception = assertThrows(DomainException.class, () -> user.changeEmail(" "));

        assertEquals("Erro: E-mail inválido", exception.getMessage());
        assertEquals("diego@example.com", user.getEmail());
    }

    @Test
    void changePasswordShouldChangePasswordHash() {
        User user = createValidUser();

        user.changePassword("new-hash");

        assertEquals("new-hash", readPasswordHash(user));
    }

    @Test
    void changePasswordShouldThrowWithoutChangingCurrentValue() {
        User user = createValidUser();
        String previousPassword = readPasswordHash(user);

        DomainException exception = assertThrows(DomainException.class, () -> user.changePassword(null));

        assertEquals("Erro: senha inválida", exception.getMessage());
        assertEquals(previousPassword, readPasswordHash(user));
    }

    @Test
    void changeProfileInfoShouldChangeProfile() {
        User user = createValidUser();

        user.changeProfileInfo("  Maria Silva  ", LocalDate.of(1995, 5, 10), LocalDate.of(2026, 5, 10));

        assertEquals("Maria Silva", user.getFullName());
        assertEquals(LocalDate.of(1995, 5, 10), user.getBirthDate());
    }

    @Test
    void changeProfileInfoShouldThrowWithoutChangingCurrentValue() {
        User user = createValidUser();

        DomainException exception = assertThrows(DomainException.class, () ->
                user.changeProfileInfo("", LocalDate.of(1995, 5, 10), LocalDate.of(2026, 5, 10)));

        assertEquals("Erro: nome inválido", exception.getMessage());
        assertEquals("Diego Ramos", user.getFullName());
        assertEquals(LocalDate.of(1990, 1, 2), user.getBirthDate());
    }
}
