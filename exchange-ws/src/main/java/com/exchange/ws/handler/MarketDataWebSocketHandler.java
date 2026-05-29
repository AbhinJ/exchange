package com.exchange.ws.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketDataWebSocketHandler implements WebSocketHandler {

    private final WebSocketSessionManager sessionManager;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String path = session.getHandshakeInfo().getUri().getPath();
        String tradingPair = path.substring(path.lastIndexOf('/') + 1);

        log.info("WebSocket connected: {} for {}", session.getId(), tradingPair);
        sessionManager.addSession(tradingPair, session);

        return session.receive()
                .doFinally(signal -> {
                    sessionManager.removeSession(tradingPair, session);
                    log.info("WebSocket disconnected: {} for {}", session.getId(), tradingPair);
                })
                .then();
    }
}
