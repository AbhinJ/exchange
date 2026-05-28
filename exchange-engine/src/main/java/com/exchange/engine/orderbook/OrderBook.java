package com.exchange.engine.orderbook;

import com.exchange.common.enums.OrderSide;
import com.exchange.common.enums.OrderType;
import com.exchange.common.event.TradeEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class OrderBook {

    private final String tradingPair;

    private final TreeMap<BigDecimal, LinkedList<Order>> bids =
            new TreeMap<>(Comparator.reverseOrder());

    private final TreeMap<BigDecimal, LinkedList<Order>> asks = new TreeMap<>();

    private final Map<String, Order> orderMap = new HashMap<>();

    public OrderBook(String tradingPair) {
        this.tradingPair = tradingPair;
    }

    public List<TradeEvent> addOrder(Order order) {
        List<TradeEvent> trades = match(order);

        if (order.getRemainingQuantity().signum() > 0
                && order.getType() == OrderType.LIMIT) {
            TreeMap<BigDecimal, LinkedList<Order>> book =
                    order.getSide() == OrderSide.BUY ? bids : asks;

            book.computeIfAbsent(order.getPrice(), k ->
                    new LinkedList<>()).add(order);

            orderMap.put(order.getOrderId(), order);
        }

        return trades;
    }

    public boolean cancelOrder(String orderId) {
        Order order = orderMap.remove(orderId);
        if (order == null) return false;

        TreeMap<BigDecimal, LinkedList<Order>> book =
                order.getSide() == OrderSide.BUY ? bids : asks;

        LinkedList<Order> level = book.get(order.getPrice());
        if (level != null) {
            level.remove(order);
            if (level.isEmpty()) {
                book.remove(order.getPrice());
            }
        }
        return true;
    }


    private List<TradeEvent> match(Order incomingOrder) {
        List<TradeEvent> trades = new ArrayList<>();

        TreeMap<BigDecimal, LinkedList<Order>> oppositeBook =
                incomingOrder.getSide() == OrderSide.BUY ? asks : bids;

        while (incomingOrder.getRemainingQuantity().signum() > 0 &&
                !oppositeBook.isEmpty()) {
            Map.Entry<BigDecimal, LinkedList<Order>> bestLevel =
                    oppositeBook.firstEntry();

            BigDecimal bestPrice = bestLevel.getKey();

            if (incomingOrder.getType() == OrderType.LIMIT) {
                if (incomingOrder.getSide() == OrderSide.BUY &&
                        incomingOrder.getPrice().compareTo(bestPrice) < 0) {
                    break;
                }

                if (incomingOrder.getSide() == OrderSide.SELL &&
                        incomingOrder.getPrice().compareTo(bestPrice) > 0) {
                    break;
                }
            }

            LinkedList<Order> level = bestLevel.getValue();

            while (incomingOrder.getRemainingQuantity().signum() > 0 && !level.isEmpty()) {
                Order restingOrder = level.getFirst();

                BigDecimal tradeQuantity =
                        incomingOrder.getRemainingQuantity().min(restingOrder.getRemainingQuantity());

                incomingOrder.setRemainingQuantity(
                        incomingOrder.getRemainingQuantity().subtract(tradeQuantity));

                restingOrder.setRemainingQuantity(
                        restingOrder.getRemainingQuantity().subtract(tradeQuantity));

                String buyOrderId = incomingOrder.getSide() == OrderSide.BUY
                        ? incomingOrder.getOrderId() : restingOrder.getOrderId();
                String sellOrderId = incomingOrder.getSide() == OrderSide.SELL
                        ? incomingOrder.getOrderId() : restingOrder.getOrderId();
                Long buyUserId = incomingOrder.getSide() == OrderSide.BUY
                        ? incomingOrder.getUserId() : restingOrder.getUserId();
                Long sellUserId = incomingOrder.getSide() == OrderSide.SELL
                        ? incomingOrder.getUserId() : restingOrder.getUserId();

                TradeEvent trade = new TradeEvent(
                        UUID.randomUUID().toString(),
                        tradingPair,
                        buyOrderId, sellOrderId,
                        buyUserId, sellUserId,
                        bestPrice,
                        tradeQuantity,
                        Instant.now()
                );
                trades.add(trade);

                if (restingOrder.getRemainingQuantity().signum() == 0) {
                    level.removeFirst();
                    orderMap.remove(restingOrder.getOrderId());
                }
            }

            if (level.isEmpty()) {
                oppositeBook.pollFirstEntry();
            }
        }
        return trades;
    }

    public String getTradingPair() { return tradingPair; }
    public TreeMap<BigDecimal, LinkedList<Order>> getBids() { return bids; }
    public TreeMap<BigDecimal, LinkedList<Order>> getAsks() { return asks; }
    public Order getOrder(String orderId) { return orderMap.get(orderId); }
}
