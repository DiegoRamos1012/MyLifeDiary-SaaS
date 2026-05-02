package com.diegoramos.mylifediary.modules.user.service;

import com.diegoramos.mylifediary.common.exception.DomainException;
import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import com.diegoramos.mylifediary.modules.user.dto.request.CreateUserRequest;
import com.diegoramos.mylifediary.modules.user.dto.request.UpdateEmailRequest;
import com.diegoramos.mylifediary.modules.user.dto.request.UpdatePasswordRequest;
import com.diegoramos.mylifediary.modules.user.dto.request.UpdateUserInfoRequest;
import com.diegoramos.mylifediary.modules.user.dto.response.UserResponseDTO;
import com.diegoramos.mylifediary.modules.user.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Método de leitura com paginação que retorna todos os usuários do sistema em ordem alfabética (ASC)
     * que permite filtragem por nome
     *
     * @param search = Termo de busca opcional para uma pesquisa filtrada pelo nome completo ignorando maiúsculo
     * @param page   = A página com o conteúdo dos usuários requisitados
     * @param size   = O tamanho da página (começa por 0)
     * @return = retorna o usuário pesquisado ou todos cadastrados convertidos em DTO
     */
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> findAll(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());

        if (search != null && !search.isBlank()) {
            return userRepository.findByFullNameContainingIgnoreCase(search, pageable)
                    .map(UserResponseDTO::from);
        }

        return userRepository.findAll(pageable).map(UserResponseDTO::from);
    }

    /**
     * Cria um usuário com senha criptografada
     *
     * @param dto = DTO que traz os dados para a criação
     * @return = Retorna o usuário criado e o status de Success
     */
    public Result<UserResponseDTO> register(@NonNull CreateUserRequest dto) {
        if (userRepository.existsByEmailIgnoreCase(dto.email())) {
            log.info("register: email already exists (fast-fail)");
            return Result.failure("USER_EMAIL_ALREADY_EXISTS", "E-mail já cadastrado");
        }

        try {
            String passwordHash = passwordEncoder.encode(dto.password());
            User user = User.create(dto.email(), passwordHash, dto.fullName(), dto.birthDate());
            User saved = userRepository.save(user);
            return Result.success(UserResponseDTO.from(saved));
        } catch (DomainException ex) {
            log.info("register: domain validation failed: {}", ex.getMessage());
            return Result.failure("USER_INVALID_INPUT", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            log.warn("register: data integrity violation (possible race)");
            return Result.failure("USER_EMAIL_ALREADY_EXISTS", "E-mail já cadastrado");
        }
    }

    /**
     * Atualiza o email do usuário identificado por {@code userId}.
     * <p>
     * Mantém o padrão de uso de {@link Result}: validações esperadas retornam
     * {@code Result.failure(code, message)} enquanto sucessos retornam
     * {@code Result.success(dto)}.
     *
     * @param userId id do usuário a ser atualizado
     * @param dto    DTO contendo o novo email
     * @return resultado com o usuário atualizado ou falha de negócio
     */
    public Result<UserResponseDTO> changeEmail(UUID userId, @NonNull UpdateEmailRequest dto) {
        return userRepository.findById(userId)
                .map(user -> switch (dto.newEmail() == null ? null : dto.newEmail().trim()) {
                    case null -> Result.<UserResponseDTO>failure("USER_INVALID_EMAIL", "E-mail inválido");
                    case "" -> Result.<UserResponseDTO>failure("USER_INVALID_EMAIL", "E-mail inválido");
                    case String requestedEmail when requestedEmail.equalsIgnoreCase(user.getEmail()) ->
                            Result.<UserResponseDTO>failure("USER_EMAIL_SAME", "O novo e-mail é igual ao atual");
                    case String requestedEmail when userRepository.existsByEmailIgnoreCase(requestedEmail) ->
                            Result.<UserResponseDTO>failure("USER_EMAIL_ALREADY_EXISTS", "E-mail já cadastrado");
                    case String requestedEmail -> {
                        try {
                            user.changeEmail(requestedEmail);
                        } catch (DomainException ex) {
                            log.info("changeEmail: domain error for userId={} reason={}", userId, ex.getMessage());
                            yield Result.<UserResponseDTO>failure("USER_UPDATE_FAILED", ex.getMessage());
                        }

                        User updated = userRepository.save(user);
                        yield Result.success(UserResponseDTO.from(updated));
                    }
                })
                .orElseGet(() -> {
                    log.info("changeEmail: user not found userId={}", userId);
                    return Result.failure("USER_NOT_FOUND", "Usuário não encontrado");
                });
    }

    /**
     * Altera a senha do usuário identificado por {@code userId}.
     * <p>
     * Mantém o padrão de uso de {@link Result}: validações esperadas retornam
     * {@code Result.failure(code, message)} enquanto sucessos retornam
     * {@code Result.success(dto)}.
     *
     * @param userId id do usuário a ser atualizado
     * @param dto    DTO contendo a nova senha
     * @return resultado com o usuário atualizado ou falha de negócio
     */
    public Result<UserResponseDTO> changePassword(UUID userId, @NonNull UpdatePasswordRequest dto) {
        return userRepository.findById(userId)
                .map(user -> {
                    try {
                        String requestedPassword = dto.newPassword();
                        String newPasswordHash = passwordEncoder.encode(requestedPassword);
                        user.changePassword(newPasswordHash);
                    } catch (DomainException ex) {
                        return Result.<UserResponseDTO>failure("USER_UPDATE_FAILED", ex.getMessage());
                    }
                    User updated = userRepository.save(user);
                    return Result.success(UserResponseDTO.from(updated));
                })
                .orElseGet(() -> {
                    log.info("changePassword: user not found userId={}", userId);
                    return Result.failure("USER_NOT_FOUND", "Usuário não encontrado");
                });
    }

    public Result<UserResponseDTO> changeProfileInfo(UUID userId, @NonNull UpdateUserInfoRequest dto) {
        boolean hasNoUpdates =
                dto.newFullName() == null && dto.newDateBirth() == null;

        if (hasNoUpdates) {
            return Result.failure(
                    "USER_INFO_EMPTY_UPDATE",
                    "Informe ao menos nome ou data de nascimento para atualizar"
            );
        }

        if (dto.newFullName() != null && dto.newFullName().isBlank()) {
            return Result.failure(
                    "USER_INVALID_FULL_NAME",
                    "Nome inválido"
            );
        }

        return userRepository.findById(userId)
                .map(user -> {
                    String targetFullName = dto.newFullName() != null ? dto.newFullName() : user.getFullName();
                    LocalDate targetBirthDate = dto.newDateBirth() != null ? dto.newDateBirth() : user.getBirthDate();
                    try {
                        user.changeProfileInfo(targetFullName, targetBirthDate);
                    } catch (DomainException ex) {
                        log.info("changeProfileInfo: domain error for userId={} reason={}", userId, ex.getMessage());
                        return Result.<UserResponseDTO>failure("USER_UPDATE_FAILED", ex.getMessage());
                    }
                    User updated = userRepository.save(user);
                    return Result.success(UserResponseDTO.from(updated));
                })
                .orElseGet(() -> {
                    log.info("changeProfileInfo: user not found userId={}", userId);
                    return Result.failure("USER_NOT_FOUND", "Usuário não encontrado");
                });
    }

    /**
     * Solicita a exclusão da conta do usuário e registra a data da solicitação
     *
     * @param userId = ID do usuário recebido pelo controller
     * @return = Retorna o usuário atualizado em caso de sucesso ou falha esperada conforme regra de negócio
     */
    public Result<UserResponseDTO> deleteUser(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> switch (user.getStatus()) {
                    case PENDING_DELETION ->
                            Result.<UserResponseDTO>failure("DELETION_ALREADY_REQUESTED", "A exclusão já foi solicitada");
                    case INACTIVE ->
                            Result.<UserResponseDTO>failure("USER_ALREADY_INACTIVE", "Usuário já está inativo");
                    case SUSPENDED ->
                            Result.<UserResponseDTO>failure("USER_SUSPENDED", "Usuário suspenso não pode solicitar exclusão");
                    case ACTIVE -> {
                        try {
                            user.requestDeletion(clock.instant());
                        } catch (DomainException ex) {
                            log.info("deleteUser: domain error userId={} reason={}", userId, ex.getMessage());
                            yield Result.<UserResponseDTO>failure("USER_UPDATE_FAILED", ex.getMessage());
                        }
                        User updated = userRepository.save(user);
                        yield Result.success(UserResponseDTO.from(updated));
                    }
                })
                .orElseGet(() -> {
                    log.info("deleteUser: user not found userId={}", userId);
                    return Result.failure("USER_NOT_FOUND", "Usuário não encontrado");
                });
    }

    /**
     * Restaura o usuário para ACTIVE quando a conta continua em exclusão pendente.
     *
     * @param userId = ID do usuário recebido pelo controller
     * @return = Retorna o usuário restaurado em caso de sucesso ou falha esperada conforme regra de negócio
     */
    public Result<UserResponseDTO> restoreUser(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> switch (user.getStatus()) {
                    case ACTIVE -> Result.<UserResponseDTO>failure("USER_ALREADY_ACTIVE", "Usuário já está ativo");
                    case SUSPENDED ->
                            Result.<UserResponseDTO>failure("USER_SUSPENDED", "Usuário suspenso não pode ser restaurado");
                    case INACTIVE ->
                            Result.<UserResponseDTO>failure("USER_RESTORE_NOT_ALLOWED", "Não é possível restaurar usuário inativo");

                    case PENDING_DELETION -> {
                        try {
                            user.restoreAccount();
                        } catch (DomainException ex) {
                            log.info("restoreUser: domain error userId={} reason={}", userId, ex.getMessage());
                            yield Result.<UserResponseDTO>failure("USER_UPDATE_FAILED", ex.getMessage());
                        }
                        User updated = userRepository.save(user);
                        yield Result.success(UserResponseDTO.from(updated));
                    }
                })
                .orElseGet(() -> {
                    log.info("restoreUser: user not found userId={}", userId);
                    return Result.failure("USER_NOT_FOUND", "Usuário não encontrado");
                });
    }
}
