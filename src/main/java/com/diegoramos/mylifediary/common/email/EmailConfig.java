package com.diegoramos.mylifediary.common.email;

import com.resend.Resend;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ResendProperties.class)
public class EmailConfig {

    @Bean
    public Resend resend(ResendProperties properties) {
        return new Resend(properties.apiKey());
    }
    // só habilitar o ConfigurationProperties já é suficiente aqui
    // o Spring injeta o ResendEmailService automaticamente
}
