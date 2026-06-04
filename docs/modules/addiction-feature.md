# Módulo Addictions — Visão geral da funcionalidade

Este documento descreve o comportamento implementado no módulo `addiction` do backend MyLifeDiary. A explicação foi extraída diretamente do código-fonte para mostrar contratos HTTP, regras de domínio, DTOs, normalizações e códigos de erro usados pelo serviço.

## Objetivo

Permitir que um usuário registre dependências/comportamentos que deseja evitar (ex.: cigarro, bebida), registre logs diários indicando recaída ou dia de sobriedade, e consulte métricas como a streak (dias consecutivos sem recaída).

## Principais elementos do domínio

- `Addiction` (entidade)
  - Campos principais: `user` (dono), `title` (obrigatório), `description` (opcional), `addictionCategory`, `startDate`.
  - Validações de criação são feitas via `DomainValidation` dentro de `Addiction.create(...)` (usuário, título, categoria e startDate são obrigatórios).
  - `title` é normalizado com `TextNormalizer.name(...)` (trim + reduzir espaços consecutivos).
  - `description` é normalizada localmente: trim + blank → null (assim `null` representa “sem descrição”).

- `AddictionLog` (registro diário)
  - Campos: `addiction` (referência), `date` (LocalDate), `relapsed` (boolean), `note` (opcional, max 500 chars).
  - Existe uma `UniqueConstraint(addiction_id, date)` para garantir no nível do banco que só haja um log por dia por dependência.
  - `note` é normalizada localmente (trim + blank → null).
  - `create(...)` e `mark(...)` aplicam validações de domínio e atualizam `lastTimeChanged` quando apropriado.

## Contratos HTTP (Controller)

Base path: `/addictions` (controlador protegido por JWT). As rotas principais:

- POST /addictions/users/{userId}
  - Cria uma nova `Addiction` para o `userId` informado.
  - Body: `CreateAddictionRequest` (title: obrigatório, description, category, startDate obrigatório).
  - Respostas: 201 criado / 400 validação / 404 usuário não encontrado

- PUT /addictions/{addictionId}/logs/users/{userId}
  - Registra ou atualiza o log diário de uma dependência.
  - Body: `RegisterAddictionLogRequest` (date obrigatório, relapsed obrigatório, note opcional até 500 chars).
  - Regras importantes: não permite registrar logs com `date` anterior ao `startDate` da dependência.
  - Se já existir log para a data, o serviço atualiza o registro existente (com `mark(...)`);
  - Respostas: 200 OK / 400 validação / 404 dependência não encontrada

- GET /addictions/{addictionId}/logs/users/{userId}?fromDate=&toDate=
  - Lista logs do intervalo solicitado (ou todo o histórico se nenhum filtro fornecido).
  - Valida: intervalo de datas (from <= to).

- GET /addictions/{addictionId}/sobriety/users/{userId}
  - Calcula a streak atual de sobriedade: começa em hoje e conta dias consecutivos para trás enquanto houver log para a data e `relapsed == false`.
  - Se não houver logs, retorna streak = 0.

- GET /addictions
  - Lista addictions do usuário autenticado (paginação). Extrai o email do JWT e busca o usuário por email.

## Service — regras e comportamento

- Padrão `Result<T>` para falhas esperadas (códigos e mensagens ligadas ao domínio), e exceções para falhas técnicas.
- `createAddiction(...)`:
  - Verifica existência do usuário; converte DomainException em `Result.failure("ADDICTION_INVALID_INPUT", ...)`.
  - Salva a entidade e retorna `AddictionResponseDTO`.

- `registerAddictionLog(...)`:
  - Verifica existência da dependência associada ao `userId`.
  - Rejeita logs com data anterior ao `startDate` (`ADDICTION_LOG_BEFORE_START_DATE`).
  - Se já existir log para (addictionId, date) atualiza; caso contrário cria novo registro.

- `getAddictionLogs(...)`:
  - Suporta filtros `fromDate` e `toDate` (ambos opcionais) e valida ranges.

- `getCurrentSobrietyStreak(...)`:
  - Busca logs ordenados desc; monta mapa por data e itera a partir de `LocalDate.now(clock)` contando dias consecutivos não-relapsos. Para quando não existir log no dia ou o log indicar recaída.

- `findAll(...)` / `findAllByEmail(...)`:
  - Paginação por `page` e `size`, retorna `Page<AddictionResponseDTO>`.

## Códigos de erro usados (exemplos)

- `ADDICTION_USER_NOT_FOUND` — quando usuário não existe.
- `ADDICTION_NOT_FOUND` — quando uma dependência específica não é encontrada (pelo par addictionId/userId).
- `ADDICTION_INVALID_INPUT` — quando uma validação de domínio falha (mensagem descreve o motivo).
- `ADDICTION_LOG_BEFORE_START_DATE` — tentativa de registrar log anterior à data de início.
- `ADDICTION_LOG_INVALID_RANGE` — intervalo de datas inválido ao listar logs.

## Observações de implementação / pontos de atenção

- Normalização: o módulo aplica trim e converte strings vazias/em branco para `null` em pontos locais (`description` e `note`) — isto reduz ruído no banco e facilita consultas por "ausência de valor" (IS NULL).
- Unicidade de logs por (addiction_id, date) é garantida no nível do banco (unique constraint) e tratada no serviço via `findByAddictionIdAndDate(...)` para decidir entre criar ou atualizar.
- A lógica de streak depende de logs explicitamente presentes. Dias sem registro interrompem a contagem (a implementação atual considera ausência de log como quebra da streak).
  - Isso é uma decisão de produto: se preferir interpretar ausência como "não houve recaída", a lógica deveria ser ajustada.

## Exemplos de payloads

- CreateAddictionRequest (POST /addictions/users/{userId}):

```json
{
  "title": "Cigarro",
  "description": "Quero parar de fumar aos poucos",
  "category": "SUBSTANCE",
  "startDate": "2026-06-01"
}
```

- RegisterAddictionLogRequest (PUT /addictions/{id}/logs/users/{userId}):

```json
{
  "date": "2026-06-04",
  "relapsed": false,
  "note": "Dia tranquilo, exercícios ajudaram"
}
```

## Sugestões / próximas melhorias

- Documentar explicitamente a política sobre dias sem registro e sua interpretação para a streak.
- Se desejar consistência cross-module, extrair a rotina `trim + blank -> null` para um helper reutilizável (atualmente cada entidade mantém sua própria normalização local).
- Adicionar testes de integração cobrindo o cálculo de streak em cenários com dias ausentes e com logs antigos.

---

Arquivo gerado automaticamente a partir da análise do código fonte em `modules/addiction`. Se precisar que eu gere diagramas de sequência (fluxo de criação, fluxo de registro de log, ou cálculo de streak), eu posso adicionar como imagens ou diagramas ASCII.

