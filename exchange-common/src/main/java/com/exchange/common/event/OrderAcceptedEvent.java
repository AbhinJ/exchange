package com.exchange.common.event;

import com.exchange.common.enums.OrderSide;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderAcceptedEvent extends ExchangeEvent {
    private String orderId;
    private Long userId;
    private OrderSide side;
    private BigDecimal price;
    private BigDecimal remainingQuantity;

    public OrderAcceptedEvent(String orderId, Long userId, String tradingPair,
                              OrderSide side, BigDecimal price,
                              BigDecimal remainingQuantity, Instant timestamp) {
        super(tradingPair, timestamp);
        this.orderId = orderId;
        this.userId = userId;
        this.side = side;
        this.price = price;
        this.remainingQuantity = remainingQuantity;
    }
}

