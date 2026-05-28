package com.exchange.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeEvent {
    private String tradeId;
    private String tradingPair;
    private String buyOrderId;
    private String sellOrderId;
    private Long buyUserId;
    private Long sellUserId;
    private BigDecimal price;
    private BigDecimal quantity;
    private Instant timestamp;
}

