package com.exchange.persistence.repository;

import com.exchange.persistence.entity.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<TradeEntity, Long> {
}

