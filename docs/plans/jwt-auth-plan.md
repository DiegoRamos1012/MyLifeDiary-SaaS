# Plano de Implementação — Autenticação JWT

> Branch: `feat/jwt-auth`
>
> Objetivo: inserir autenticação JWT no projeto mantendo a simplicidade, seguindo os padrões já estabelecidos e preparando o sistema para integração futura com provedores externos (OAuth2).

> **Estado atual no repositório:** a autenticação JWT básica e a rotação de refresh token já foram implementadas. Este documento permanece como referência de arquitetura e decisão técnica.

---

## 1. Visão geral da abordagem

A autenticação será construída sobre **Spring Security** com **JWT stateless**, usando o módulo `spring-security-oauth2-resource-server` — solução nativa do ecossistema Spring, sem bibliotecas extras desnecessárias.

Essa escolha:

- mantém o projeto alinhado com as dependências já existentes do Spring Boot;
- é a rota oficial para integração futura com OAuth2 (Google, Facebook);
- torna o `SecurityConfig` simples e declarativo;
- não quebra o `Result Pattern` nem a estrutura de módulos.

O JWT será **assinado com chave HMAC-SHA256** inicialmente. A estrutura já permite migrar para RSA (assimétrico) quando necessário para provedores externos.

---

## 2. Dependências a adicionar

Apenas uma dependência nova no `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

> Esse starter inclui suporte nativo a JWT (`BearerTokenAuthenticationFilter`, `JwtDecoder`, `JwtEncoder`) sem precisar de `jjwt` ou outras libs externas.

---

## 3. Banco de dados — Flyway

### Nova migration

**Arquivo:** `V2__add_user_role.sql`

```sql
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
```

- Todos os usuários existentes recebem `USER` automaticamente.
- O valor `DEFAULT 'USER'` garante compatibilidade retroativa.

---

## 4. Domínio — `UserRole`

**Novo enum:** `modules/user/domain/enums/UserRole`

```
USER  → usuário padrão; acessa apenas os próprios dados
ADMIN → acesso total; inclui rotas de auditoria e controle
```

### Atualização da entidade `User`

Adicionar o campo `role` ao agregado:

- tipo: `UserRole`
- mapeamento JPA: `@Enumerated(EnumType.STRING)`
- valor padrão na fábrica `User.create(...)`: sempre `UserRole.USER`
- Javadoc atualizado descrevendo o campo e as invariantes

---

## 5. Novo módulo — `auth`

Estrutura de pacotes a criar:

```text
modules/auth/
├── controller/
│   └── AuthController
├── service/
│   └── AuthService
└── dto/
    ├── request/
    │   └── LoginRequest
    └── response/
        └── AuthResponse
```

### 5.1 `LoginRequest` (DTO)

Campos:
- `email` — `@NotBlank`, `@Email`
- `password` — `@NotBlank`

### 5.2 `AuthResponse` (DTO)

Campos:
- `accessToken` — o token JWT gerado
- `refreshToken` — token opaco de longa duração salvo no banco
- `tokenType` — sempre `"Bearer"`
- `expiresIn` — duração em segundos

### 5.3 `AuthService`

Responsabilidade:
1. verificar se o usuário existe pelo e-mail;
2. verificar se a senha fornecida bate com o hash (`PasswordEncoder#matches`);
3. verificar se o status da conta é `ACTIVE` (bloquear `SUSPENDED`, `INACTIVE`, `PENDING_DELETION`);
4. gerar e retornar o par de tokens do login (`accessToken` + `refreshToken`).

> A renovação e o logout por refresh token foram adicionados posteriormente e estão documentados em `docs/refresh-token-plan.md`.

Retorna `Result<AuthResponse>` para falhas esperadas:
- `AUTH_INVALID_CREDENTIALS` → e-mail não encontrado ou senha incorreta (mensagem genérica por segurança)
- `AUTH_ACCOUNT_NOT_ACTIVE` → conta não está em status `ACTIVE`

> **Nota de segurança:** o erro de e-mail inexistente e o erro de senha errada devem retornar a **mesma mensagem genérica** para evitar enumeração de usuários.

### 5.4 `AuthController`

Endpoint único nesta fase:

| Método | Rota           | Acesso     | Descrição                   |
|--------|----------------|------------|-----------------------------|
| POST   | `/auth/login`  | público    | Autentica e retorna o token |

Na implementação atual do repositório, o módulo `auth` também expõe:

| Método | Rota            | Acesso  | Descrição                                      |
|--------|-----------------|---------|------------------------------------------------|
| POST   | `/auth/refresh` | público | Renova access token e refresh token            |
| POST   | `/auth/logout`  | público | Revoga o refresh token informado               |

Segue o padrão do projeto:
- `@Valid` no request
- `ResultHttpResponseHelper.respond(...)` para traduzir o `Result` em HTTP
- anotações Swagger (`@Operation`, `@ApiResponses`, `@Tag`)

---

## 6. Infraestrutura JWT — `config/security/`

### 6.1 `JwtProperties`

Record ou classe `@ConfigurationProperties` para isolar as propriedades JWT:

```
jwt.secret      → chave HMAC (mínimo 32 bytes, via variável de ambiente)
jwt.expiration  → tempo de vida do token em segundos (ex.: 3600)
```

> A chave **nunca** deve ficar no `application.properties` commitado. Deve ser lida de variável de ambiente ou secrets.

### 6.2 `JwtService`

Encapsula geração e validação do JWT:

- `generateToken(UserDetails)` → cria o JWT com claims: `sub` (email), `role`, `iat`, `exp`
- `extractEmail(String token)` → lê o subject do token
- `isTokenValid(String token, UserDetails)` → valida assinatura e expiração

### 6.3 `UserDetailsServiceImpl`

Implementa `UserDetailsService` do Spring Security.

Responsabilidade:
- `loadUserByUsername(email)` → busca o usuário no repositório e retorna um `UserDetails` com:
  - username: e-mail
  - password: `passwordHash`
  - authorities: `ROLE_USER` ou `ROLE_ADMIN`

> Retorna `UsernameNotFoundException` se o e-mail não for encontrado (comportamento padrão do Spring Security).

### 6.4 `SecurityConfig` — atualização completa

Mudanças:

1. **Sessão stateless** — `SessionCreationPolicy.STATELESS`
2. **Configuração do JWT filter** — `oauth2ResourceServer(oauth2 -> oauth2.jwt(...))` via `BearerTokenAuthenticationFilter`
3. **Autorização por rota:**

| Rota                                  | Acesso mínimo         |
|---------------------------------------|-----------------------|
| `POST /auth/login`                    | público               |
| `POST /users/register`                | público               |
| `GET /v3/api-docs/**`                 | público               |
| `GET /swagger-ui/**`                  | público               |
| `GET /users`                          | `ADMIN`               |
| `PATCH /users/{userId}/reactivate`    | `ADMIN`               |
| `PATCH /users/{userId}/userInfo`      | autenticado           |
| `PATCH /users/{userId}/changeEmail`   | autenticado           |
| `PATCH /users/{userId}/changePassword`| autenticado           |
| `PATCH /users/{userId}/deactivate`    | autenticado           |

4. **`JwtDecoder` bean** — configurado com a chave HMAC do `JwtProperties`
5. **`JwtEncoder` bean** — para geração dos tokens no `JwtService`

---

## 7. Proteção de endpoints do `UserController`

### Mudanças necessárias

- `GET /users` (findAll): restrito a `ADMIN` via `SecurityConfig`
- Todos os `PATCH /users/{userId}/...`: autenticado (o token já identifica o usuário)
- Comentário `// @SecurityRequirement(name = bearerAuth)` ativado no controller

> Para uma segunda iteração: validar se o `userId` do path corresponde ao usuário logado (para as rotas de autoatendimento), ou se é `ADMIN`. Isso **não está no escopo inicial desta branch**, mas o design permite adicionar facilmente com `@AuthenticationPrincipal`.

---

## 8. Swagger — atualização completa

### 8.1 `SwaggerConfig` — adicionar `SecurityScheme`

```java
.components(new Components()
    .addSecuritySchemes("bearerAuth", new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")
        .description("Insira o token JWT obtido em POST /auth/login")))
```

### 8.2 Endpoints públicos

Sem `@SecurityRequirement` — ficam abertos no Swagger.

### 8.3 Endpoints protegidos

Anotar com `@SecurityRequirement(name = "bearerAuth")`:
- `UserController` inteiro — `@SecurityRequirement` no nível da classe
- `AuthController` — apenas a rota de login permanece sem o requisito

### 8.4 Documentar `AuthController` no Swagger

Respostas a documentar no `POST /auth/login`:
- `200 OK` — token retornado
- `400 Bad Request` — request inválido (campos vazios)
- `401 Unauthorized` — credenciais inválidas ou conta inativa

---

## 9. `GlobalExceptionHandler` — atualização

Tratar `AuthenticationException` e `AccessDeniedException` do Spring Security:

| Exceção                    | HTTP | Código          | Mensagem                            |
|----------------------------|------|-----------------|-------------------------------------|
| `AuthenticationException`  | 401  | `UNAUTHORIZED`  | Token ausente, inválido ou expirado |
| `AccessDeniedException`    | 403  | `FORBIDDEN`     | Acesso não permitido                |

> O Spring Security já responde com 401/403 por padrão, mas customizar via `@ExceptionHandler` mantém a resposta no padrão `ApiErrorResponse` do projeto.

Alternativamente, configurar `authenticationEntryPoint` e `accessDeniedHandler` diretamente no `SecurityConfig` (mais limpo para erros de segurança).

---

## 10. Preparação para OAuth2 futuro

O design desta branch já prevê integração futura com provedores externos:

1. **`UserDetailsService`** — ponto de extensão central já implementado
2. **`oauth2ResourceServer`** — o mesmo mecanismo do Spring suporta validar tokens emitidos por Google/GitHub
3. **Campo `authProvider` (futuro)** — quando necessário, adicionar via nova migration:
   - enum `AuthProvider`: `LOCAL`, `GOOGLE`, `FACEBOOK`
   - valor default: `LOCAL` para todos os usuários existentes
4. **`passwordHash` opcional (futuro)** — em contas OAuth, o hash pode ser nulo/ignorado; a entidade `User` já isola o campo com acesso controlado

> Nenhuma dessas adições quebra o que esta branch implementa.

---

## 11. Arquivos afetados e criados

### Criados

```
docs/jwt-auth-plan.md                                  ← este arquivo
src/main/resources/db/migration/V2__add_user_role.sql
modules/user/domain/enums/UserRole.java
modules/auth/controller/AuthController.java
modules/auth/service/AuthService.java
modules/auth/dto/request/LoginRequest.java
modules/auth/dto/response/AuthResponse.java
modules/auth/dto/request/RefreshRequest.java
modules/auth/domain/entity/RefreshToken.java
modules/auth/repository/RefreshTokenRepository.java
config/security/JwtProperties.java
config/security/JwtService.java
config/security/UserDetailsServiceImpl.java
src/main/resources/db/migration/V1__create_users.sql
src/main/resources/db/migration/V3__create_refresh_tokens.sql
```

### Modificados

```
pom.xml                                     ← nova dependência oauth2-resource-server
modules/user/domain/entity/User.java        ← campo role + fábrica atualizada
modules/user/dto/response/UserResponseDTO   ← incluir role na resposta
modules/user/controller/UserController.java ← @SecurityRequirement ativado
config/security/SecurityConfig.java        ← JWT filter + autorização por rota
config/swagger/SwaggerConfig.java          ← SecurityScheme bearerAuth
common/exception/GlobalExceptionHandler    ← 401 e 403 padronizados
application.properties                     ← referência às propriedades JWT
```

---

## 12. Códigos de erro novos

Seguindo a convenção do projeto (caixa alta, underscore, estável):

| Código                    | Situação                                                  |
|---------------------------|-----------------------------------------------------------|
| `AUTH_INVALID_CREDENTIALS`| E-mail ou senha incorretos (mensagem genérica intencional)|
| `AUTH_ACCOUNT_NOT_ACTIVE` | Conta não está em status `ACTIVE`                         |

---

## 13. Segurança — checklist

- [ ] Chave JWT lida de variável de ambiente (nunca em `.properties` commitado)
- [ ] Mensagens de erro de login genéricas (sem vazar se e-mail existe ou não)
- [ ] Token com expiração curta (configurável, padrão sugerido: 1 hora)
- [ ] Sessão stateless (sem armazenamento de sessão no servidor)
- [ ] Senha validada com `PasswordEncoder#matches` (já usa BCrypt)
- [ ] Conta `SUSPENDED` / `INACTIVE` / `PENDING_DELETION` bloqueada no login
- [ ] `GET /users` acessível apenas por `ADMIN`
- [ ] CORS configurado conforme necessidade (a definir quando houver frontend)

---

## 14. Diagrama de fluxo de autenticação

```
Cliente                         AuthController              AuthService              JwtService
  │                                   │                          │                       │
  │── POST /auth/login ──────────────►│                          │                       │
  │   { email, password }             │── authenticate(dto) ────►│                       │
  │                                   │                          │── findByEmail ────────►│
  │                                   │                          │◄── User                │
  │                                   │                          │── matches(pwd, hash)   │
  │                                   │                          │── checkStatus          │
  │                                   │                          │── generateToken() ────►│
  │                                   │                          │◄── JWT string          │
  │                                   │◄── Result<AuthResponse> ─│                       │
  │◄── 200 { accessToken, ... } ──────│                          │                       │
  │                                   │                          │                       │
  │── PATCH /users/{id}/changeEmail   │                          │                       │
  │   Authorization: Bearer <token>   │                          │                       │
  │                                   │                          │                       │
  │              [Spring Security BearerTokenAuthenticationFilter valida o token]        │
  │                                   │                          │                       │
  │◄── 200 ou 40x ────────────────────│                          │                       │
```

---

## 15. O que esta branch **não** inclui (escopo futuro)

- Refresh token
- Revogação de tokens (token blacklist)
- Login com provedores externos (Google, Facebook) — estrutura preparada, mas não implementada
- Validação de propriedade do recurso (garantir que `userId` do path == usuário logado)
- Rate limiting no endpoint de login
- 2FA

---

## 16. Referências

- `docs/project-architecture.md` — arquitetura e convenções do projeto
- `docs/ResultPattern.md` — uso do `Result` no projeto
- `docs/user-lifecycle-flow.md` — ciclo de vida do usuário
