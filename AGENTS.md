# AGENTS.md — AI Agent Guide for MyLifeDiary

> An architecture explicitly designed to be readable by both AI agents and humans.

## High-Level Architecture

**MyLifeDiary** is a Spring Boot 4.0.5 (Java 25) SaaS backend organized into **business-context modules**, each with
clearly defined layers: controller → service → domain → repository.

### Folder Structure

```text
src/main/java/com/diegoramos/mylifediary
├── common/         # Result pattern, exceptions, response helpers, base entities
├── config/         # Security (JWT), Swagger, Time
└── modules/
    ├── user/         # Lifecycle: ACTIVE → PENDING_DELETION → INACTIVE → SUSPENDED
    ├── auth/         # Login, JWT, refresh tokens, logout/revocation
    ├── habit/        # Habits, daily logs, streaks
    ├── journal/      # Journal with password protection and daily notes
    ├── addiction/    # Module under development
    ├── payment/      # Module under development
    └── subscription/ # Module under development
```

**Critical principle:** each module is independent; new modules follow the pattern
`controller → service → domain → repository`, with their own DTOs and repositories.

---

## The "Result Pattern" — Core Philosophy

**Golden rule:** `If it's expected, use Result. If it's unexpected, throw an exception.`

### How It Works

* **Expected failures** (duplicate email, user not found, business rule violation) → return
  `Result.failure("CODE", "message")`
* **Technical/unexpected errors** (database unavailable, code bug) → throw a `DomainException` →
  `GlobalExceptionHandler` → 500

This keeps the flow clear: business failures are **values**, not exceptions.

### Practical Example

```text
// UserService returns Result<UserResponseDTO>
if (userRepository.existsByEmail(req.email())) {
    return Result.failure("USER_EMAIL_ALREADY_EXISTS", "Email already registered");
}
// create domain entity with validated values
// User.create(...)
// Controller converts Result → HTTP
// resultHttpResponseHelper.respond(result, HttpStatus.CREATED)
```

### Error Codes (Stable and Uppercase)

`USER_EMAIL_ALREADY_EXISTS`, `USER_NOT_FOUND`, `USER_SUSPENDED`, `DELETION_ALREADY_REQUESTED`, etc.

---

## Layers and Responsibilities

### Controller (Thin HTTP Bridge)

* Receives requests
* Validates structure using `@Valid` (Bean Validation)
* Calls service methods
* Converts `Result` → `ResponseEntity` via `ResultHttpResponseHelper`
* **Does not** contain business logic

### Service (Orchestration)

* Implements use cases
* Queries repositories
* Returns `Result<T>` for expected failures
* Leaves exceptions for truly exceptional scenarios
* Uses `flatMap()` to chain multiple validations

### Domain (Identity + Rules)

* Entities with intentional behaviors:
  `User#requestDeletion(Instant)`, `User#restoreAccount()`, `User#changeEmail()`, `Habit#create(...)`,
  `HabitLog#create(...)`, `HabitLog#mark(...)`
* Maintains invariants (e.g., email cannot be empty)
* Methods reflect business concepts, not just getters/setters
* Documented with explanatory Javadoc

### Repository (Persistence)

* Spring Data JPA
* Specialized JPQL queries for bulk updates used by scheduled jobs
* Example: `markPendingDeletionUsersAsInactive(...)` and `hardDeleteInactiveUsersBefore(...)` move/remove users in
  batches for lifecycle jobs

---

## Critical Flow: User Lifecycle

This is an exemplary pattern that new modules should study.

**States:** `ACTIVE` → `PENDING_DELETION` → `INACTIVE` (there is currently no `DELETED` state in the enum)

**Transitions:**

* Users can deactivate their account via `PATCH /users/{id}/deactivate` → `requestDeletion(clock.instant())` in the
  domain
* Users can restore their account while in `PENDING_DELETION` via `PATCH /users/{id}/reactivate` → `restoreAccount()`
* Jobs run automatically:

    * `UserDeletionJob`: 00:00 UTC → moves `PENDING_DELETION` → `INACTIVE` if `deletionRequestedAt < now - 30d`
    * `UserHardDeletionJob`: 00:30 UTC → permanently deletes users if `deletionRequestedAt < now - 37d`

**Why this matters:** state transitions are **not simple UPDATEs**. They represent domain rules and appear in:

1. Domain method (`User#requestDeletion()`)
2. Service rule (`UserService#deleteUser()`)
3. Scheduled job (`UserDeletionJob`)
4. Documentation (`docs/user-lifecycle-flow.md`)

---

## JWT & Security

* **Login:** `POST /auth/login` returns `{accessToken, refreshToken, expiresIn, tokenType:"Bearer"}`
* **Refresh:** `POST /auth/refresh` with a `refreshToken` issues a new `accessToken`
* **Logout:** `POST /auth/logout` revokes the persisted refresh token
* **Protected endpoints:** require `Authorization: Bearer <token>` (Spring Security + `@SecurityRequirement`)
* **Centralized configuration:** `config/security/JwtService`, `JwtProperties`, `RefreshTokenRepository`, and
  `SwaggerConfig`

---

## Development & Build

### Run Locally (Windows PowerShell)

```powershell
.\mvnw.cmd spring-boot:run
```

### Run Tests

```powershell
.\mvnw.cmd test
```

### Test Structure

* `src/test/java/` mirrors `src/main/java/` by package
* **Unit tests** (service logic, Result pattern)
* **Integration tests** (controller + service end-to-end)
* Use `@SpringBootTest` for full-stack tests
* Test database: H2 (via `application-test.yml`)

---

## Specific Code Conventions

### DTOs Separated by Intent

```text
modules/user/dto/
  ├── request/    (CreateUserRequest, UpdateEmailRequest, etc.)
  └── response/   (UserResponseDTO, etc.)
```

### Layered Validation

* **DTO:** structural validation (`@NotBlank`, `@Email`, `@Size`)
* **Service/Domain:** business validation (duplicates, state, business rules)

### Javadoc

* **Required** for: entities, enums, services, jobs, repositories with specialized queries
* **Tone:** direct, explanatory, focused on behavior and responsibility
* **Do not repeat** the method name; provide context instead

### Standardized HTTP Responses

* Success: `2xx` with a response body (or empty when appropriate)
* Expected failure: `4xx` (`409 Conflict`, `404 Not Found`, `400 Bad Request`) with `ApiErrorResponse`
* Technical error: `500` with `ApiErrorResponse` via `GlobalExceptionHandler`

---

## Implementation Patterns

### Using `Result` with `flatMap` (Multiple Validations)

```text
// return validateUser(userId)
//     .flatMap(user -> validateBusinessRule(user))
//     .flatMap(user -> performAction(user))
//     .map(user -> new ResponseDTO(user));
```

### Accessing `Result` in a Controller

```text
result.fold(
    success -> ResponseEntity.ok(success),
    error -> ResponseEntity.status(error.statusCode()).body(error)
);
```

### Scheduled Jobs (Spring Scheduling)

* Use `@Scheduled(cron = "...")` with UTC timezone
* Must remain **short-lived**: validate threshold, call repository, done
* **Do not mix** with UI/HTTP concerns
* Use JPQL bulk updates for performance

---

## Important Dependencies

* **Spring Data JPA** + **Flyway** (database migrations)
* **Spring Security** + **JWT** (authentication)
* **Springdoc OpenAPI** (Swagger/automatic documentation)
* **Lombok** (reduces boilerplate)
* **Bean Validation** (structural validation)
* **H2** (local testing) + **PostgreSQL** (production)

---

## Essential Documents

* `docs/arch/project-architecture.md` — complete design, style, and conventions
* `docs/arch/ResultPattern.md` — philosophy and examples of the Result pattern
* `docs/arch/user-lifecycle-flow.md` — detailed user lifecycle flow

Study these files before creating a new module.

---

## Red Flags & Best Practices

### ❌ Avoid

* Throwing exceptions for common business-rule violations (use `Result`)
* Controllers containing business logic
* Services that know controller implementation details
* Domain code depending on Spring/HTTP
* Duplicated error handling across controllers
* Code errors in IDE

### ✅ Do

* Keep business rules in the service or domain layer
* Use `Result` for expected failures
* Document non-obvious decisions
* Reuse helpers (`ResultHttpResponseHelper`, `BaseEntity`) whenever a pattern exists
* Preserve the standardized HTTP flow

---

## Example of a New Module

If creating `modules/habit/`:

```text
// Domain: habit rules
// public class Habit extends BaseEntity {
//     private String name;
//     private UUID userId;
//     public static Habit create(/* ... */) { /* ... */ }
//     public void markHabitDay(/* ... */) { /* ... */ }
// }
```

```java
// Service: orchestrates the use case
public Result<HabitResponseDTO> completeHabit(Long habitId, UUID userId) {
    Habit habit = habitRepository.findByIdAndUserId(habitId, userId)
            .orElse(null);

    if (habit == null) {
        return Result.failure("HABIT_NOT_FOUND", "Habit not found");
    }

    if (habit.isCompletedToday()) {
        return Result.failure("HABIT_ALREADY_COMPLETED", "Already completed today");
    }

    habit.markAsCompleted();
    habitRepository.save(habit);

    return Result.success(new HabitResponseDTO(habit));
}

// Controller: HTTP bridge
@PostMapping("/{id}/complete")
public ResponseEntity<?> completeHabit(@PathVariable Long id) {
    UUID userId = extractUserFromJwt();
    Result<HabitResponseDTO> result = habitService.completeHabit(id, userId);
    return resultHttpResponseHelper.toResponseEntity(result, HttpStatus.OK);
}
```

---

## Executive Summary

1. **Modules** → each module is independent and follows the controller-service-domain-repository pattern
2. **Result Pattern** → expected failures are values, not exceptions
3. **Well-defined layers** → thin controller, orchestrating service, rule-centric domain, persistence-focused repository
4. **Explicit lifecycle** → study the `user` module; new modules should follow the same pattern
5. **Documentation close to the code** → Javadoc and comments explaining design decisions
6. **Structured testing** → organized by module, following the same folder structure
7. **Scheduled jobs** → handle automatic state transitions

**The goal:** a backend that grows while remaining **readable** and **locally understandable**, allowing AI agents to
navigate and contribute without ambiguity.
