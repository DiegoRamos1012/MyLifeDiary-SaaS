# 🗄️ Configurações do H2 - MyLifeDiary

## Resumo

Seu projeto usa **H2 Database** sem configurações explícitas, o que significa que está usando as configurações padrão do Spring Boot.

---

## 📋 Configurações Atuais (Verificadas em Runtime)

| Configuração | Valor Atual | Descrição |
|---|---|---|
| **Banco de Dados** | Em memória (dinâmico por teste) | `jdbc:h2:mem:83999ac8-60b3...` (UUID gerado por teste) |
| **Vers ão do H2** | 2.4.240 | Driver H2 JDBC |
| **User (username)** | `SA` | Usuário padrão do H2 |
| **Senha** | (vazia) | Sem senha por padrão |
| **Driver** | `H2 JDBC Driver` | Incluído automaticamente |
| **Dialect Hibernate** | `H2Dialect` | ORM usa dialeto específico do H2 |
| **Pool de Conexões** | HikariCP | Gerenciador de conexões padrão |
| **Isolamento de Transação** | `READ_COMMITTED` | Nível de isolamento padrão |
| **Console H2** | `http://localhost:8080/h2-console` | Interface web para gerenciar o banco |
| **Console Habilitado** | ✅ Sim | Já configurado (`spring-boot-h2console`) |
| **Flyway** | ✅ Ativado | Migração automática (`db/migration/`) |

---

## 🔍 Como Acessar o Console H2

### 1) **Iniciar a Aplicação**
```powershell
./mvnw spring-boot:run
```

### 2) **Abrir o Console no Navegador**
```
http://localhost:8080/h2-console
```

### 3) **Credenciais de Conexão**
- **JDBC URL:** `jdbc:h2:mem:testdb`
- **User:** `sa`
- **Password:** (deixar em branco)
- Clique em **Connect**

---

## ⚙️ Personalizar Configurações do H2

Se quiser alterar as configurações padrão, edite `application.properties` e adicione:

### Exemplo: Ativar Modo Arquivo (Persistência)
```properties
# Armazenar em arquivo ao invés de memória
spring.datasource.url=jdbc:h2:file:./data/db
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Habilitar console H2
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Inicializador de BD (Flyway configurado)
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

### Exemplo: Modo In-Memory com Trace
```properties
# Para debug detalhado (lento, apenas desenvolvimento)
spring.datasource.url=jdbc:h2:mem:testdb;TRACE_LEVEL_SYSTEM_OUT=3
```

---

## 🧪 Verificar Configuração Atual em Runtime

### 1) **Via Properties no Teste**
Adicione um teste para ver as propriedades:

```java
@SpringBootTest
class H2ConfigurationTest {
    
    @Autowired
    private Environment environment;
    
    @Test
    void printH2Configuration() {
        System.out.println("JDBC URL: " + environment.getProperty("spring.datasource.url"));
        System.out.println("Username: " + environment.getProperty("spring.datasource.username"));
        System.out.println("Password: " + environment.getProperty("spring.datasource.password"));
        System.out.println("Driver: " + environment.getProperty("spring.datasource.driver-class-name"));
        System.out.println("H2 Console Enabled: " + environment.getProperty("spring.h2.console.enabled"));
        System.out.println("H2 Console Path: " + environment.getProperty("spring.h2.console.path"));
    }
}
```

### 2) **Via Logs da Aplicação**
Ao iniciar com `./mvnw spring-boot:run`, procure por logs como:
```
HikariPool-1 - Connection : url=jdbc:h2:mem:testdb user=SA
```

### 3) **Consultar Informações Direto no Banco**
Via console H2 ou JDBC, execute:
```sql
SELECT * FROM INFORMATION_SCHEMA.SETTINGS;
SELECT DATABASE_URL FROM INFORMATION_SCHEMA.SETTINGS;
```

---

## 📁 Arquivos Relacionados

- `pom.xml` → Dependência `spring-boot-h2console` habilita o console
- `src/main/resources/application.properties` → Configurações (atualmente vazio para H2)
- `src/test/resources/application-test.yml` → Configurações de teste (sem H2 específico)
- `src/main/resources/db/migration/` → Scripts Flyway para iniciar o schema

---

## 🚀 Próximos Passos (Opcionais)

1. **Persistência de Dados**: Mudar de in-memory para arquivo se precisar reter dados entre restarts.
2. **Senha do Console**: Adicionar autenticação ao H2 Console via Spring Security.
3. **Performance**: Ajustar pool de conexões (HikariCP) ou cache.
4. **Migração**: Preparar para PostgreSQL em produção (já está no `pom.xml`).

---

## 📚 Referências

- [H2 Console Official](http://h2database.com/html/cheatSheet.html)
- [Spring Boot H2 Docs](https://spring.io/guides/gs/accessing-data-h2/)
- [Flyway Migrations](https://flywaydb.org/)


