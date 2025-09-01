package com.sdlc.pro.txboard.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.sdlc.pro.txboard.actuator.TxBoardMicrometerMetrics;
import com.sdlc.pro.txboard.repository.TransactionLogRepository;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({PlatformTransactionManager.class, MeterRegistry.class})
@ConditionalOnProperty(
    name = {
        "sdlc.pro.spring.tx.board.enable",
        "sdlc.pro.spring.tx.board.actuator.enable"
    },
    havingValue = "true",
    matchIfMissing = true
)
public class SpringTxBoardMicrometerConfiguration {
    private static final Logger log = LoggerFactory.getLogger(SpringTxBoardMicrometerConfiguration.class);

    @Bean("sdlcProTxBoardMicrometerMetrics")
    @ConditionalOnBean(MeterRegistry.class)
    public TxBoardMicrometerMetrics txBoardMicrometerMetrics(MeterRegistry meterRegistry, 
                                                             TransactionLogRepository transactionLogRepository) {
        log.info("TX Board Micrometer metrics enabled at /actuator/metrics/txboard.*");
        return new TxBoardMicrometerMetrics(meterRegistry, transactionLogRepository);
    }
}
