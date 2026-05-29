package com.exchange.ws.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {
    
    private final Map<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    
    public void addSession(String tradingPair, WebSocketSession session) {
        sessions.computeIfAbsent(tradingPair, k -> ConcurrentHashMap.newKeySet())
                .add(session);
    }
    
    public void removeSession(String tradingPair, WebSocketSession session) {
        Set<WebSocketSession> pairSessions = sessions.get(tradingPair);
        if (pairSessions != null) {
            pairSessions.remove(session);
            if (pairSessions.isEmpty()) {
                sessions.remove(tradingPair);
            }
        }
    }
    
    public Set<WebSocketSession> getSessions(String tradingPair) {
        return sessions.getOrDefault(tradingPair, Set.of());
    }
}