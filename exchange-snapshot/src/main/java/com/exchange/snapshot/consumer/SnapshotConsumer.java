package com.exchange.snapshot.consumer;

import com.exchange.common.enums.OrderSide;
import com.exchange.snapshot.service.SnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotConsumer {

    private final SnapshotService snapshotService;

    @KafkaListener(topics = "events-out", groupId = "snapshot-service")
    public void onEvent(ConsumerRecord<String, Map<String, Object>> record) {
        String tradingPair = record.key();
        Map<String, Object> data = record.value();

        if (data.containsKey("tradeId")) {
            snapshotService.handleTrade(
                    tradingPair,
                    new BigDecimal(data.get("price").toString()),
                    new BigDecimal(data.get("quantity").toString())
            );
            log.info("Snapshot updated after trade on {}", tradingPair);
        } else if (data.containsKey("remainingQuantity")) {
            snapshotService.handleOrderAccepted(
                    tradingPair,
                    OrderSide.valueOf((String) data.get("side")),
                    new BigDecimal(data.get("price").toString()),
                    new BigDecimal(data.get("remainingQuantity").toString())
            );
            log.info("Snapshot updated after order accepted on {}", tradingPair);
        } else if (data.containsKey("orderId") && !data.containsKey("remainingQuantity")) {
            snapshotService.handleOrderCancelled(tradingPair);
            log.info("Snapshot updated after order cancelled on {}", tradingPair);
        }
    }
}

