package com.diegoramos.mylifediary.common.email;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ResendProperties.class)
public class EmailConfig {
    // só habilitar o ConfigurationProperties já é suficiente aqui
    // o Spring injeta o ResendEmailService automaticamente
}
