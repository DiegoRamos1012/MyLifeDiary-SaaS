# 📘 TheLifeDiary — Arquitetura de Pastas (Backend)

## 🧠 Visão Geral

O backend do **TheLifeDiary** segue o modelo:

> **Monólito Modular (Modular Monolith) com organização Package-by-Feature**

Isso significa que o sistema é:

- um único projeto (monólito)
- dividido em **módulos independentes por domínio**
- organizado para ser **simples, escalável e de fácil manutenção**

---

## 🎯 Princípios da Arquitetura

- 📦 Organização por **domínio (`modules`)**
- 🔒 **Isolamento entre módulos**
- 🧠 Regras de negócio centralizadas em `service`
- ⚙️ Infraestrutura separada em `config`
- 🔁 Uso de **Result Pattern** para fluxos esperados
- ❌ Sem overengineering

---

## 🌳 Estrutura de Diretórios (com responsabilidades)

```plaintext
src/main/java/com/thelifediary/

├── config/                          # Configurações globais do sistema (infraestrutura)
│   ├── security/                    # Configuração de autenticação/autorização (Spring Security)
│   ├── jwt/                         # Configuração de geração e validação de tokens JWT
│   ├── stripe/                      # Integração e configuração do Stripe (pagamentos)
│   └── swagger/
│       └── SwaggerConfig.java       # Configuração da documentação da API (OpenAPI/Swagger)

├── common/                          # Componentes reutilizáveis e genéricos (sem domínio)
│   ├── exception/                   # Exceções globais da aplicação
│   ├── response/                    # Padronização de respostas HTTP
│   ├── util/                        # Utilitários técnicos (ex: datas, strings, criptografia)
│   │                                 # ⚠️ NÃO contém regra de negócio
│   └── result/
│       └── Result.java              # Implementação do Result Pattern (sucesso/falha)

├── modules/                         # Módulos do sistema (organização por domínio)

│   ├── user/                        # Módulo de usuários
│   │   ├── controller/              # Entrada HTTP (REST)
│   │   ├── service/                 # Regras de negócio do usuário
│   │   ├── repository/              # Acesso ao banco (User)
│   │   ├── domain/
│   │   │   ├── entity/              # Entidades (User)
│   │   │   └── enums/               # Enumerações relacionadas
│   │   └── dto/
│   │       ├── request/             # DTOs de entrada
│   │       └── response/            # DTOs de saída

│   ├── habit/                       # Módulo de hábitos
│   │   ├── controller/
│   │   ├── service/                 # Regras de hábitos (criação, conclusão, etc)
│   │   ├── repository/
│   │   ├── domain/
│   │   │   ├── entity/              # Habit, HabitLog
│   │   │   └── enums/
│   │   └── dto/
│   │       ├── request/
│   │       └── response/

│   ├── addiction/                   # Módulo de vícios
│   │   ├── controller/
│   │   ├── service/                 # Regras de recaída, urgência, resistência
│   │   ├── repository/
│   │   ├── domain/
│   │   │   ├── entity/              # Addiction, AddictionLog
│   │   │   └── enums/
│   │   └── dto/
│   │       ├── request/
│   │       └── response/

│   ├── journal/                     # Módulo de diário emocional
│   │   ├── controller/
│   │   ├── service/                 # Regras de diário (criação, bloqueio, etc)
│   │   ├── repository/
│   │   ├── domain/
│   │   │   ├── entity/              # Journal, JournalEntry
│   │   │   └── enums/
│   │   └── dto/
│   │       ├── request/
│   │       └── response/

│   ├── subscription/                # Módulo de assinatura
│   │   ├── controller/
│   │   ├── service/                 # Regras de plano, status, trial
│   │   ├── repository/
│   │   ├── domain/
│   │   │   ├── entity/              # Subscription
│   │   │   └── enums/
│   │   └── dto/
│   │       ├── request/
│   │       └── response/

│   └── payment/                     # Módulo de pagamentos
│       ├── controller/
│       ├── service/                 # Processamento de pagamentos
│       ├── repository/
│       ├── domain/
│       │   ├── entity/              # Payment
│       │   └── enums/
│       └── dto/
│           ├── request/
│           └── response/
```
