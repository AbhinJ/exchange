package com.exchange.engine.config;

import com.exchange.engine.matcher.MatchingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineConfig {

    @Bean
    public MatchingService matchingService() {
        return new MatchingService();
    }
}

