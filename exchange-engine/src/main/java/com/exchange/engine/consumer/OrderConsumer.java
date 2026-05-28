package com.exchange.engine.consumer;

import com.exchange.common.command.PlaceOrderCommand;
import com.exchange.engine.matcher.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderConsumer {

    private static final String TOPIC_OUT = "events-out";

    private final MatchingService matchingService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "orders-in", groupId = "matching-engine")
    public void onOrder(PlaceOrderCommand command) {
        log.info("Received order: {} {} {} @ {}",
                command.getSide(), command.getQuantity(),
                command.getTradingPair(), command.getPrice());

        List<Object> events = matchingService.handlePlaceOrder(command);

        for (Object event : events) {
            kafkaTemplate.send(TOPIC_OUT, command.getTradingPair(), event);
        }

        log.info("Published {} events for order {}", events.size(), command.getOrderId());
    }
}
