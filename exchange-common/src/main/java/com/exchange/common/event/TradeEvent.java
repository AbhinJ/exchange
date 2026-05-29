package com.exchange.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class TradeEvent extends ExchangeEvent{
    private String tradeId;
    private String buyOrderId;
    private String sellOrderId;
    private Long buyUserId;
    private Long sellUserId;
    private BigDecimal price;
    private BigDecimal quantity;

    public TradeEvent(String tradeId, String tradingPair, String buyOrderId,
                      String sellOrderId, Long buyUserId, Long sellUserId,
                      BigDecimal price, BigDecimal quantity, Instant timestamp) {
        super(tradingPair, timestamp);
        this.tradeId = tradeId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.buyUserId = buyUserId;
        this.sellUserId = sellUserId;
        this.price = price;
        this.quantity = quantity;
    }
}

