package com.sdlc.pro.txboard.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.sdlc.pro.txboard.actuator.TxBoardActuatorEndpoint;
import com.sdlc.pro.txboard.repository.TransactionLogRepository;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({PlatformTransactionManager.class, Endpoint.class})
@ConditionalOnProperty(
    name = {
        "sdlc.pro.spring.tx.board.enable",
        "sdlc.pro.spring.tx.board.actuator.enable"
    },
    havingValue = "true",
    matchIfMissing = true
)
public class SpringTxBoardActuatorConfiguration {
    private static final Logger log = LoggerFactory.getLogger(SpringTxBoardActuatorConfiguration.class);

    @Bean("sdlcProTxBoardActuatorEndpoint")
    public TxBoardActuatorEndpoint txBoardActuatorEndpoint(TransactionLogRepository transactionLogRepository) {
        log.info("TX Board Actuator endpoint enabled at /actuator/txboard");
        return new TxBoardActuatorEndpoint(transactionLogRepository);
    }
}
