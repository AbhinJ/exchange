package com.exchange.gateway.service;

import com.exchange.common.command.PlaceOrderCommand;
import com.exchange.common.enums.OrderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final String TOPIC = "orders-in";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Mono<PlaceOrderCommand> placeOrder(PlaceOrderCommand command) {
        return Mono.fromCallable(() -> {
            command.setOrderId(UUID.randomUUID().toString());
            command.setTimestamp(Instant.now());

            validate(command);

            kafkaTemplate.send(TOPIC, command.getTradingPair(), command);
            log.info("Order published: {} {} {} @ {} on {}",
                    command.getSide(), command.getQuantity(),
                    command.getTradingPair(), command.getPrice(),
                    command.getOrderId());

            return command;
        });
    }

    private void validate(PlaceOrderCommand command) {
        if (command.getTradingPair() == null || command.getTradingPair().isBlank()) {
            throw new IllegalArgumentException("Trading pair is required");
        }
        if (command.getSide() == null) {
            throw new IllegalArgumentException("Side (BUY/SELL) is required");
        }
        if (command.getType() == null) {
            throw new IllegalArgumentException("Type (LIMIT/MARKET) is required");
        }
        if (command.getQuantity() == null || command.getQuantity().signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (command.getType() == OrderType.LIMIT &&
                (command.getPrice() == null || command.getPrice().signum() <= 0)) {
            throw new IllegalArgumentException("Limit orders require a positive price");
        }
    }
}

