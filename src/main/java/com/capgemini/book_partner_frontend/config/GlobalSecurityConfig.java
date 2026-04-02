package com.capgemini.book_partner_frontend.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GlobalSecurityConfig {

    @Bean
    public RestClientCustomizer addSecretHeaderCustomizer() {
        // This automatically glues the secret header to EVERY outgoing request
        return builder -> builder.defaultHeader("X-Project-Secret", "BulletProofDemo2026!");
    }
}