# Plano de Implementação — Refresh Token

> Branch sugerida: `feat/refresh-token`
>
> **Pré-requisito:** a branch `feat/jwt-auth` (plano em `jwt-auth-plan.md`) deve estar mesclada antes desta, pois o login (`POST /auth/login`) é o ponto de emissão do primeiro par de tokens.
>
> **Escopo desta branch:** apenas o refresh token — emissão, renovação e logout. Autorização de rotas protegidas fica para uma branch dedicada posterior.

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

Seguindo o padrão de `UserServiceTest` — Mockito, sem Spring context:

### `AuthServiceTest`

Cenários para `refresh(...)`:

- token não encontrado → `AUTH_REFRESH_TOKEN_NOT_FOUND`
- token revogado → `AUTH_REFRESH_TOKEN_REVOKED`
- token expirado → `AUTH_REFRESH_TOKEN_EXPIRED`
- token válido → sucesso, token antigo revogado, novo par emitido

Cenários para `logout(...)`:

- token não encontrado → `AUTH_REFRESH_TOKEN_NOT_FOUND`
- token válido → sucesso, `revoked = true`

### `RefreshTokenTest`

Semelhante a `UserTest` — testa a entidade diretamente:

- `isExpired` retorna `true` quando `expiresAt` está no passado
- `isExpired` retorna `false` quando ainda não expirou
- `revoke()` altera o campo `revoked` para `true`
- `RefreshToken.create(...)` com campos obrigatórios nulos lança `DomainException`

---

## 11. Arquivos criados e modificados

### Criados

```
docs/refresh-token-plan.md                                      ← este arquivo
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
