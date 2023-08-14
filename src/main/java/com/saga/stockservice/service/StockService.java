package com.saga.stockservice.service;

import com.saga.common.dto.OrchestratorResponseDTO;
import com.saga.common.dto.StockResponseDTO;
import com.saga.common.enums.OrderStatus;
import com.saga.common.enums.StockStatus;
import com.saga.stockservice.entity.Stock;
import com.saga.stockservice.producer.KafkaProducer;
import com.saga.stockservice.repository.StockRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StockService {
    @Autowired
    private StockRepository stockRepository;
    private final KafkaProducer kafkaProducer;

    @PostConstruct
    private void init() {
        stockRepository.save(new Stock(1, 5));
        stockRepository.save(new Stock(2, 5));
        stockRepository.save(new Stock(3, 5));
    }

    public StockService(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    private void deductStock(OrchestratorResponseDTO responseDTO) {
        StockResponseDTO stockResponseDTO = new StockResponseDTO();
        stockResponseDTO.setUserID(responseDTO.getUserID());
        stockResponseDTO.setOrderID(responseDTO.getOrderID());
        stockResponseDTO.setProductID(responseDTO.getProductID());

        Stock stock = stockRepository
                .findById(responseDTO.getProductID())
                .orElseThrow();
        int stockQuantity = stock.getQuantity();
        if (stockQuantity > 0) {
            stockResponseDTO.setStatus(StockStatus.AVAILABLE);
            stock.setQuantity(stockQuantity - 1);
            stockRepository.save(stock);
        } else {
            stockResponseDTO.setStatus(StockStatus.UNAVAILABLE);
        }
        kafkaProducer.sendStock(stockResponseDTO);
    }

    private void addStock(OrchestratorResponseDTO responseDTO) {
        Stock stock = stockRepository
                .findById(responseDTO.getProductID())
                .orElseThrow();
        if (stock.getQuantity() != 0) {
            stock.setQuantity(stock.getQuantity() + 1);
            stockRepository.save(stock);
        }
    }

    @KafkaListener(
            topics = "${topic.name.stock.out}",
            groupId = "${spring.kafka.consumer.stock-group-id}"
    )
    private void getOrchestratorMessage(OrchestratorResponseDTO responseDTO) {
        deductStock(responseDTO);
    }

    @KafkaListener(
            topics = "${topic.name.stock.cancel}",
            groupId = "stock"
    )
    private void getOrchestratorCancelMessage(OrchestratorResponseDTO responseDTO) {
        addStock(responseDTO);
    }
}
