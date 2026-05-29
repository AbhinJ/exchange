package com.exchange.snapshot.service;

import com.exchange.common.enums.OrderSide;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private final Map<String, TreeMap<BigDecimal, BigDecimal>> bids = new HashMap<>();
    private final Map<String, TreeMap<BigDecimal, BigDecimal>> asks = new HashMap<>();

    public void handleOrderAccepted(String tradingPair, OrderSide side,
                                     BigDecimal price, BigDecimal quantity) {
        TreeMap<BigDecimal, BigDecimal> book = getBook(tradingPair, side);
        book.merge(price, quantity, BigDecimal::add);
        saveSnapshot(tradingPair);
    }

    public void handleTrade(String tradingPair, BigDecimal price, BigDecimal quantity) {
        TreeMap<BigDecimal, BigDecimal> askBook = asks.get(tradingPair);
        if (askBook != null) {
            reduceLevel(askBook, price, quantity);
        }

        TreeMap<BigDecimal, BigDecimal> bidBook = bids.get(tradingPair);
        if (bidBook != null) {
            reduceLevel(bidBook, price, quantity);
        }

        saveSnapshot(tradingPair);
    }

    public void handleOrderCancelled(String tradingPair) {
        saveSnapshot(tradingPair);
    }

    private void reduceLevel(TreeMap<BigDecimal, BigDecimal> book,
                             BigDecimal price, BigDecimal quantity) {
        BigDecimal current = book.get(price);
        if (current != null) {
            BigDecimal remaining = current.subtract(quantity);
            if (remaining.signum() <= 0) {
                book.remove(price);
            } else {
                book.put(price, remaining);
            }
        }
    }

    private void saveSnapshot(String tradingPair) {
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("tradingPair", tradingPair);
            snapshot.put("bids", formatLevels(bids.getOrDefault(tradingPair, new TreeMap<>())));
            snapshot.put("asks", formatLevels(asks.getOrDefault(tradingPair, new TreeMap<>())));
            snapshot.put("updatedAt", System.currentTimeMillis());

            String json = objectMapper.writeValueAsString(snapshot);
            redisTemplate.opsForValue().set("orderbook:" + tradingPair, json);

            log.debug("Snapshot saved for {}", tradingPair);
        } catch (JsonProcessingException e) {
            log.error("Failed to save snapshot for {}", tradingPair, e);
        }
    }

    private List<List<BigDecimal>> formatLevels(TreeMap<BigDecimal, BigDecimal> book) {
        List<List<BigDecimal>> levels = new ArrayList<>();
        for (Map.Entry<BigDecimal, BigDecimal> entry : book.entrySet()) {
            levels.add(List.of(entry.getKey(), entry.getValue()));
        }
        return levels;
    }

    private TreeMap<BigDecimal, BigDecimal> getBook(String tradingPair, OrderSide side) {
        if (side == OrderSide.BUY) {
            return bids.computeIfAbsent(tradingPair,
                    k-> new TreeMap<>(Comparator.reverseOrder()));
        } else {
            return asks.computeIfAbsent(tradingPair, k -> new TreeMap<>());
        }
    }
}