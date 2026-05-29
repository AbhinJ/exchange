package com.exchange.persistence.consumer;

import com.exchange.common.event.OrderAcceptedEvent;
import com.exchange.common.event.TradeEvent;
import com.exchange.persistence.entity.OrderEntity;
import com.exchange.persistence.entity.TradeEntity;
import com.exchange.persistence.repository.OrderRepository;
import com.exchange.persistence.repository.TradeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.hibernate.query.Order;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeEventConsumer {

    private final TradeRepository tradeRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "events-out", groupId = "persistence-writer")
    public void onEvent(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> data = record.value();

        if (data.containsKey("tradeId")) {
            handleTrade(data);
        } else if (data.containsKey("remainingQuantity")) {
            handleOrderAccepted(data);
        } else {
            log.warn("Unknown event type: {}", data);
        }
    }

    private void handleOrderAccepted(Map<String, Object> data) {
        OrderAcceptedEvent event = objectMapper.convertValue(data, OrderAcceptedEvent.class);

        OrderEntity order = orderRepository.findByOrderId(event.getOrderId())
                .orElseGet(() -> {
                    OrderEntity newOrder = new OrderEntity();
                    newOrder.setOrderId(event.getOrderId());
                    newOrder.setUserId(event.getUserId());
                    newOrder.setTradingPair(event.getTradingPair());
                    newOrder.setSide(event.getSide().name());
                    newOrder.setType("LIMIT");
                    newOrder.setPrice(event.getPrice());
                    newOrder.setQuantity(event.getRemainingQuantity());
                    newOrder.setFilledQuantity(BigDecimal.ZERO);
                    newOrder.setStatus("NEW");
                    newOrder.setCreatedAt(Instant.now());
                    return newOrder;
                });

        order.setStatus("NEW");
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        log.info("Saved order: {} {} {} @ {} with remaining quantity {}",
                order.getSide(), order.getQuantity(),
                order.getTradingPair(), order.getPrice(),
                event.getRemainingQuantity());
    }

    private void handleTrade(Map<String, Object> data) {
        TradeEvent event = objectMapper.convertValue(data, TradeEvent.class);

        TradeEntity trade = new TradeEntity();
        trade.setTradeId(event.getTradeId());
        trade.setBuyOrderId(event.getBuyOrderId());
        trade.setSellOrderId(event.getSellOrderId());
        trade.setTradingPair(event.getTradingPair());
        trade.setPrice(event.getPrice());
        trade.setQuantity(event.getQuantity());
        trade.setBuyUserId(event.getBuyUserId());
        trade.setSellUserId(event.getSellUserId());
        trade.setExecutedAt(event.getTimestamp());

        tradeRepository.save(trade);
        log.info("Saved trade: {} {} @ {} for orders {} and {}",
                trade.getQuantity(), trade.getTradingPair(),
                trade.getPrice(), trade.getBuyOrderId(), trade.getSellOrderId());
    }
}
