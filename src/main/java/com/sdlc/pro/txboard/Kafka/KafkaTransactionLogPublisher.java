package com.sdlc.pro.txboard.Kafka;

import com.sdlc.pro.txboard.listener.TransactionLogListener;
import com.sdlc.pro.txboard.model.TransactionLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

/**
 * Kafka publisher for TransactionLog events.
 * Implements fail-safe, non-blocking behavior to ensure Kafka publishing
 * failures never disrupt application logic or transaction execution.
 */
public final class KafkaTransactionLogPublisher implements TransactionLogListener {
    private static final Logger log = LoggerFactory.getLogger(KafkaTransactionLogPublisher.class);
    
    private final KafkaTemplate<String, TransactionLog> kafkaTemplate;
    private final TxBoardKafkaProperties properties;

    public KafkaTransactionLogPublisher(KafkaTemplate<String, TransactionLog> kafkaTemplate,
                                        TxBoardKafkaProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void listen(TransactionLog transactionLog) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            String key = generateKey(transactionLog);
            String topic = properties.getTopic();
            kafkaTemplate.send(topic, key, transactionLog);
        } catch (Exception ex) {
            // Fail-safe: log error but never disrupt application flow
            log.error("Failed to publish transaction log to Kafka topic '{}': {}", 
                    properties.getTopic(), ex.getMessage(), ex);
        }
    }

    private String generateKey(TransactionLog transactionLog) {
        // Use txId if available, otherwise generate a UUID
        // This method always returns a non-null String
        Integer txId = transactionLog.getTxId();
        if (txId != null) {
            return String.valueOf(txId);
        }
        return UUID.randomUUID().toString();
    }
}
