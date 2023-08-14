package com.saga.stockservice.producer;

import com.saga.common.dto.OrchestratorResponseDTO;
import com.saga.common.dto.StockResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaProducer {
    private final KafkaTemplate<String, StockResponseDTO> kafkaTemplate;
    private final String stockInTopic;

    public KafkaProducer(KafkaTemplate<String, StockResponseDTO> kafkaTemplate,
                         @Value("${topic.name.stock.in}") String stockInTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.stockInTopic = stockInTopic;
    }

    public void sendStock(StockResponseDTO payload) {
        log.info("sending to stock topic={}, payload={}", stockInTopic, payload);
        kafkaTemplate.send(stockInTopic, payload);
    }
}
