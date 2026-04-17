# `Result Pattern` no TheLifeDiary

## Visão geral

O `Result Pattern` é uma forma de representar o **resultado esperado** de uma operação sem usar exceções para regras de
negócio comuns.

No projeto **TheLifeDiary**, a ideia é simples:

- **resultado esperado** → retorna `Result`
- **erro inesperado** → lança exceção e deixa o `GlobalExceptionHandler` tratar

Isso deixa o código:

- mais previsível
- mais fácil de ler
- mais simples de manter
- mais adequado para fluxo de domínio

---

## Arquivos principais

O padrão está em `src/main/java/com/diegoramos/mylifediary/common/result/`:

- `Result<T>` → representa sucesso ou falha
- `ResultError` → representa o erro esperado

---

## Como o `Result` funciona

### Sucesso

Quando a operação dá certo, você retorna:

```java
return Result.success(valor);
```

### Falha esperada

Quando a operação falha por uma regra de negócio conhecida, você retorna:

```java
return Result.failure("CODE","Mensagem amigável");
```

ou

```java
return Result.failure("CODE","Mensagem amigável",List.of("detalhe 1", "detalhe 2"));
```

### Leitura do resultado

Você pode verificar:

```java
result.isSuccess();
result.

isFailure();
```

E acessar os dados com:

```java
result.getValue();
result.

getError();
```

---

## Regras de uso no projeto

### Use `Result` para

- regra de negócio esperada
- validação de domínio conhecida
- recurso não encontrado em fluxo normal
- operação recusada por regra funcional
- duplicidade já prevista
- qualquer situação que faça parte do comportamento esperado do sistema

### Use exceções para

- erro de infraestrutura
- falha inesperada de código
- problema de banco de dados
- bug
- situação que não faz parte do fluxo normal

---

## Estrutura do `Result`

### `Result<T>`

Representa o estado final da operação.

- `success(...)` → cria sucesso
- `failure(...)` → cria falha
- `map(...)` → transforma o valor de sucesso
- `flatMap(...)` → encadeia outro `Result`
- `fold(...)` → converte sucesso/falha em um único retorno
- `orElse(...)` → retorna valor padrão se falhar
- `orElseGet(...)` → gera valor padrão sob demanda
- `orElseThrow(...)` → converte a falha em exceção, se necessário

### `ResultError`

Contém:

- `code` → código estável e fácil de tratar
- `message` → mensagem legível
- `details` → lista opcional de detalhes

### Dica rápida de decisão

Use a regra abaixo para não complicar o service:

- **`Result.success(...)`** → quando a operação terminou com sucesso.
- **`Result.failure("CODE", "mensagem")`** → quando a falha é simples e você só precisa de `code` + `message`.
- **`Result.failure(ResultError.of(...))`** → quando quiser montar o erro antes de retornar ou incluir `details`.

Em outras palavras: `Result` é o envelope do retorno, e `ResultError` é o conteúdo da falha esperada.

---

## Como aplicar nos seus services

A ideia principal é: **o service decide o resultado do negócio**, e o controller apenas converte isso em HTTP.

### Exemplo simples

```java
public Result<HabitResponse> createHabit(CreateHabitRequest request) {
    if (habitRepository.existsByNameAndUserId(request.name(), request.userId())) {
        return Result.failure(
                "HABIT_ALREADY_EXISTS",
                "Já existe um hábito com esse nome para este usuário"
        );
    }

    Habit habit = new Habit(request.name(), request.userId());
    Habit savedHabit = habitRepository.save(habit);

    return Result.success(new HabitResponse(savedHabit.getId(), savedHabit.getName()));
}
```

---

## Exemplo com múltiplas etapas

Você pode encadear operações com `flatMap`:

```java
public Result<JournalEntryResponse> createEntry(CreateJournalEntryRequest request) {
    return validateDiaryOpen(request.userId())
            .flatMap(ignored -> saveEntry(request));
}

private Result<Void> validateDiaryOpen(Long userId) {
    if (journalRepository.isLockedForUser(userId)) {
        return Result.failure("JOURNAL_LOCKED", "O diário está bloqueado no momento");
    }

    return Result.success(null);
}

private Result<JournalEntryResponse> saveEntry(CreateJournalEntryRequest request) {
    JournalEntry entry = journalRepository.save(new JournalEntry(request.userId(), request.text()));
    return Result.success(new JournalEntryResponse(entry.getId()));
}
```

---

## Exemplo com transformação

Use `map` quando você quer mudar o tipo do valor sem criar outro fluxo de negócio:

```java
public Result<String> getHabitName(Long habitId) {
    return habitRepository.findById(habitId)
            .map(Habit::getName)
            .map(Result::success)
            .orElseGet(() -> Result.failure("HABIT_NOT_FOUND", "Hábito não encontrado"));
}
```

> Observação: quando o fluxo envolver busca que pode retornar `null` ou `Optional`, normalmente é mais claro resolver
> isso no próprio service e devolver `Result.failure(...)`.

---

## Como pensar no fluxo do service

Um service com `Result` normalmente segue este raciocínio:

1. valida entrada
2. verifica regras de negócio
3. executa a ação
4. devolve `Result.success(...)` ou `Result.failure(...)`

### Exemplo de lógica

```java
public Result<SubscriptionResponse> activateTrial(Long userId) {
    if (subscriptionRepository.hasActiveSubscription(userId)) {
        return Result.failure("SUBSCRIPTION_ALREADY_ACTIVE", "Usuário já possui assinatura ativa");
    }

    if (subscriptionRepository.hasUsedTrial(userId)) {
        return Result.failure("TRIAL_ALREADY_USED", "O período de teste já foi utilizado");
    }

    Subscription subscription = subscriptionRepository.save(Subscription.startTrial(userId));
    return Result.success(new SubscriptionResponse(subscription.getId(), subscription.getStatus()));
}
```

---

## Como aplicar no controller

O controller recebe o `Result` e decide a resposta HTTP.

### Exemplo

```java

@PostMapping
public ResponseEntity<?> create(@RequestBody CreateHabitRequest request) {
    Result<HabitResponse> result = habitService.createHabit(request);

    return result.fold(
            success -> ResponseEntity.status(HttpStatus.CREATED).body(success),
            error -> ResponseEntity.badRequest().body(error)
    );
}
```

### Regra prática

- `200/201` para `Result.success(...)`
- `400/409/404/...` para `Result.failure(...)`
- `500` para exceções inesperadas

---

## Como decidir entre `Result` e exceção

### Use `Result` quando

- a falha é previsível
- a falha faz parte da regra de negócio
- o sistema pode continuar normalmente
- você quer controlar a resposta sem quebrar o fluxo

### Use exceção quando

- o sistema entrou num estado inválido
- houve falha técnica
- a operação não deveria falhar daquele jeito
- você quer que o `GlobalExceptionHandler` trate o problema

---

## Padrão recomendado para os módulos

### `habit`

- regra de conclusão de hábito
- hábito já concluído hoje
- hábito bloqueado

### `addiction`

- recaída
- falha de resistência
- meta não alcançada

### `journal`

- diário bloqueado
- entrada inválida
- limite diário atingido

### `subscription`

- trial já usado
- assinatura ativa já existente
- plano indisponível

### `payment`

- pagamento recusado
- transação pendente
- gateway indisponível

---

## Convenção para `code`

Mantenha os códigos:

- em caixa alta
- com underscore
- estáveis
- fáceis de buscar

### Exemplos

- `HABIT_ALREADY_EXISTS`
- `JOURNAL_LOCKED`
- `SUBSCRIPTION_ALREADY_ACTIVE`
- `PAYMENT_REJECTED`
- `TRIAL_ALREADY_USED`

---

## Boas práticas

### Faça

- retorne `Result` em services
- mantenha mensagens claras
- use códigos consistentes
- use `flatMap` para encadear regras
- use `fold` para converter o resultado em resposta

### Evite

- lançar exceção para regra comum
- misturar regra de negócio com HTTP no service
- criar muitos códigos diferentes para o mesmo problema
- usar `Result` para falhas técnicas inesperadas

---

## Fluxo recomendado no projeto

```text
Controller -> Service -> Result
                         -> sucesso: responde normalmente
                         -> falha esperada: responde com status controlado
                         -> erro inesperado: lança exceção
                                         -> GlobalExceptionHandler
```

---

## Exemplo final completo

```java
public Result<HabitResponse> completeHabit(Long habitId, Long userId) {
    Habit habit = habitRepository.findByIdAndUserId(habitId, userId)
            .orElse(null);

    if (habit == null) {
        return Result.failure("HABIT_NOT_FOUND", "Hábito não encontrado");
    }

    if (habit.isCompletedToday()) {
        return Result.failure("HABIT_ALREADY_COMPLETED", "Este hábito já foi concluído hoje");
    }

    habit.markAsCompleted();
    Habit updated = habitRepository.save(habit);

    return Result.success(new HabitResponse(updated.getId(), updated.getName()));
}
```

---

## Resumo

Se for **esperado**, use `Result`.
Se for **inesperado**, use exceção.

Esse é o princípio que vai deixar o backend mais claro, escalável e fácil de manter.

