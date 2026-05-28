package com.exchange.common.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderCommand {
    private String orderId;
    private Long userId;
    private String tradingPair;
    private Instant timestamp;
}
