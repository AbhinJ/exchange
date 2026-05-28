package com.exchange.common.event;

import com.exchange.common.enums.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderAcceptedEvent {
    private String orderId;
    private Long userId;
    private String tradingPair;
    private OrderSide side;
    private BigDecimal price;
    private BigDecimal remainingQuantity;
    private Instant timestamp;
}

