package com.exchange.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class ExchangeEvent {
    private String tradingPair;
    private Instant timestamp;
}

