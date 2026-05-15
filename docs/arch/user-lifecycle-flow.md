# Fluxo de Ciclo de Vida do Usuário — Desativação e Hard Delete

## Visão Geral

O sistema possui quatro possíveis status para um usuário:

| Status            | Descrição                                                     |
|-------------------|---------------------------------------------------------------|
| `ACTIVE`          | Usuário ativo com acesso normal ao sistema                    |
| `PENDING_DELETION`| Usuário solicitou exclusão da conta; aguardando período de carência |
| `INACTIVE`        | Conta desativada; aguardando remoção definitiva              |
| `SUSPENDED`       | Conta bloqueada por restrição administrativa                  |

---

## Fluxo de Desativação (Soft Delete)

### 1. Solicitação pelo usuário

**Endpoint:** `PATCH /users/{userId}/deactivate`  
**Controller:** `UserController#deactivateUser`  
**Service:** `UserService#deleteUser`

O usuário (ou administrador) realiza uma chamada ao endpoint de desativação. O serviço verifica o status atual e aplica as seguintes regras:

| Status atual       | Resultado                                                   |
|--------------------|-------------------------------------------------------------|
| `ACTIVE`           | ✅ Transita para `PENDING_DELETION`, registra `deletionRequestedAt` |
| `PENDING_DELETION` | ❌ Falha: `DELETION_ALREADY_REQUESTED`                      |
| `INACTIVE`         | ❌ Falha: `USER_ALREADY_INACTIVE`                           |
| `SUSPENDED`        | ❌ Falha: `USER_SUSPENDED`                                  |

**Método de domínio chamado:** `User#requestDeletion(Instant now)`

```
user.status          = PENDING_DELETION
user.deletionRequestedAt = <instante atual>
```

A resposta é `202 Accepted` em caso de sucesso.

---

### 2. Possibilidade de restauração

**Endpoint:** `PATCH /users/{userId}/reactivate`  
**Controller:** `UserController#reactivateUser`  
**Service:** `UserService#restoreUser`

Enquanto o usuário está em `PENDING_DELETION`, é possível cancelar a solicitação de exclusão:

| Status atual       | Resultado                                                   |
|--------------------|-------------------------------------------------------------|
| `PENDING_DELETION` | ✅ Transita de volta para `ACTIVE`, limpa `deletionRequestedAt` |
| `ACTIVE`           | ❌ Falha: `USER_ALREADY_ACTIVE`                             |
| `SUSPENDED`        | ❌ Falha: `USER_SUSPENDED`                                  |
| `INACTIVE`         | ❌ Falha: `USER_RESTORE_NOT_ALLOWED`                        |

**Método de domínio chamado:** `User#restoreAccount()`

```
user.status              = ACTIVE
user.deletionRequestedAt = null
```

---

## Fluxo Automatizado — Jobs de Ciclo de Vida

### Job 1: `UserDeletionJob` — PENDING_DELETION → INACTIVE

**Execução:** todo dia às `00:00 UTC` (cron: `0 0 0 * * *`)  
**Classe:** `UserDeletionJob#processPendingDeletionUsers`

Após **30 dias** contados a partir de `deletionRequestedAt`, o job move automaticamente os usuários em `PENDING_DELETION` para `INACTIVE`.

**Query executada (bulk update via JPQL):**
```sql
UPDATE User u
   SET u.status = INACTIVE
 WHERE u.status = PENDING_DELETION
   AND u.deletionRequestedAt IS NOT NULL
   AND u.deletionRequestedAt < :threshold
```

> `threshold = now() - 30 dias`

A partir desse ponto, a conta não pode mais ser restaurada pelo usuário.

---

### Job 2: `UserHardDeletionJob` — INACTIVE → removido definitivamente

**Execução:** todo dia às `00:30 UTC` (cron: `0 30 0 * * *`)  
**Classe:** `UserHardDeletionJob#hardDeleteInactiveUsers`

Após **37 dias** contados a partir de `deletionRequestedAt` (30 dias de carência + 7 dias de retenção em `INACTIVE`), o job remove definitivamente o registro do banco de dados.

**Constantes usadas:**
```java
PENDING_DELETION_GRACE_DAYS = 30
INACTIVE_RETENTION_DAYS     = 7
// totalRetentionDays        = 37
```

**Query executada (bulk delete via JPQL):**
```sql
DELETE FROM User u
 WHERE u.status = INACTIVE
   AND u.deletionRequestedAt IS NOT NULL
   AND u.deletionRequestedAt < :threshold
```

> `threshold = now() - 37 dias`

---

## Diagrama de Estados

```
                  ┌─────────────────────┐
                  │        ACTIVE        │
                  └────────┬────────────┘
                           │
               PATCH /deactivate
                           │
                  ┌────────▼────────────┐
                  │   PENDING_DELETION   │◄──── PATCH /reactivate (restaura para ACTIVE)
                  └────────┬────────────┘
                           │
                  após 30 dias (UserDeletionJob — 00:00 UTC)
                           │
                  ┌────────▼────────────┐
                  │       INACTIVE       │
                  └────────┬────────────┘
                           │
                  após 37 dias desde deletionRequestedAt
                  (UserHardDeletionJob — 00:30 UTC)
                           │
                  ┌────────▼────────────┐
                  │  REMOVIDO DO BANCO  │
                  └─────────────────────┘
```

---

## Linha do Tempo Resumida

```
Dia  0  → Usuário solicita desativação (status: PENDING_DELETION)
           └─ Ainda pode restaurar a conta via /reactivate

Dia 30  → UserDeletionJob executa (00:00 UTC)
           └─ status muda para INACTIVE
           └─ Conta não pode mais ser restaurada

Dia 37  → UserHardDeletionJob executa (00:30 UTC)
           └─ Registro removido definitivamente do banco de dados
```

---

## Camadas Envolvidas

| Camada       | Classe/Interface                  | Responsabilidade                                              |
|--------------|-----------------------------------|---------------------------------------------------------------|
| Controller   | `UserController`                  | Recebe requisições HTTP e delega ao serviço                  |
| Service      | `UserService`                     | Orquestra regras de negócio e persiste via repositório       |
| Domain       | `User`                            | Mantém invariantes e executa transições de estado            |
| Repository   | `UserRepository`                  | Consultas e operações em lote (bulk update/delete)           |
| Jobs         | `UserDeletionJob`                 | Transição agendada: `PENDING_DELETION` → `INACTIVE`          |
|              | `UserHardDeletionJob`             | Remoção agendada: `INACTIVE` → hard delete                   |
