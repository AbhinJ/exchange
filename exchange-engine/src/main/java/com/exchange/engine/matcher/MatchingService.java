package com.exchange.engine.matcher;

import com.exchange.common.command.CancelOrderCommand;
import com.exchange.common.command.PlaceOrderCommand;
import com.exchange.common.event.ExchangeEvent;
import com.exchange.common.event.OrderAcceptedEvent;
import com.exchange.common.event.OrderCancelledEvent;
import com.exchange.common.event.TradeEvent;
import com.exchange.engine.orderbook.Order;
import com.exchange.engine.orderbook.OrderBook;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MatchingService {

    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

    public List<ExchangeEvent> handlePlaceOrder(PlaceOrderCommand command) {
        OrderBook book = orderBooks.computeIfAbsent(
                command.getTradingPair(), OrderBook::new);

        Order order = new Order(
                command.getOrderId(),
                command.getUserId(),
                command.getTradingPair(),
                command.getSide(),
                command.getType(),
                command.getPrice(),
                command.getQuantity(),
                command.getTimestamp()
        );

        // Match the order against the book
        List<TradeEvent> trades = book.addOrder(order);
        List<ExchangeEvent> events = new ArrayList<>(trades);

        for (TradeEvent trade : trades) {
            log.info("Trade executed: {} {} @ {} on {}",
                    trade.getQuantity(), command.getTradingPair(),
                    trade.getPrice(), trade.getTradeId());
        }

        // If the order has remaining quantity on the book, emit an accepted event
        if (order.getRemainingQuantity().signum() > 0
                && command.getType() == com.exchange.common.enums.OrderType.LIMIT) {
            events.add(new OrderAcceptedEvent(
                    order.getOrderId(),
                    order.getUserId(),
                    order.getTradingPair(),
                    order.getSide(),
                    order.getPrice(),
                    order.getRemainingQuantity(),
                    Instant.now()
            ));
            log.info("Order resting: {} {} {} @ {} (remaining: {})",
                    order.getSide(), command.getTradingPair(),
                    order.getOrderId(), order.getPrice(),
                    order.getRemainingQuantity());
        }

        return events;
    }

    public List<ExchangeEvent> handleCancelOrder(CancelOrderCommand command) {
        List<ExchangeEvent> events = new ArrayList<>();

        OrderBook book = orderBooks.get(command.getTradingPair());
        if (book != null && book.cancelOrder(command.getOrderId())) {
            events.add(new OrderCancelledEvent(
                    command.getOrderId(),
                    command.getTradingPair(),
                    command.getUserId(),
                    Instant.now()
            ));
            log.info("Order cancelled: {} on {}", command.getOrderId(), command.getTradingPair());
        }

        return events;
    }

    public OrderBook getOrderBook(String tradingPair) {
        return orderBooks.get(tradingPair);
    }
}
