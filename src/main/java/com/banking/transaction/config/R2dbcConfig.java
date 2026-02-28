package com.banking.transaction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.banking.transaction.repository")
@EnableR2dbcAuditing
public class R2dbcConfig {
    // R2DBC auditing and repository scanning configuration
    // Connection factory is auto-configured by Spring Boot via application.yml
}
