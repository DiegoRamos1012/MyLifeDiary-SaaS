
## unreleased - 2026-05-24



### ⚙️ Maintenance


- Added worflow to avoid commits with sensitive files attached, .gitignore updated



### 💼 Other


- Initial Commit - Setting Spring Project

- Dockerfile, docker-compose and .dockerignore

- Changing HELP.md to /docs

- BaseEntity with UUID v7

- GlobalExceptionHandler for unexpected erros and Result Pattern implementation

- OpenAPI Swagger initial setup

- Renaming 'TheLifeDiary' to 'MyLifeDiary'

- Improving Result Pattern documentation to better explanation about decisions of result failure and result error

- Adding constructor and factory to User entity

- Removing tag public in Main to avoid redundancy with Java 25

- Refactoring User

- Created DomainException to handle unexpected errors from domain layer

- Refactoring with Domain Exception completed

- Created User DTOs and Repository

- Added passwordConfig and Instant modified with custom clock

- Erased one line

- Inserting create and update methods for User, with scheduling jobs and unit tests

- Upgrading documentation in entity and repository

- Added restoreUser method

- Changed a lot of Ifs for switch case

- Created UserController with helper to result to http status treatment

- Added controller

- Refactoring User logic to update docs and improve code legibility

- Padronizando erros, adicionando Logs para auditoria no desenvolvimento sem exposição de dados sensíveis

- Created changePassword method

- Added changeEmail endpoint with refactoring

- Added deleteUser, restoreUser endpoints with swagger docs

- Debug

- Added SecurityConfig to avoid authentication to access Swagger

- Added method to search users by status in UserRepository

- findByStatusIn method fixed

- Added UserServiceTest with 100% coverage

- Added date validation with LocalDate clock

- changeProfileInfo javadoc updated

- adding docs of project

- Added auth feature with unit and integration tests

- Protecting JWT enviromnent variables

- Added test variables to SpringBootTest

- added env.example

- Added h2 configuration

- RefreshToken entity now inherits BaseEntity ID column, fixed User's tests

- Added new doc

- Habit domain created, User refactored with utility classes



### 📚 Docs


- add jwt-auth implementation plan

- add refresh token implementation plan

- expand refresh-token-plan with detailed test section



### 🚀 Features


- add findAll endpoint to UserController, add missing Javadoc, add lifecycle flow docs

- Add new refresh token for user protection


