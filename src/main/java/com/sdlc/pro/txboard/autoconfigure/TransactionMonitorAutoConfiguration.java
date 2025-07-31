package com.sdlc.pro.txboard.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sdlc.pro.txboard.config.TransactionBoardWebConfiguration;
import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.handler.TransactionChartHttpHandler;
import com.sdlc.pro.txboard.handler.TransactionLogsHttpHandler;
import com.sdlc.pro.txboard.handler.TransactionMetricsHttpHandler;
import com.sdlc.pro.txboard.listener.TransactionMonitoringListener;
import com.sdlc.pro.txboard.repository.InMemoryTransactionLogRepository;
import com.sdlc.pro.txboard.repository.RedisTransactionLogRepository;
import com.sdlc.pro.txboard.repository.TransactionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import java.util.Map;

@AutoConfiguration
@ConditionalOnClass({PlatformTransactionManager.class, WebMvcConfigurer.class, HttpRequestHandler.class})
@EnableConfigurationProperties(TxBoardProperties.class)
@ConditionalOnProperty(prefix = "sdlc.pro.spring.tx.board", name = "enable", havingValue = "true", matchIfMissing = true)
public class TransactionMonitorAutoConfiguration {
    private static final int ORDER = 0;
    private static final Logger log = LoggerFactory.getLogger(TransactionMonitorAutoConfiguration.class);

    @ConditionalOnClass(ObjectMapper.class)
    public Jackson2ObjectMapperBuilderCustomizer javaTimeModuleCustomizer() {
        return builder -> builder.modulesToInstall(JavaTimeModule.class);
    }

    @Bean("sdlcProSpringTxLogRepository")
    @ConditionalOnMissingBean(TransactionLogRepository.class)
    public TransactionLogRepository transactionLogRepository(TxBoardProperties txBoardProperties) {
        TxBoardProperties.StorageType storageType = txBoardProperties.getStorage();
        log.info("Spring Tx Board is configured to use {} storage for transaction logs.", storageType);

        return switch (storageType) {
            case IN_MEMORY -> new InMemoryTransactionLogRepository(txBoardProperties);
            case REDIS -> new RedisTransactionLogRepository();
        };
    }

    @ConditionalOnMissingBean
    @Bean("sdlcProTxMonitoringListener")
    public TransactionMonitoringListener transactionMonitoringListener(TransactionLogRepository transactionLogRepository, TxBoardProperties txBoardProperties) {
        return new TransactionMonitoringListener(transactionLogRepository, txBoardProperties);
    }

    @Bean("sdlcProTxBoardWebConfiguration")
    @ConditionalOnClass(WebMvcConfigurer.class)
    public TransactionBoardWebConfiguration transactionBoardWebConfiguration(TransactionLogRepository transactionLogRepository) {
        return new TransactionBoardWebConfiguration();
    }

    @Bean("sdlcProTxBoardRestHandlerMapping")
    public HandlerMapping txBoardRestHandlerMapping(ObjectMapper objectMapper, TransactionLogRepository transactionLogRepository) {
        return new SimpleUrlHandlerMapping(Map.of(
                "/api/tx-metrics", new TransactionMetricsHttpHandler(objectMapper, transactionLogRepository),
                "/api/tx-logs", new TransactionLogsHttpHandler(objectMapper, transactionLogRepository),
                "/api/tx-charts", new TransactionChartHttpHandler(objectMapper, transactionLogRepository)
        ), ORDER);
    }
}
