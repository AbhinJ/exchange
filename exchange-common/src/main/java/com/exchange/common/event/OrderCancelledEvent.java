package com.exchange.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderCancelledEvent extends ExchangeEvent {
    private String orderId;
    private Long userId;

    public OrderCancelledEvent(String orderId, String tradingPair,
                               Long userId, Instant timestamp) {
        super(tradingPair, timestamp);
        this.orderId = orderId;
        this.userId = userId;
    }
}


