# k6 — Testes de carga e fluxo autenticado

Este documento descreve o uso do `k6` no projeto MyLifeDiary e explica o que cada script de teste faz.

## O que é o k6

O `k6` é uma ferramenta de teste de carga e performance voltada para APIs e sistemas web. Ele permite simular múltiplos usuários virtuais, medir tempos de resposta, taxa de erro e comportamento do sistema sob pressão.

No MyLifeDiary, o `k6` é usado para validar principalmente:

- criação de usuários;
- autenticação com JWT;
- uso de endpoints protegidos com `Authorization: Bearer ...`;
- comportamento da API sob carga crescente.

## Estrutura atual dos scripts

Todos os scripts ficam dentro da pasta `k6/` para não se misturarem com os arquivos do backend Java.

```text
k6/
├── package.json
├── package-lock.json
└── tests/
    ├── register.js
    └── authenticated-habit.js
```

## Visão geral dos scripts

### 1. `k6/tests/register.js`

Este é o script mais simples e faz apenas o teste de registro de usuário.

#### O que ele faz

1. Gera dados únicos para cada execução usando `__VU` e `__ITER`.
2. Envia `POST /users/register`.
3. Valida se a resposta foi `201 Created`.
4. Verifica se o tempo da requisição ficou abaixo de `500ms` no `check` local do script.
5. Aplica também thresholds globais:
   - `p(95) < 2000ms` para `http_req_duration`;
   - `rate < 0.01` para `http_req_failed`.

#### Objetivo do teste

Esse script serve para medir a saúde do endpoint de cadastro e a capacidade do backend em processar novos usuários sob carga.

#### Endpoint usado

- `POST /users/register`

#### Quando usar

Use esse script quando quiser testar apenas a etapa de criação de usuários, sem autenticação.

---

### 2. `k6/tests/authenticated-habit.js`

Este script faz um fluxo mais completo, cobrindo registro, login e uma operação autenticada.

#### O que ele faz

1. Registra um novo usuário em `POST /users/register`.
2. Faz login em `POST /auth/login`.
3. Extrai o `accessToken` retornado pelo login.
4. Usa o token no header `Authorization: Bearer <accessToken>`.
5. Cria um hábito autenticado em `POST /habits/users/{userId}`.
6. Valida os retornos esperados em cada etapa.

#### Objetivo do teste

Esse script existe para validar o fluxo real de uso da API:

- o usuário é criado;
- o usuário consegue autenticar;
- um endpoint protegido aceita o JWT;
- o backend consegue manter o fluxo completo funcionando sob carga.

#### Endpoints usados

- `POST /users/register`
- `POST /auth/login`
- `POST /habits/users/{userId}`

#### Observações importantes

- O script tenta ler o `id` retornado no registro para usar na criação do hábito.
- Ele permite configurar a URL base via variável de ambiente `K6_BASE_URL`.
- Se `K6_BASE_URL` não for informada, o script usa `http://localhost:8080`.

Exemplo:

```powershell
$env:K6_BASE_URL = "http://localhost:8080"
```

## Fluxo coberto pelos testes

### Teste de cadastro

O `register.js` cobre somente o fluxo de criação de usuário:

- entrada válida;
- resposta HTTP correta;
- tempo aceitável de resposta;
- comportamento do sistema sob múltiplos usuários simultâneos.

### Teste autenticado

O `authenticated-habit.js` cobre um fluxo mais próximo do uso real:

1. o usuário se registra;
2. o usuário faz login;
3. o JWT é emitido;
4. o JWT é usado em uma rota protegida;
5. o backend processa uma operação autenticada.

## Métricas e thresholds

Os scripts usam dois níveis de verificação:

### `check(...)`

Valida o comportamento de cada requisição individualmente.

Exemplos:

- status HTTP esperado;
- presença do `accessToken` no login;
- tempo de resposta local em um teste específico.

### `thresholds`

Definem limites globais para a execução inteira.

No projeto atual:

- `http_req_duration: p(95) < 2000ms`
- `http_req_failed: rate < 0.01`

Esses limites ajudam a identificar degradação de performance e aumento de falhas.

## Como executar

Execute os scripts a partir da pasta `k6/`.

### Registro puro

```powershell
Set-Location C:\Users\Windows\IdeaProjects\mylifediary\k6
k6 run .\tests\register.js
```

### Fluxo autenticado

```powershell
Set-Location C:\Users\Windows\IdeaProjects\mylifediary\k6
k6 run .\tests\authenticated-habit.js
```

### Com URL base personalizada

```powershell
$env:K6_BASE_URL = "http://localhost:8080"
Set-Location C:\Users\Windows\IdeaProjects\mylifediary\k6
k6 run .\tests\authenticated-habit.js
```

## Boas práticas

- Rode os testes em ambiente local ou de homologação.
- Use dados únicos por execução para evitar colisão de usuários.
- Não rode esses cenários em produção sem um plano de observabilidade e controle.
- Se quiser ampliar o teste, adicione scripts específicos para:
  - refresh token;
  - criação de hábitos em maior volume;
  - operações de leitura autenticada;
  - fluxo de journaling.

## Resumo

- `register.js` mede o cadastro de usuários sob carga.
- `authenticated-habit.js` valida o fluxo completo de autenticação + endpoint protegido.
- Os dois scripts ficam isolados em `k6/` para manter o projeto organizado.

