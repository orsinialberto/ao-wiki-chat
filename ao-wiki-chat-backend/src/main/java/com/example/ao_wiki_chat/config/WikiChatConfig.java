package com.example.ao_wiki_chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Spring configuration class providing Jackson ObjectMapper bean.
 * 
 * <p>This configuration ensures a consistent ObjectMapper instance is available
 * throughout the application for JSON serialization and deserialization.
 */
@Configuration
public class WikiChatConfig {

    /**
     * Creates and configures a Jackson ObjectMapper bean.
     * 
     * @return a configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
