package com.exchange.ws.consumer;

import com.exchange.ws.handler.WebSocketSessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.net.http.WebSocket;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketDataConsumer {
    
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = "events-out", groupId = "ws-broadcaster")
    public void onEvent(ConsumerRecord<String, Map<String, Object>> record) {
        String tradingPair = record.key();
        Map<String, Object> eventData = record.value();

        Set<WebSocketSession> sessions = sessionManager.getSessions(tradingPair);
        if (sessions.isEmpty()) return;

        try {
            String json = objectMapper.writeValueAsString(eventData);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.send(Mono.just(
                            session.textMessage(json)
                    )).subscribe();
                }
            }

            log.debug("Broadcast to {} clients for {}", sessions.size(), tradingPair);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event", e);
        }
    }
}
