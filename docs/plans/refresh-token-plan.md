# Plano de Implementação — Refresh Token

> Branch sugerida: `feat/refresh-token`
>
> **Pré-requisito:** a branch `feat/jwt-auth` (plano em `jwt-auth-plan.md`) deve estar mesclada antes desta, pois o login (`POST /auth/login`) é o ponto de emissão do primeiro par de tokens.
>
> **Escopo desta branch:** apenas o refresh token — emissão, renovação e logout. Autorização de rotas protegidas fica para uma branch dedicada posterior.

> **Estado atual no repositório:** o fluxo de refresh token já está implementado junto com login e logout em `modules/auth/`.

---

## 1. O que o refresh token resolve

O access token tem vida curta (ex.: 15 minutos) para limitar o dano em caso de vazamento. O problema é que o cliente precisaria pedir login novamente a cada 15 minutos.

O refresh token é um token de longa duração (ex.: 7 dias) armazenado no banco de dados. Ele permite ao cliente renovar o access token sem precisar de e-mail e senha novamente.

**Fluxo resumido:**

```
login             → access token (15min) + refresh token (7 dias, salvo no banco)
access expirado   → POST /auth/refresh → novo access token + novo refresh token
logout            → refresh token invalidado no banco
```

---

## 2. Banco de dados — Flyway

### Migration `V3__create_refresh_tokens.sql`

```sql
CREATE TABLE refresh_tokens (
    id             UUID        NOT NULL PRIMARY KEY,
    user_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token          TEXT        NOT NULL UNIQUE,
    expires_at     TIMESTAMPTZ NOT NULL,
    revoked        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL
);
```

- `ON DELETE CASCADE` garante que ao deletar um usuário os tokens também saem.
- `UNIQUE` no campo `token` viabiliza busca direta sem índice extra.
- `revoked` permite invalidar um token sem precisar deletar a linha imediatamente (facilita auditoria futura).

---

## 3. Entidade de domínio — `RefreshToken`

**Localização:** `modules/auth/domain/entity/RefreshToken.java`

Campos:

| Campo        | Tipo      | Observação                              |
|--------------|-----------|-----------------------------------------|
| `id`         | `UUID`    | UUID v7 via `UuidCreator` (padrão)      |
| `userId`     | `UUID`    | FK para `users.id`                      |
| `token`      | `String`  | string aleatória segura (UUID ou SHA)   |
| `expiresAt`  | `Instant` | instante de expiração                   |
| `revoked`    | `boolean` | se foi invalidado                       |
| `createdAt`  | `Instant` | gerado no `@PrePersist`                 |

> **Nota de design:** `RefreshToken` **não estende `BaseEntity`** — os campos `lastTimeChanged` e `updatedAt` não fazem sentido aqui, e a entidade é essencialmente imutável após criação. O `@PrePersist` local define apenas `id` e `createdAt`.

Métodos:

- `RefreshToken.create(userId, token, expiresAt)` — fábrica estática, mesma convenção de `User.create(...)`
- `isExpired(Instant now)` — retorna `true` se `expiresAt` for anterior a `now`
- `revoke()` — marca `revoked = true`

---

## 4. Repositório — `RefreshTokenRepository`

**Localização:** `modules/auth/repository/RefreshTokenRepository.java`

Interface Spring Data JPA:

```java
Optional<RefreshToken> findByToken(String token);
void deleteByUserId(UUID userId);
```

- `findByToken` é o ponto principal de busca (chamado no refresh e no logout).
- `deleteByUserId` é chamado em operações de logout completo ("sair de todos os dispositivos") — útil no futuro, mas já previsto.

---

## 5. Módulo `auth` — estrutura de pacotes completa

Combinando o que já estava previsto em `jwt-auth-plan.md` com o que esta branch adiciona:

```text
modules/auth/
├── controller/
│   └── AuthController          ← endpoints: /login, /refresh, /logout
├── domain/
│   └── entity/
│       └── RefreshToken        ← entidade desta branch
├── repository/
│   └── RefreshTokenRepository  ← desta branch
├── service/
│   └── AuthService             ← login (jwt-auth) + refresh/logout (esta branch)
└── dto/
    ├── request/
    │   ├── LoginRequest        ← do jwt-auth-plan
    │   └── RefreshRequest      ← desta branch
    └── response/
        └── AuthResponse        ← atualizado para incluir refreshToken
```

---

## 6. DTOs

### `RefreshRequest`

```
refreshToken: String  (@NotBlank)
```

### `AuthResponse` — atualização

Adicionar `refreshToken` à resposta do login:

```
accessToken:  String   → JWT de curta duração
refreshToken: String   → token opaco de longa duração
tokenType:    String   → sempre "Bearer"
expiresIn:    long     → expiração do access token em segundos
```

---

## 7. `AuthService` — métodos desta branch

O `AuthService` já existirá com o método `login(LoginRequest)` do `jwt-auth-plan`. Esta branch adiciona dois métodos seguindo exatamente o mesmo padrão `Result<T>`:

### `refresh(RefreshRequest dto)`

Lógica em ordem:

1. Buscar o token no banco por `findByToken(dto.refreshToken())`.
2. Se não encontrado → `Result.failure("AUTH_REFRESH_TOKEN_NOT_FOUND", ...)`
3. Se `revoked == true` → `Result.failure("AUTH_REFRESH_TOKEN_REVOKED", ...)`
4. Se `isExpired(clock.instant())` → `Result.failure("AUTH_REFRESH_TOKEN_EXPIRED", ...)`
5. Revogar o token atual (`token.revoke()` + `save`).
6. Gerar novo refresh token e salvar.
7. Gerar novo access token via `JwtService`.
8. Retornar `Result.success(new AuthResponse(...))` com o par novo.

> **Por que invalidar o token antigo?** — É a prática chamada *Refresh Token Rotation*. Se o mesmo refresh token aparecer duas vezes (foi roubado e reutilizado), o servidor detecta que ele já foi consumido. Aqui a regra `revoked == true` captura isso.

### `logout(RefreshRequest dto)`

Lógica em ordem:

1. Buscar o token no banco.
2. Se não encontrado → `Result.failure("AUTH_REFRESH_TOKEN_NOT_FOUND", ...)`
3. Revogar o token.
4. Retornar `Result.success(null)` (sem corpo relevante; o controller retornará 204).

---

## 8. `AuthController` — endpoints desta branch

| Método | Rota             | Acesso  | Descrição                                      |
|--------|------------------|---------|------------------------------------------------|
| POST   | `/auth/refresh`  | público | Recebe refreshToken, devolve novo par de tokens |
| POST   | `/auth/logout`   | público | Invalida o refreshToken                        |

> "Público" aqui significa sem validação de access token no header — o refresh token é a própria credencial da requisição. Isso é intencional para que clientes com access token expirado ainda possam renovar.

Segue o padrão:
- `@Valid` no request
- `ResultHttpResponseHelper.respond(...)` para traduzir `Result` em HTTP
- `logout` usa `HttpStatus.NO_CONTENT` (204) no caso de sucesso
- Anotações Swagger (`@Operation`, `@ApiResponses`, `@Tag`)

### Códigos de erro novos para o `ResultHttpResponseHelper`

| Código                          | HTTP | Situação                             |
|---------------------------------|------|--------------------------------------|
| `AUTH_REFRESH_TOKEN_NOT_FOUND`  | 401  | Token não existe no banco            |
| `AUTH_REFRESH_TOKEN_REVOKED`    | 401  | Token já foi utilizado ou revogado   |
| `AUTH_REFRESH_TOKEN_EXPIRED`    | 401  | Token passou da data de expiração    |

---

## 9. Propriedades de configuração

Adicionar em `application.properties` (valores reais via variável de ambiente):

```properties
jwt.refresh-token.expiration-days=7
```

Registrar em `JwtProperties` (do `jwt-auth-plan`):

```
refreshTokenExpirationDays: long
```

---

## 10. Testes unitários

Todos os testes seguem o mesmo padrão do projeto (`UserServiceTest` / `UserTest`): Mockito puro, sem Spring context, sem banco real. Localização dos arquivos:

```
src/test/java/com/diegoramos/mylifediary/
├── modules/auth/
│   ├── domain/entity/
│   │   └── RefreshTokenTest.java
│   └── service/
│       └── AuthServiceTest.java
```

---

### 10.1 `RefreshTokenTest` — entidade de domínio

Espelha o padrão de `UserTest`: testa apenas os métodos públicos da entidade, sem mocks.

**Setup (helper estático):**

```java
private static RefreshToken createValidToken() {
    return RefreshToken.create(
        UUID.randomUUID(),            // userId
        "token-opaco-valido",
        Instant.parse("2099-01-01T00:00:00Z")  // expiresAt no futuro
    );
}
```

**Cenários:**

| Método de teste | O que valida |
|---|---|
| `createShouldReturnTokenWithCorrectFields` | Todos os campos refletem os argumentos; `revoked` começa `false` |
| `createShouldThrowWhenUserIdIsNull` | `RefreshToken.create(null, token, exp)` → `DomainException` |
| `createShouldThrowWhenTokenIsBlank` | `RefreshToken.create(id, " ", exp)` → `DomainException` |
| `createShouldThrowWhenExpiresAtIsNull` | `RefreshToken.create(id, token, null)` → `DomainException` |
| `isExpiredShouldReturnTrueWhenPastExpiration` | `expiresAt` 1 segundo atrás → `isExpired(now)` retorna `true` |
| `isExpiredShouldReturnFalseWhenBeforeExpiration` | `expiresAt` no futuro → `isExpired(now)` retorna `false` |
| `isExpiredShouldReturnTrueWhenExactlyAtExpiration` | `expiresAt == now` → `isExpired(now)` retorna `true` (borda: expirado no instante exato) |
| `revokeShouldSetRevokedToTrue` | Após `revoke()`, `isRevoked()` retorna `true` |
| `revokeShouldBeIdempotent` | Chamar `revoke()` duas vezes não lança exceção e mantém `revoked == true` |

**Exemplo de teste representativo:**

```java
@Test
void isExpiredShouldReturnTrueWhenPastExpiration() {
    Instant past = Instant.parse("2020-01-01T00:00:00Z");
    RefreshToken token = RefreshToken.create(UUID.randomUUID(), "tok", past);
    Instant now = Instant.parse("2026-05-15T00:00:00Z");

    assertTrue(token.isExpired(now));
}

@Test
void revokeShouldSetRevokedToTrue() {
    RefreshToken token = createValidToken();
    assertFalse(token.isRevoked());

    token.revoke();

    assertTrue(token.isRevoked());
}
```

---

### 10.2 `AuthServiceTest` — camada de serviço

Espelha o padrão de `UserServiceTest`: `@ExtendWith(MockitoExtension.class)`, mocks via `@Mock`, `@InjectMocks`, `@Captor` e `Clock` fixo no `@BeforeEach`.

**Setup da classe:**

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private Clock clock;

    @InjectMocks private AuthService authService;

    @Captor private ArgumentCaptor<RefreshToken> tokenCaptor;

    private final Instant now = Instant.parse("2026-05-15T00:00:00Z");

    @BeforeEach
    void setupClock() {
        when(clock.instant()).thenReturn(now);
    }
}
```

---

#### Cenários para `refresh(RefreshRequest dto)`

| Método de teste | Condição simulada | Resultado esperado |
|---|---|---|
| `refresh_tokenNotFound_returnsFailure` | `findByToken` retorna `Optional.empty()` | `isFailure()` com código `AUTH_REFRESH_TOKEN_NOT_FOUND` |
| `refresh_tokenRevoked_returnsFailure` | Token com `revoked = true` | `isFailure()` com código `AUTH_REFRESH_TOKEN_REVOKED` |
| `refresh_tokenExpired_returnsFailure` | Token com `expiresAt` no passado e `revoked = false` | `isFailure()` com código `AUTH_REFRESH_TOKEN_EXPIRED` |
| `refresh_validToken_revokesOldToken` | Token válido | `tokenCaptor` captura o token salvo com `revoked = true` |
| `refresh_validToken_savesNewToken` | Token válido | `save` é chamado 2 vezes: revogar o antigo + salvar o novo |
| `refresh_validToken_returnsNewAccessToken` | Token válido, `JwtService` retorna `"novo-jwt"` | `result.getValue().accessToken()` é `"novo-jwt"` |
| `refresh_validToken_returnsNewRefreshToken` | Token válido | `result.getValue().refreshToken()` é diferente do token original |

**Exemplo de teste representativo:**

```java
@Test
void refresh_tokenRevoked_returnsFailure() {
    RefreshToken revoked = buildToken(now.plusSeconds(3600), /* revoked= */ true);
    when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(revoked));

    Result<AuthResponse> result = authService.refresh(new RefreshRequest("tok"));

    assertTrue(result.isFailure());
    assertEquals("AUTH_REFRESH_TOKEN_REVOKED", result.getError().code());
    verify(refreshTokenRepository, never()).save(any());
}

@Test
void refresh_validToken_revokesOldToken() {
    RefreshToken valid = buildToken(now.plusSeconds(3600), false);
    when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(valid));
    when(jwtService.generateToken(any())).thenReturn("novo-jwt");
    when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    authService.refresh(new RefreshRequest("tok"));

    verify(refreshTokenRepository, atLeastOnce()).save(tokenCaptor.capture());
    // o primeiro save deve ser o token antigo com revoked = true
    assertTrue(tokenCaptor.getAllValues().get(0).isRevoked());
}
```

---

#### Cenários para `logout(RefreshRequest dto)`

| Método de teste | Condição simulada | Resultado esperado |
|---|---|---|
| `logout_tokenNotFound_returnsFailure` | `findByToken` retorna `Optional.empty()` | `isFailure()` com código `AUTH_REFRESH_TOKEN_NOT_FOUND` |
| `logout_validToken_revokesToken` | Token encontrado | `save` é chamado com o token marcado como `revoked = true` |
| `logout_validToken_returnsSuccess` | Token encontrado | `result.isSuccess()` é `true` |
| `logout_alreadyRevokedToken_stillSucceeds` | Token já revogado | Não lança exceção; `revoke()` é idempotente; `save` é chamado |

**Exemplo de teste representativo:**

```java
@Test
void logout_validToken_revokesToken() {
    RefreshToken token = buildToken(now.plusSeconds(3600), false);
    when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));
    when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Result<?> result = authService.logout(new RefreshRequest("tok"));

    assertTrue(result.isSuccess());
    verify(refreshTokenRepository).save(tokenCaptor.capture());
    assertTrue(tokenCaptor.getValue().isRevoked());
}
```

---

#### Helper privado reutilizado nos testes do serviço

```java
// Cria um RefreshToken com campos controlados via reflexão (mesmo padrão
// de readPasswordHash em UserServiceTest) ou via fábrica da própria entidade.
private RefreshToken buildToken(Instant expiresAt, boolean revoked) {
    RefreshToken token = RefreshToken.create(UUID.randomUUID(), "tok", expiresAt);
    if (revoked) token.revoke();
    return token;
}
```

---

### 10.3 Cobertura mínima esperada

| Classe | Cenários mínimos | Meta |
|---|---|---|
| `RefreshToken` | 9 cenários (entidade) | 100% dos métodos públicos |
| `AuthService#refresh` | 7 cenários | todos os ramos do `switch` / `if` |
| `AuthService#logout` | 4 cenários | caminho feliz + não encontrado + idempotência |

---

## 11. Arquivos criados e modificados

### Criados

```
docs/refresh-token-plan.md                                      ← este arquivo
src/main/resources/db/migration/V1__create_users.sql
src/main/resources/db/migration/V3__create_refresh_tokens.sql
modules/auth/domain/entity/RefreshToken.java
modules/auth/repository/RefreshTokenRepository.java
modules/auth/dto/request/RefreshRequest.java
src/test/...modules/auth/domain/entity/RefreshTokenTest.java
src/test/...modules/auth/service/AuthServiceTest.java          ← cobre refresh + logout
```

### Modificados

```
modules/auth/service/AuthService.java       ← adicionar refresh() e logout()
modules/auth/controller/AuthController.java ← adicionar /refresh e /logout
modules/auth/dto/response/AuthResponse.java ← adicionar campo refreshToken
config/security/JwtProperties.java          ← adicionar refreshTokenExpirationDays
common/response/ResultHttpResponseHelper.java ← mapear os 3 novos códigos de erro
application.properties                      ← jwt.refresh-token.expiration-days
```

---

## 12. O que esta branch **não** inclui

- Proteção de rotas com o access token (SecurityConfig sem mudança)
- `@AuthenticationPrincipal` nos controllers
- Logout de todos os dispositivos simultâneos (já preparado via `deleteByUserId`, mas sem endpoint)
- Rate limiting no `/auth/refresh`
- Rotação automática de refresh token antes do vencimento

---

## 13. Sequência de implementação sugerida

A ordem abaixo minimiza retrabalho:

1. Migration `V3` + entidade `RefreshToken` + repositório
2. Atualizar `AuthResponse` com o campo `refreshToken`
3. Adicionar `refresh()` e `logout()` no `AuthService`
4. Atualizar `AuthController` com os dois novos endpoints
5. Atualizar `JwtProperties` e `application.properties`
6. Mapear os novos códigos de erro no `ResultHttpResponseHelper`
7. Testes: `RefreshTokenTest` e `AuthServiceTest`

---

## 14. Diagrama de fluxo

```
Cliente                    AuthController         AuthService              Banco
  │                              │                     │                     │
  │── POST /auth/refresh ───────►│                     │                     │
  │   { refreshToken }           │── refresh(dto) ────►│                     │
  │                              │                     │── findByToken ──────►│
  │                              │                     │◄── RefreshToken      │
  │                              │                     │  (verifica revoked,  │
  │                              │                     │   expirado)          │
  │                              │                     │── revoke() + save ──►│
  │                              │                     │── novoRefresh + save►│
  │                              │                     │── generateToken()    │
  │                              │◄── Result<AuthResponse>                    │
  │◄── 200 { accessToken,        │                     │                     │
  │          refreshToken }      │                     │                     │
  │                              │                     │                     │
  │── POST /auth/logout ────────►│                     │                     │
  │   { refreshToken }           │── logout(dto) ──────►│                    │
  │                              │                     │── findByToken ──────►│
  │                              │                     │── revoke() + save ──►│
  │◄── 204 No Content ───────────│                     │                     │
```
