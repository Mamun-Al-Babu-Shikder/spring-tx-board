package com.sdlc.pro.txboard.Kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Kafka integration in Spring Tx Board.
 * Properties are prefixed with 'sdlc.pro.spring.tx.board.kafka' to maintain consistency
 * with the main TxBoardProperties.
 */
@ConfigurationProperties(prefix = "sdlc.pro.spring.tx.board.kafka")
public class TxBoardKafkaProperties {

    /**
     * Enable/disable Kafka publishing of transaction logs.
     * Defaults to true when Kafka is available on classpath.
     */
    private boolean enabled = true;

    /**
     * Kafka topic name for publishing transaction logs.
     * Defaults to 'tx-board-transactions'.
     */
    private String topic = "tx-board-transactions";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}

