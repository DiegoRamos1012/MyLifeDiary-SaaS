package com.diegoramos.mylifediary.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuração inicial do OpenAPI/Swagger para a API do TheLifeDiary.
 *
 * <p>Este bean centraliza os metadados da documentação e prepara a aplicação
 * para evoluir com autenticação, tags por módulo e ajustes de UI sem espalhar
 * configuração pela base de código.
 */
@Configuration
public class SwaggerConfig {

    /**
     * Cria a definição principal do OpenAPI exposta pelo Swagger UI.
     *
     * @return configuração base da documentação da API
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TheLifeDiary API")
                        .description("Documentação da API backend do TheLifeDiary")
                        .version("v1"))
                .servers(List.of(new Server().url("/")));
    }
}

