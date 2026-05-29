package com.exchange.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trades")
@Data
@NoArgsConstructor
public class TradeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tradeId;

    @Column(nullable = false)
    private String tradingPair;

    @Column(nullable = false)
    private String buyOrderId;

    @Column(nullable = false)
    private String sellOrderId;

    @Column(nullable = false)
    private Long buyUserId;

    @Column(nullable = false)
    private Long sellUserId;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false)
    private Instant executedAt;
}
