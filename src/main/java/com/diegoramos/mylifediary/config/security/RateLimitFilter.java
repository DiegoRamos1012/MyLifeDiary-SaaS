package com.diegoramos.mylifediary.config.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Filtro que aplica Rate Limit em rotas sensíveis para impedir ataques de força bruta
 * e criação massiva de contas não verificadas.
 *
 * <p>Cada rota possui sua própria política de tentativas. A chave do bucket
 * combina IP e rota para garantir buckets independentes por endpoint.</p>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Map<String, Integer> RATE_LIMITED_ROUTES = Map.of(
            "POST:/users/register", 5,
            "POST:/auth/login", 15
    );

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    /**
     * Cria um bucket com capacidade definida pela política da rota,
     * restaurado integralmente após 1 hora.
     */
    private Bucket createBucket(int capacity) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervally(capacity, Duration.ofHours(1))
                        .build())
                .build();
    }

    /**
     * Extrai o IP real do cliente, considerando proxy e Docker.
     * O header X-Forwarded-For pode conter múltiplos IPs — o primeiro é sempre o cliente real.
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Realiza a filtragem de Rate Limit, devolvendo {@code 429 Too Many Requests}
     * caso o IP exceda as tentativas disponíveis para a rota.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String routeKey = request.getMethod() + ":" + request.getRequestURI();
        int capacity = RATE_LIMITED_ROUTES.get(routeKey);

        String ip = extractClientIp(request);
        String bucketKey = ip + ":" + routeKey;
        Bucket bucket = buckets.get(bucketKey, k -> createBucket(capacity));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Muitas tentativas. Tente novamente em 1 hora.\"}");
        }
    }

    /**
     * Aplica o filtro apenas nas rotas mapeadas em {@code RATE_LIMITED_ROUTES}.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String routeKey = request.getMethod() + ":" + request.getRequestURI();
        return !RATE_LIMITED_ROUTES.containsKey(routeKey);
    }
}