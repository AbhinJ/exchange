package com.exchange.engine.orderbook;

import com.exchange.common.enums.OrderSide;
import com.exchange.common.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private String orderId;
    private Long userId;
    private String tradingPair;
    private OrderSide side;
    private OrderType type;
    private BigDecimal price;
    private BigDecimal remainingQuantity;
    private Instant timestamp;
}
