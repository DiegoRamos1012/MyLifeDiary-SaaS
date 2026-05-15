# MyLifeDiary SaaS

Backend do **MyLifeDiary**, construído com **Spring Boot**, com foco em:

- organização por módulos de negócio;
- regras de domínio claras;
- manutenção simples;
- documentação próxima do código;
- respostas HTTP padronizadas;
- evolução previsível do ciclo de vida do usuário.

---

## Visão geral

O projeto foi desenhado para manter o fluxo de negócio fácil de entender tanto por humanos quanto por agentes
automatizados.

A base da arquitetura segue estes princípios:

- **controller** apenas recebe a requisição e devolve a resposta HTTP;
- **service** orquestra o caso de uso e retorna `Result` para falhas esperadas;
- **domain** concentra regras e invariantes do agregado;
- **repository** cuida da persistência;
- **jobs** automatizam transições de estado de longa duração;
- **common** concentra peças reutilizáveis do projeto.

O projeto também já possui um módulo de autenticação JWT com emissão de access token e refresh token.

---

## Stack

- Java 25
- Spring Boot 4.0.5
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Spring Validation
- Flyway
- Swagger / OpenAPI com Springdoc
- H2 para ambiente local
- PostgreSQL para ambiente alvo
- Lombok

---

## Principais conceitos do projeto

### Result Pattern

O projeto usa `Result` para representar **falhas esperadas** sem recorrer a exceções para regras comuns de negócio.

- `Result.success(...)` para sucesso
- `Result.failure(...)` para falhas conhecidas
- `fold(...)` para converter sucesso/falha em um retorno final

A documentação detalhada está em:

- [`docs/ResultPattern.md`](docs/arch/ResultPattern.md)

### Exceções de domínio

As exceções ficam reservadas para cenários realmente excepcionais ou inesperados.

- `DomainException` → falha de domínio não tratada como fluxo normal
- `GlobalExceptionHandler` → tratamento centralizado de erros inesperados

### Resposta HTTP padronizada

O projeto usa um helper para converter `Result` em resposta HTTP com corpo padronizado.

- sucesso → status configurado pelo controller
- falha esperada → `ApiErrorResponse`

### Ciclo de vida do usuário

O módulo `user` possui fluxo de desativação e reativação documentado em detalhe.

- [`docs/user-lifecycle-flow.md`](docs/arch/user-lifecycle-flow.md)

---

## Estrutura do projeto

```text
src/main/java/com/diegoramos/mylifediary
├── common
│   ├── base
│   ├── exception
│   ├── response
│   └── result
├── config
│   ├── security
│   ├── swagger
│   └── time
└── modules
    ├── auth
    │   ├── controller
    │   ├── domain
    │   ├── dto
    │   ├── repository
    │   └── service
    └── user
        ├── controller
        ├── domain
        ├── dto
        ├── job
        ├── repository
        └── service
```

### Documentação de arquitetura

Para entender o design do projeto com mais profundidade, veja:

- [`docs/project-architecture.md`](docs/arch/project-architecture.md)

Esse documento descreve:

- estilo de arquitetura;
- convenções de Javadoc;
- uso de `Result`;
- tratamento de exceções;
- organização de pastas;
- papéis de cada camada.

---

## Módulo atual

### `user`

O módulo de usuário já cobre:

- cadastro;
- leitura paginada;
- atualização de perfil;
- mudança de e-mail;
- mudança de senha;
- desativação lógica;
- restauração da conta;
- jobs automáticos de ciclo de vida.

### `auth`

O módulo de autenticação cobre:

- login com emissão de `accessToken` + `refreshToken`;
- renovação de tokens via `POST /auth/refresh`;
- logout via `POST /auth/logout`;
- proteção de endpoints por JWT e roles.

#### Resposta do login

O `POST /auth/login` retorna um `AuthResponse` com os campos:

- `accessToken`
- `refreshToken`
- `tokenType` (`Bearer`)
- `expiresIn`

---

## Como executar localmente

### Pré-requisitos

- Java 25
- Maven Wrapper (`mvnw`)

### Subir a aplicação

```bash
./mvnw spring-boot:run
```

No Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

### Executar testes

```bash
./mvnw test
```

No Windows PowerShell:

```powershell
.\mvnw.cmd test
```

---

## Swagger / OpenAPI

A documentação da API é exposta via configuração central de Swagger.

Depois que a aplicação estiver rodando, a interface de documentação fica disponível no endpoint padrão do Springdoc.

O esquema de autenticação usa `bearerAuth`; o `POST /auth/login` permanece público, enquanto os demais endpoints
protegidos exigem token JWT no header `Authorization: Bearer <token>`.

---

## Convenções adotadas

- documentação próxima do código;
- controllers finos;
- services responsáveis pelo fluxo de negócio;
- domínio com regras e invariantes próprias;
- `Result` para erros esperados;
- exceções para erros inesperados;
- jobs para automação de mudanças de estado de longa duração.

---

## Objetivo do projeto

A ideia do projeto é manter o backend:

- fácil de ler;
- fácil de evoluir;
- consistente no tratamento de erros;
- previsível para novos módulos;
- documentado o bastante para reduzir perda de contexto.

---

## Documentos úteis

- [`docs/project-architecture.md`](docs/arch/project-architecture.md)
- [`docs/ResultPattern.md`](docs/arch/ResultPattern.md)
- [`docs/user-lifecycle-flow.md`](docs/arch/user-lifecycle-flow.md)
- [`docs/jwt-auth-plan.md`](docs/plans/jwt-auth-plan.md)
- [`docs/refresh-token-plan.md`](docs/plans/refresh-token-plan.md)

---

## Observação

Este repositório ainda está em evolução. A documentação foi pensada para acompanhar o código e servir como contexto
confiável conforme novos módulos forem sendo adicionados.

