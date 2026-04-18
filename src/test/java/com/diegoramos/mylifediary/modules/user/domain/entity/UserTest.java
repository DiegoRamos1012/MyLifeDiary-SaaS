package com.diegoramos.mylifediary.modules.user.domain.entity;

import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.modules.user.domain.enums.UserStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void createShouldReturnSuccessWithNormalizedValues() {
        Result<User> result = User.create("  Diego@Example.com ", "hash-123", "  Diego Ramos  ", LocalDate.of(1990, 1, 2));

        assertTrue(result.isSuccess());
        User user = result.getValue();
        assertEquals("diego@example.com", user.getEmail());
        assertEquals("Diego Ramos", user.getFullName());
        assertEquals(LocalDate.of(1990, 1, 2), user.getBirthdayDate());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals("hash-123", readPasswordHash(user));
    }

    @Test
    void createShouldFailWhenEmailIsInvalid() {
        Result<User> result = User.create(" ", "hash-123", "Diego Ramos", LocalDate.of(1990, 1, 2));

        assertTrue(result.isFailure());
        assertEquals("INVALID_EMAIL", result.getError().code());
        assertEquals("Email inválido", result.getError().message());
    }

    @Test
    void createShouldFailWhenPasswordIsInvalid() {
        Result<User> result = User.create("diego@example.com", "", "Diego Ramos", LocalDate.of(1990, 1, 2));

        assertTrue(result.isFailure());
        assertEquals("INVALID_PASSWORD", result.getError().code());
        assertEquals("Senha inválida", result.getError().message());
    }

    @Test
    void createShouldFailWhenFullNameIsInvalid() {
        Result<User> result = User.create("diego@example.com", "hash-123", null, LocalDate.of(1990, 1, 2));

        assertTrue(result.isFailure());
        assertEquals("INVALID_FULL_NAME", result.getError().code());
        assertEquals("Nome inválido", result.getError().message());
    }

    @Test
    void updateEmailShouldReturnSuccessAndNormalizeValue() {
        User user = createValidUser();

        Result<User> result = user.updateEmail("  NEW@Example.com  ");

        assertTrue(result.isSuccess());
        assertSame(user, result.getValue());
        assertEquals("new@example.com", user.getEmail());
    }

    @Test
    void updateEmailShouldFailWithoutChangingCurrentValue() {
        User user = createValidUser();

        Result<User> result = user.updateEmail(" ");

        assertTrue(result.isFailure());
        assertEquals("INVALID_EMAIL", result.getError().code());
        assertEquals("diego@example.com", user.getEmail());
    }

    @Test
    void updatePasswordShouldReturnSuccessAndChangePasswordHash() {
        User user = createValidUser();

        Result<User> result = user.updatePassword("new-hash");

        assertTrue(result.isSuccess());
        assertSame(user, result.getValue());
        assertEquals("new-hash", readPasswordHash(user));
    }

    @Test
    void updatePasswordShouldFailWithoutChangingCurrentValue() {
        User user = createValidUser();
        String previousPassword = readPasswordHash(user);

        Result<User> result = user.updatePassword(null);

        assertTrue(result.isFailure());
        assertEquals("INVALID_PASSWORD", result.getError().code());
        assertEquals(previousPassword, readPasswordHash(user));
    }

    @Test
    void updateProfileShouldReturnSuccessAndChangeProfile() {
        User user = createValidUser();

        Result<User> result = user.updateProfile("  Maria Silva  ", LocalDate.of(1995, 5, 10));

        assertTrue(result.isSuccess());
        assertSame(user, result.getValue());
        assertEquals("Maria Silva", user.getFullName());
        assertEquals(LocalDate.of(1995, 5, 10), user.getBirthdayDate());
    }

    @Test
    void updateProfileShouldFailWithoutChangingCurrentValue() {
        User user = createValidUser();

        Result<User> result = user.updateProfile("", LocalDate.of(1995, 5, 10));

        assertTrue(result.isFailure());
        assertEquals("INVALID_FULL_NAME", result.getError().code());
        assertEquals("Diego Ramos", user.getFullName());
        assertEquals(LocalDate.of(1990, 1, 2), user.getBirthdayDate());
    }

    private static User createValidUser() {
        Result<User> result = User.create("diego@example.com", "hash-123", "Diego Ramos", LocalDate.of(1990, 1, 2));
        assertTrue(result.isSuccess());
        return result.getValue();
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
}
