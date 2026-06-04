package com.diegoramos.mylifediary.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuração de codificação de senhas da aplicação.
 *
 * <p>Utiliza o algoritmo Argon2, considerado um dos mais seguros atualmente
 * para armazenamento de senhas. Ele foi projetado para ser resistente a ataques
 * de força bruta e ataques com GPU, utilizando não apenas CPU, mas também memória.</p>
 *
 * <p>Ao contrário do bcrypt, o Argon2 permite configurar explicitamente o uso de memória,
 * tornando ataques com hardware paralelo (como GPUs) significativamente mais difíceis e custosos.</p>
 *
 * <p>O Argon2 permite configurar custo computacional através de três fatores principais:
 * memória, tempo (iterações) e paralelismo, permitindo equilibrar segurança e performance.</p>
 *
 * <p>Esta configuração foi ajustada para um cenário de SaaS, priorizando segurança
 * sem comprometer a escalabilidade.</p>
 */
@Configuration
public class PasswordConfig {

    /**
     * Define o {@link PasswordEncoder} utilizado na aplicação.
     *
     * <p>Parâmetros utilizados no Argon2:</p>
     *
     * <ul>
     *   <li><b>saltLength (16 bytes):</b>
     *   Define o tamanho do salt gerado automaticamente para cada senha.
     *   O salt garante que senhas iguais gerem hashes diferentes, prevenindo ataques
     *   como rainbow tables.</li>
     *
     *   <li><b>hashLength (32 bytes):</b>
     *   Tamanho do hash final gerado. Um valor maior aumenta a segurança,
     *   tornando mais difícil a colisão e ataques de pré-imagem.</li>
     *
     *   <li><b>parallelism (1):</b>
     *   Número de threads usadas no cálculo. Em ambientes SaaS com alta concorrência,
     *   manter em 1 ajuda a evitar consumo excessivo de CPU.</li>
     *
     *   <li><b>memory (65536 KB = 64 MB):</b>
     *   Quantidade de memória utilizada no processo de hashing.
     *   Esse é o principal fator de segurança contra ataques com GPU,
     *   pois torna o custo por tentativa significativamente maior.</li>
     *
     *   <li><b>iterations (3):</b>
     *   Número de vezes que o algoritmo é executado.
     *   Aumenta o tempo necessário para gerar/verificar o hash,
     *   dificultando ataques de força bruta.</li>
     * </ul>
     *
     * <p><b>Observação:</b> Esses valores foram escolhidos para manter o tempo de hashing
     * entre aproximadamente 100ms e 250ms, o que é considerado seguro e performático
     * para aplicações web modernas.</p>
     *
     * @return instância configurada de {@link Argon2PasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
                16,
                32,
                1,
                65536,
                3
        );
    }
}