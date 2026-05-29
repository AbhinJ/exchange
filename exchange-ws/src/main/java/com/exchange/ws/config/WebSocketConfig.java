package com.exchange.ws.config;

import com.exchange.ws.handler.MarketDataWebSocketHandler;
import com.exchange.ws.handler.WebSocketSessionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

@Configuration
public class WebSocketConfig {

    @Bean
    public HandlerMapping webSocketHandlerMapping(MarketDataWebSocketHandler handler) {
        return new SimpleUrlHandlerMapping(
                Map.of("/ws/marketdata/**", handler), 1);
    }

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
