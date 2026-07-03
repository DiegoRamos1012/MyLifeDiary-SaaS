package com.diegoramos.mylifediary.config.app;

import com.diegoramos.mylifediary.common.base.AppProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {
}