package com.sdlc.pro.txboard.Kafka;

import com.sdlc.pro.txboard.listener.TransactionLogListener;
import com.sdlc.pro.txboard.model.TransactionLog;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Auto-configuration for Kafka integration in Spring Tx Board.
 * Automatically activates when spring-kafka is on the classpath and KafkaTemplate is available.
 * This enables seamless streaming of transaction logs to Kafka topics.
 */
@AutoConfiguration
@EnableConfigurationProperties(TxBoardKafkaProperties.class)
@ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
public class TxBoardKafkaAutoConfiguration {

    @Bean("sdlcProKafkaTransactionLogPublisher")
    @ConditionalOnBean(KafkaTemplate.class)
    public TransactionLogListener kafkaTransactionLogPublisher(
            KafkaTemplate<String, TransactionLog> kafkaTemplate,
            TxBoardKafkaProperties properties) {
        return new KafkaTransactionLogPublisher(kafkaTemplate, properties);
    }
}

