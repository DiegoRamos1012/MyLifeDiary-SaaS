# MyLifeDiary — Projeto: Arquitetura, Convenções e Contexto

> Documento de referência do design atual do backend.
>
> Objetivo duplo:
> - servir como contexto confiável para agentes de IA;
> - ser legível e útil para humanos que entram no projeto.

---

## 1. Visão geral

O projeto é um backend Spring Boot organizado por **módulos de negócio** e por **camadas técnicas**, com foco explícito em:

- manutenção simples;
- leitura direta do fluxo;
- separação clara entre regra de negócio, HTTP e persistência;
- documentação próxima do código.

Hoje os módulos funcionais principais são `user` e `auth`, mas a estrutura já está preparada para crescer por novos módulos seguindo o mesmo padrão.

---

## 2. Stack e base técnica

Pelo `pom.xml`, o projeto usa atualmente:

- **Java 25**
- **Spring Boot 4.0.5**
- **Spring Web MVC**
- **Spring Data JPA**
- **Spring Security**
- **Spring Validation**
- **Flyway**
- **OpenAPI / Swagger** com `springdoc`
- **H2** para runtime local
- **PostgreSQL** para runtime real
- **Lombok**
- **UUID Creator**

O arquivo `application.properties` está mínimo no momento, indicando que a configuração principal tende a vir de perfis, variáveis de ambiente ou expansão futura.

---

## 3. Estilo arquitetural

### 3.1 Organização por domínio + camadas

A estrutura combina duas ideias:

1. **módulos por contexto de negócio**
2. **camadas internas em cada módulo**

Exemplo atual:

```text
com.diegoramos.mylifediary
├── common
├── config
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

### 3.2 Responsabilidade por pacote

- `common/` → componentes reutilizáveis transversais
- `config/` → configuração da aplicação
- `modules/` → funcionalidades de negócio isoladas por módulo

Essa separação ajuda a evitar um pacote `service`, `controller` ou `repository` global demais, que normalmente cresce sem fronteiras.

---

## 4. Estrutura de pastas

### 4.1 `common/`

Contém código compartilhado entre módulos.

Subpacotes atuais:

- `base/`
- `exception/`
- `response/`
- `result/`

#### Papel de cada um

- `base/` → entidades base e comportamentos comuns de persistência
- `exception/` → exceções de domínio e tratamento global
- `response/` → respostas HTTP padronizadas e helpers de tradução
- `result/` → padrão para representar sucesso/falha esperada

### 4.2 `config/`

Configurações do Spring e de infraestrutura.

Subpacotes atuais:

- `security/`
- `swagger/`
- `time/`

### 4.3 `modules/`

Cada módulo de negócio deve viver aqui.

Hoje existe o módulo:

- `auth/`
- `user/`

Subpacotes:

- `controller/`
- `domain/`
- `dto/`
- `job/`
- `repository/`
- `service/`

No módulo `auth`, o subpacote `job/` não existe; os fluxos ficam concentrados em controller, service, repository e domínio.

---

## 5. Papel de cada camada

## 5.1 Controller

O controller é a borda HTTP da aplicação.

Responsabilidades:

- receber requisições;
- validar entradas com Bean Validation (`@Valid`);
- chamar o service;
- converter o resultado em resposta HTTP;
- documentar endpoints com Swagger/OpenAPI.

O controller **não** deve carregar regra de negócio complexa.

### Padrão atual

O controller usa `ResultHttpResponseHelper` para evitar repetição no tratamento de sucesso/falha.

Exemplo de intenção:

- service devolve `Result<T>`;
- controller chama o helper;
- helper converte para `ResponseEntity<?>`.

### Exemplo do que o controller NÃO deve fazer

- decidir regra de domínio;
- duplicar código de tradução de erro em vários endpoints;
- conter lógica de persistência;
- conhecer detalhes internos do agregado além do necessário.

---

## 5.2 Service

O service coordena o caso de uso.

Responsabilidades:

- orquestrar fluxo;
- consultar repositórios;
- aplicar decisões esperadas de negócio;
- devolver `Result<T>` quando a falha faz parte do fluxo normal;
- deixar exceções para casos excepcionais.

### Exemplo de uso do `Result`

No `UserService`, operações como:

- criação de usuário;
- atualização de perfil;
- mudança de e-mail;
- mudança de senha;
- solicitação de desativação;
- restauração de conta;

retornam `Result<UserResponseDTO>`.

Isso permite que o service trate situações previstas como:

- e-mail já cadastrado;
- usuário não encontrado;
- operação não permitida pelo status atual;
- tentativa de atualização vazia;
- dados inválidos de entrada no fluxo esperado.

---

## 5.3 Domain

O domínio concentra a identidade e as regras internas da entidade.

No módulo `user`, a entidade principal é `User`.

### Papel do domínio

- manter invariantes locais;
- normalizar dados;
- aplicar regras que pertencem ao próprio agregado;
- expor comportamentos como métodos de intenção clara.

### Estado atual do agregado `User`

A entidade inclui campos como:

- `fullName`
- `email`
- `passwordHash`
- `birthDate`
- `status`
- `deletionRequestedAt`

E comportamentos como:

- `create(...)`
- `requestDeletion(...)`
- `restoreAccount()`
- `changeEmail(...)`
- `changePassword(...)`
- `changeProfileInfo(...)`

---

## 6. `Result Pattern`

O projeto usa `Result` para representar **falhas esperadas** sem lançar exceção para cada regra comum.

### 6.1 Intenção

A ideia é simples:

- **resultado esperado** → `Result`
- **erro inesperado** → exceção

### 6.2 Classes envolvidas

- `Result<T>` → envelope de sucesso ou falha
- `ResultError` → descrição padronizada da falha esperada

### 6.3 Quando usar `Result`

Use `Result` para:

- validações previsíveis de negócio;
- conflitos de estado já conhecidos;
- duplicidade esperada;
- recurso não encontrado em fluxo normal;
- operação recusada por regra funcional;
- qualquer situação que faça parte do comportamento normal do sistema.

### 6.4 Quando usar exceção

Use exceção para:

- erro técnico;
- falha inesperada;
- estado impossível;
- bug;
- quebra de infraestrutura;
- qualquer coisa que não faça parte do fluxo normal.

### 6.5 API principal do `Result`

O `Result` já expõe:

- `success(...)`
- `failure(...)`
- `isSuccess()`
- `isFailure()`
- `getValue()`
- `getError()`
- `map(...)`
- `flatMap(...)`
- `fold(...)`
- `orElse(...)`
- `orElseGet(...)`
- `orElseThrow(...)`

### 6.6 `fold(...)`

O `fold(...)` converte o resultado em um valor final escolhendo entre os dois caminhos:

- sucesso;
- falha.

No projeto, ele é útil principalmente em controller/helper porque transforma `Result<T>` em uma resposta HTTP final.

### 6.7 Convenção de `code`

Os códigos de erro devem ser:

- estáveis;
- em caixa alta;
- com underscore;
- fáceis de buscar;
- fáceis de mapear para HTTP.

Exemplos já usados no projeto:

- `USER_EMAIL_ALREADY_EXISTS`
- `USER_NOT_FOUND`
- `USER_INFO_EMPTY_UPDATE`
- `USER_INVALID_FULL_NAME`
- `USER_ALREADY_ACTIVE`
- `USER_ALREADY_INACTIVE`
- `USER_SUSPENDED`
- `USER_RESTORE_NOT_ALLOWED`
- `DELETION_ALREADY_REQUESTED`

---

## 7. Resposta HTTP padronizada

### 7.1 `ApiErrorResponse`

A resposta de erro do projeto é o record `ApiErrorResponse`.

Campos atuais:

- `timestamp`
- `status`
- `error`
- `message`
- `path`
- `details`

A anotação `@JsonInclude(JsonInclude.Include.NON_EMPTY)` evita serializar campos vazios desnecessários.

### 7.2 `ResultHttpResponseHelper`

Esse helper centraliza a tradução de `Result<T>` para `ResponseEntity<?>`.

Objetivo:

- manter controllers enxutos;
- evitar repetição;
- padronizar sucesso e erro esperado.

### 7.3 Papel atual do helper

Ele faz duas coisas:

1. retorna o corpo de sucesso com o `HttpStatus` desejado;
2. converte falhas esperadas em `ApiErrorResponse` com um status mapeado.

### 7.4 Status HTTP

A intenção do projeto é mapear o erro esperado para o status mais apropriado.

Exemplo conceitual:

- duplicidade de e-mail → `409 Conflict`
- usuário não encontrado → `404 Not Found`
- entrada inválida ou regra geral de negócio → `400 Bad Request`

Se surgir necessidade, o helper pode ser expandido com novos códigos sem alterar todos os controllers.

---

## 8. Exceções

### 8.1 `DomainException`

O projeto possui uma exceção de domínio:

- `common/exception/DomainException`

Ela existe para cenários de domínio que não cabem como falha esperada do `Result`.

### 8.2 Uso previsto

A documentação do próprio código indica uma diretriz importante:

- **erros esperados** → `Result`
- **falhas inesperadas / excepcionais** → exceções

### 8.3 `GlobalExceptionHandler`

A aplicação possui um handler global com `@RestControllerAdvice`.

Hoje ele trata exceções inesperadas com resposta de `500 Internal Server Error` e um corpo padronizado.

Isso mantém o sistema consistente quando algo foge do fluxo esperado.

### 8.4 Regra prática

Evite usar exceção para:

- duplicidade previsível;
- recurso ausente em fluxo normal;
- validação de negócio comum.

Use exceção para:

- algo realmente fora do comportamento esperado;
- problema técnico;
- violação de contrato interno;
- bug de implementação.

---

## 9. Javadoc e estilo de documentação

O projeto mantém uma documentação próxima do código, com Javadoc em classes, métodos e entidades relevantes.

### 9.1 Objetivo da documentação

O Javadoc aqui não é enfeite.
Ele serve para:

- explicar intenção;
- registrar decisões de design;
- mostrar limites de responsabilidade;
- reduzir ambiguidade para quem lê o código depois.

### 9.2 Tom recomendado

O tom usado no projeto é:

- direto;
- explicativo;
- sem verborragia;
- focado em comportamento e responsabilidade.

### 9.3 O que costuma ser documentado

- entidades de domínio;
- enums;
- DTOs com regras menos óbvias;
- serviços;
- jobs;
- repositórios com queries especiais;
- helpers reutilizáveis;
- configurações relevantes.

### 9.4 O que evitar

- documentação repetindo o nome do método sem agregar contexto;
- comentários excessivamente longos onde o nome já explica tudo;
- Javadoc desconectado da implementação real.

---

## 10. DTOs e validação

Os DTOs ficam no pacote `modules/user/dto` e são separados por intenção:

- `request/`
- `response/`

### 10.1 DTOs de request

São usados para entrada HTTP.

Exemplos atuais:

- `CreateUserRequest`
- `UpdateUserInfoRequest`
- `UpdateEmailRequest`
- `UpdatePasswordRequest`

### 10.2 Validação

O projeto usa Bean Validation via `@Valid` no controller e anotações como:

- `@NotBlank`
- `@Email`
- `@Size`

### 10.3 Regra prática

- validação estrutural/presença: DTO
- validação de negócio: service ou domínio

Ou seja:

- o DTO ajuda a impedir payload obviamente inválido;
- o service/domínio valida o que depende do estado da aplicação.

---

## 11. Persistência

### 11.1 `BaseEntity`

Existe uma base compartilhada para entidades JPA.

Ela concentra:

- `id`
- `createdAt`
- `lastTimeChanged`

E usa callbacks JPA:

- `@PrePersist`
- `@PreUpdate`

### 11.2 Objetivo

Evitar duplicar campos e regras de auditoria básicas em cada entidade.

### 11.3 `UserRepository`

O repositório do usuário concentra:

- consultas por e-mail;
- consultas paginadas por nome;
- consultas por status;
- operações em lote para ciclos de vida da conta.

### 11.4 Operações em lote

O projeto usa queries JPQL para operações agendadas de grande escala, como:

- mover usuários de `PENDING_DELETION` para `INACTIVE`;
- remover definitivamente usuários `INACTIVE` após retenção.

---

## 12. Ciclo de vida do usuário

O fluxo de usuário já está documentado com mais profundidade em:

- `docs/user-lifecycle-flow.md`

### Resumo atual

O usuário passa por estados como:

- `ACTIVE`
- `PENDING_DELETION`
- `INACTIVE`
- `SUSPENDED`

### Fluxo principal

- o usuário solicita desativação;
- a conta vai para `PENDING_DELETION`;
- após 30 dias, um job move para `INACTIVE`;
- após mais 7 dias, outro job remove definitivamente.

### Jobs envolvidos

- `UserDeletionJob`
- `UserHardDeletionJob`

### Regra de negócio importante

A transição de estado não deve ser tratada como simples update de banco.
Ela faz parte do domínio, por isso está refletida na entidade, no service, nos jobs e na documentação.

---

## 13. Jobs e tarefas agendadas

### 13.1 `UserDeletionJob`

Job diário às 00:00 UTC.

Responsabilidade:

- mover contas em `PENDING_DELETION` para `INACTIVE` depois da janela de 30 dias.

### 13.2 `UserHardDeletionJob`

Job diário às 00:30 UTC.

Responsabilidade:

- remover definitivamente contas inativas após 37 dias do pedido original.

### 13.3 Regra de design

Jobs devem ser curtos e previsíveis:

- calcular threshold;
- chamar repositório;
- não misturar com regra de UI ou HTTP.

---

## 14. Swagger / OpenAPI

A documentação da API é centralizada em `config/swagger/SwaggerConfig`.

### Papel

- definir metadados da API;
- preparar a documentação para crescer com tags, autenticação e ajustes futuros;
- facilitar leitura e testes de endpoints.

### Prática no controller

Os endpoints são anotados com:

- `@Operation`
- `@ApiResponses`
- `@Tag`

Isso ajuda humanos e agentes a entenderem rapidamente a intenção da rota.

---

## 15. Convenções de design já adotadas

### Faça

- mantenha regras de negócio no service ou no domínio;
- use `Result` para falhas esperadas;
- use exceção apenas para casos excepcionais;
- mantenha controllers finos;
- documente decisões que não são óbvias;
- reutilize helpers compartilhados quando houver repetição real;
- preserve o fluxo HTTP padronizado.

### Evite

- espalhar lógica de tradução de erro por vários controllers;
- lançar exceção para cada validação comum;
- misturar HTTP com regra de domínio;
- criar abstrações demais antes de haver repetição;
- deixar o domínio dependente de detalhes do controller.

---

## 16. Como o projeto pensa evolução

A preferência do projeto é por crescimento **legível** e **localmente compreensível**.

Isso significa:

- primeiro clareza;
- depois reutilização;
- abstração só quando o ganho for real.

A direção atual é:

1. manter o comportamento explícito;
2. criar reutilização apenas onde o padrão já se repetiu;
3. documentar as decisões para evitar perda de contexto no futuro.

---

## 17. Fonte complementar de contexto

Este documento deve ser lido junto com:

- `docs/user-lifecycle-flow.md`
- `docs/ResultPattern.md`

Esses arquivos aprofundam, respectivamente:

- o ciclo de vida do usuário;
- a filosofia do `Result Pattern` no projeto.

---

## 18. Resumo executivo

### O que define o projeto hoje?

- arquitetura por módulos de negócio;
- camadas claras entre controller, service, domain e repository;
- `Result Pattern` para falhas esperadas;
- exceções para cenários realmente excepcionais;
- documentação próxima do código;
- resposta HTTP padronizada;
- fluxo de usuário com lifecycle explícito;
- jobs agendados para evolução automática de status.

### Regra-mãe

> Se é esperado, trate como `Result`.
> Se é inesperado, trate como exceção.
> Se é repetição, extraia quando houver ganho real.
> Se é comportamento de domínio, documente.
# MyLifeDiary — Projeto: Arquitetura, Convenções e Contexto

> Documento de referência do design atual do backend.
>
> Objetivo duplo:
> - servir como contexto confiável para agentes de IA;
> - ser legível e útil para humanos que entram no projeto.

---

## 1. Visão geral

O projeto é um backend Spring Boot organizado por **módulos de negócio** e por **camadas técnicas**, com foco explícito
em:

- manutenção simples;
- leitura direta do fluxo;
- separação clara entre regra de negócio, HTTP e persistência;
- documentação próxima do código.

Hoje o módulo funcional principal é `user`, mas a estrutura já está preparada para crescer por novos módulos seguindo o
mesmo padrão.

---

## 2. Stack e base técnica

Pelo `pom.xml`, o projeto usa atualmente:

- **Java 25**
- **Spring Boot 4.0.5**
- **Spring Web MVC**
- **Spring Data JPA**
- **Spring Security**
- **Spring Validation**
- **Flyway**
- **OpenAPI / Swagger** com `springdoc`
- **H2** para runtime local
- **PostgreSQL** para runtime real
- **Lombok**
- **UUID Creator**

O arquivo `application.properties` está mínimo no momento, indicando que a configuração principal tende a vir de perfis,
variáveis de ambiente ou expansão futura.

---

## 3. Estilo arquitetural

### 3.1 Organização por domínio + camadas

A estrutura combina duas ideias:

1. **módulos por contexto de negócio**
2. **camadas internas em cada módulo**

Exemplo atual:

```text
com.diegoramos.mylifediary
├── common
├── config
└── modules
    └── user
        ├── controller
        ├── domain
        ├── dto
        ├── job
        ├── repository
        └── service
```

### 3.2 Responsabilidade por pacote

- `common/` → componentes reutilizáveis transversais
- `config/` → configuração da aplicação
- `modules/` → funcionalidades de negócio isoladas por módulo

Essa separação ajuda a evitar um pacote `service`, `controller` ou `repository` global demais, que normalmente cresce
sem fronteiras.

---

## 4. Estrutura de pastas

### 4.1 `common/`

Contém código compartilhado entre módulos.

Subpacotes atuais:

- `base/`
- `exception/`
- `response/`
- `result/`

#### Papel de cada um

- `base/` → entidades base e comportamentos comuns de persistência
- `exception/` → exceções de domínio e tratamento global
- `response/` → respostas HTTP padronizadas e helpers de tradução
- `result/` → padrão para representar sucesso/falha esperada

### 4.2 `config/`

Configurações do Spring e de infraestrutura.

Subpacotes atuais:

- `security/`
- `swagger/`
- `time/`

### 4.3 `modules/`

Cada módulo de negócio deve viver aqui.

Hoje existe o módulo:

- `user/`

Subpacotes:

- `controller/`
- `domain/`
- `dto/`
- `job/`
- `repository/`
- `service/`

---

## 5. Papel de cada camada

## 5.1 Controller

O controller é a borda HTTP da aplicação.

Responsabilidades:

- receber requisições;
- validar entradas com Bean Validation (`@Valid`);
- chamar o service;
- converter o resultado em resposta HTTP;
- documentar endpoints com Swagger/OpenAPI.

O controller **não** deve carregar regra de negócio complexa.

### Padrão atual

O controller usa `ResultHttpResponseHelper` para evitar repetição no tratamento de sucesso/falha.

Exemplo de intenção:

- service devolve `Result<T>`;
- controller chama o helper;
- helper converte para `ResponseEntity<?>`.

### Exemplo do que o controller NÃO deve fazer

- decidir regra de domínio;
- duplicar código de tradução de erro em vários endpoints;
- conter lógica de persistência;
- conhecer detalhes internos do agregado além do necessário.

---

## 5.2 Service

O service coordena o caso de uso.

Responsabilidades:

- orquestrar fluxo;
- consultar repositórios;
- aplicar decisões esperadas de negócio;
- devolver `Result<T>` quando a falha faz parte do fluxo normal;
- deixar exceções para casos excepcionais.

### Exemplo de uso do `Result`

No `UserService`, operações como:

- criação de usuário;
- atualização de perfil;
- mudança de e-mail;
- mudança de senha;
- solicitação de desativação;
- restauração de conta;

retornam `Result<UserResponseDTO>`.

Isso permite que o service trate situações previstas como:

- e-mail já cadastrado;
- usuário não encontrado;
- operação não permitida pelo status atual;
- tentativa de atualização vazia;
- dados inválidos de entrada no fluxo esperado.

---

## 5.3 Domain

O domínio concentra a identidade e as regras internas da entidade.

No módulo `user`, a entidade principal é `User`.

### Papel do domínio

- manter invariantes locais;
- normalizar dados;
- aplicar regras que pertencem ao próprio agregado;
- expor comportamentos como métodos de intenção clara.

### Estado atual do agregado `User`

A entidade inclui campos como:

- `fullName`
- `email`
- `passwordHash`
- `birthDate`
- `status`
- `deletionRequestedAt`

E comportamentos como:

- `create(...)`
- `requestDeletion(...)`
- `restoreAccount()`
- `changeEmail(...)`
- `changePassword(...)`
- `changeProfileInfo(...)`

---

## 6. `Result Pattern`

O projeto usa `Result` para representar **falhas esperadas** sem lançar exceção para cada regra comum.

### 6.1 Intenção

A ideia é simples:

- **resultado esperado** → `Result`
- **erro inesperado** → exceção

### 6.2 Classes envolvidas

- `Result<T>` → envelope de sucesso ou falha
- `ResultError` → descrição padronizada da falha esperada

### 6.3 Quando usar `Result`

Use `Result` para:

- validações previsíveis de negócio;
- conflitos de estado já conhecidos;
- duplicidade esperada;
- recurso não encontrado em fluxo normal;
- operação recusada por regra funcional;
- qualquer situação que faça parte do comportamento normal do sistema.

### 6.4 Quando usar exceção

Use exceção para:

- erro técnico;
- falha inesperada;
- estado impossível;
- bug;
- quebra de infraestrutura;
- qualquer coisa que não faça parte do fluxo normal.

### 6.5 API principal do `Result`

O `Result` já expõe:

- `success(...)`
- `failure(...)`
- `isSuccess()`
- `isFailure()`
- `getValue()`
- `getError()`
- `map(...)`
- `flatMap(...)`
- `fold(...)`
- `orElse(...)`
- `orElseGet(...)`
- `orElseThrow(...)`

### 6.6 `fold(...)`

O `fold(...)` converte o resultado em um valor final escolhendo entre os dois caminhos:

- sucesso;
- falha.

No projeto, ele é útil principalmente em controller/helper porque transforma `Result<T>` em uma resposta HTTP final.

### 6.7 Convenção de `code`

Os códigos de erro devem ser:

- estáveis;
- em caixa alta;
- com underscore;
- fáceis de buscar;
- fáceis de mapear para HTTP.

Exemplos já usados no projeto:

- `USER_EMAIL_ALREADY_EXISTS`
- `USER_NOT_FOUND`
- `USER_INFO_EMPTY_UPDATE`
- `USER_INVALID_FULL_NAME`
- `USER_ALREADY_ACTIVE`
- `USER_ALREADY_INACTIVE`
- `USER_SUSPENDED`
- `USER_RESTORE_NOT_ALLOWED`
- `DELETION_ALREADY_REQUESTED`

---

## 7. Resposta HTTP padronizada

### 7.1 `ApiErrorResponse`

A resposta de erro do projeto é o record `ApiErrorResponse`.

Campos atuais:

- `timestamp`
- `status`
- `error`
- `message`
- `path`
- `details`

A anotação `@JsonInclude(JsonInclude.Include.NON_EMPTY)` evita serializar campos vazios desnecessários.

### 7.2 `ResultHttpResponseHelper`

Esse helper centraliza a tradução de `Result<T>` para `ResponseEntity<?>`.

Objetivo:

- manter controllers enxutos;
- evitar repetição;
- padronizar sucesso e erro esperado.

### 7.3 Papel atual do helper

Ele faz duas coisas:

1. retorna o corpo de sucesso com o `HttpStatus` desejado;
2. converte falhas esperadas em `ApiErrorResponse` com um status mapeado.

### 7.4 Status HTTP

A intenção do projeto é mapear o erro esperado para o status mais apropriado.

Exemplo conceitual:

- duplicidade de e-mail → `409 Conflict`
- usuário não encontrado → `404 Not Found`
- entrada inválida ou regra geral de negócio → `400 Bad Request`

Se surgir necessidade, o helper pode ser expandido com novos códigos sem alterar todos os controllers.

---

## 8. Exceções

### 8.1 `DomainException`

O projeto possui uma exceção de domínio:

- `common/exception/DomainException`

Ela existe para cenários de domínio que não cabem como falha esperada do `Result`.

### 8.2 Uso previsto

A documentação do próprio código indica uma diretriz importante:

- **erros esperados** → `Result`
- **falhas inesperadas / excepcionais** → exceções

### 8.3 `GlobalExceptionHandler`

A aplicação possui um handler global com `@RestControllerAdvice`.

Hoje ele trata exceções inesperadas com resposta de `500 Internal Server Error` e um corpo padronizado.

Isso mantém o sistema consistente quando algo foge do fluxo esperado.

### 8.4 Regra prática

Evite usar exceção para:

- duplicidade previsível;
- recurso ausente em fluxo normal;
- validação de negócio comum.

Use exceção para:

- algo realmente fora do comportamento esperado;
- problema técnico;
- violação de contrato interno;
- bug de implementação.

---

## 9. Javadoc e estilo de documentação

O projeto mantém uma documentação próxima do código, com Javadoc em classes, métodos e entidades relevantes.

### 9.1 Objetivo da documentação

O Javadoc aqui não é enfeite.
Ele serve para:

- explicar intenção;
- registrar decisões de design;
- mostrar limites de responsabilidade;
- reduzir ambiguidade para quem lê o código depois.

### 9.2 Tom recomendado

O tom usado no projeto é:

- direto;
- explicativo;
- sem verborragia;
- focado em comportamento e responsabilidade.

### 9.3 O que costuma ser documentado

- entidades de domínio;
- enums;
- DTOs com regras menos óbvias;
- serviços;
- jobs;
- repositórios com queries especiais;
- helpers reutilizáveis;
- configurações relevantes.

### 9.4 O que evitar

- documentação repetindo o nome do método sem agregar contexto;
- comentários excessivamente longos onde o nome já explica tudo;
- Javadoc desconectado da implementação real.

---

## 10. DTOs e validação

Os DTOs ficam no pacote `modules/user/dto` e são separados por intenção:

- `request/`
- `response/`

### 10.1 DTOs de request

São usados para entrada HTTP.

Exemplos atuais:

- `CreateUserRequest`
- `UpdateUserInfoRequest`
- `UpdateEmailRequest`
- `UpdatePasswordRequest`

### 10.2 Validação

O projeto usa Bean Validation via `@Valid` no controller e anotações como:

- `@NotBlank`
- `@Email`
- `@Size`

### 10.3 Regra prática

- validação estrutural/presença: DTO
- validação de negócio: service ou domínio

Ou seja:

- o DTO ajuda a impedir payload obviamente inválido;
- o service/domínio valida o que depende do estado da aplicação.

---

## 11. Persistência

### 11.1 `BaseEntity`

Existe uma base compartilhada para entidades JPA.

Ela concentra:

- `id`
- `createdAt`
- `lastTimeChanged`

E usa callbacks JPA:

- `@PrePersist`
- `@PreUpdate`

### 11.2 Objetivo

Evitar duplicar campos e regras de auditoria básicas em cada entidade.

### 11.3 `UserRepository`

O repositório do usuário concentra:

- consultas por e-mail;
- consultas paginadas por nome;
- consultas por status;
- operações em lote para ciclos de vida da conta.

### 11.4 Operações em lote

O projeto usa queries JPQL para operações agendadas de grande escala, como:

- mover usuários de `PENDING_DELETION` para `INACTIVE`;
- remover definitivamente usuários `INACTIVE` após retenção.

---

## 12. Ciclo de vida do usuário

O fluxo de usuário já está documentado com mais profundidade em:

- `docs/user-lifecycle-flow.md`

### Resumo atual

O usuário passa por estados como:

- `ACTIVE`
- `PENDING_DELETION`
- `INACTIVE`
- `SUSPENDED`

### Fluxo principal

- o usuário solicita desativação;
- a conta vai para `PENDING_DELETION`;
- após 30 dias, um job move para `INACTIVE`;
- após mais 7 dias, outro job remove definitivamente.

### Jobs envolvidos

- `UserDeletionJob`
- `UserHardDeletionJob`

### Regra de negócio importante

A transição de estado não deve ser tratada como simples update de banco.
Ela faz parte do domínio, por isso está refletida na entidade, no service, nos jobs e na documentação.

---

## 13. Jobs e tarefas agendadas

### 13.1 `UserDeletionJob`

Job diário às 00:00 UTC.

Responsabilidade:

- mover contas em `PENDING_DELETION` para `INACTIVE` depois da janela de 30 dias.

### 13.2 `UserHardDeletionJob`

Job diário às 00:30 UTC.

Responsabilidade:

- remover definitivamente contas inativas após 37 dias do pedido original.

### 13.3 Regra de design

Jobs devem ser curtos e previsíveis:

- calcular threshold;
- chamar repositório;
- não misturar com regra de UI ou HTTP.

---

## 14. Swagger / OpenAPI

A documentação da API é centralizada em `config/swagger/SwaggerConfig`.

### Papel

- definir metadados da API;
- preparar a documentação para crescer com tags, autenticação e ajustes futuros;
- facilitar leitura e testes de endpoints.

### Prática no controller

Os endpoints são anotados com:

- `@Operation`
- `@ApiResponses`
- `@Tag`

Isso ajuda humanos e agentes a entenderem rapidamente a intenção da rota.

---

## 15. Convenções de design já adotadas

### Faça

- mantenha regras de negócio no service ou no domínio;
- use `Result` para falhas esperadas;
- use exceção apenas para casos excepcionais;
- mantenha controllers finos;
- documente decisões que não são óbvias;
- reutilize helpers compartilhados quando houver repetição real;
- preserve o fluxo HTTP padronizado.

### Evite

- espalhar lógica de tradução de erro por vários controllers;
- lançar exceção para cada validação comum;
- misturar HTTP com regra de domínio;
- criar abstrações demais antes de haver repetição;
- deixar o domínio dependente de detalhes do controller.

---

## 16. Como o projeto pensa evolução

A preferência do projeto é por crescimento **legível** e **localmente compreensível**.

Isso significa:

- primeiro clareza;
- depois reutilização;
- abstração só quando o ganho for real.

A direção atual é:

1. manter o comportamento explícito;
2. criar reutilização apenas onde o padrão já se repetiu;
3. documentar as decisões para evitar perda de contexto no futuro.

---

## 17. Fonte complementar de contexto

Este documento deve ser lido junto com:

- `docs/user-lifecycle-flow.md`
- `docs/ResultPattern.md`

Esses arquivos aprofundam, respectivamente:

- o ciclo de vida do usuário;
- a filosofia do `Result Pattern` no projeto.

---

## 18. Resumo executivo

### O que define o projeto hoje?

- arquitetura por módulos de negócio;
- camadas claras entre controller, service, domain e repository;
- `Result Pattern` para falhas esperadas;
- exceções para cenários realmente excepcionais;
- documentação próxima do código;
- resposta HTTP padronizada;
- fluxo de usuário com lifecycle explícito;
- jobs agendados para evolução automática de status.

### Regra-mãe

> Se é esperado, trate como `Result`.
> Se é inesperado, trate como exceção.
> Se é repetição, extraia quando houver ganho real.
> Se é comportamento de domínio, documente.


