package com.sdlc.pro.txboard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdlc.pro.txboard.handler.TransactionChartHttpHandler;
import com.sdlc.pro.txboard.handler.TransactionLogsHttpHandler;
import com.sdlc.pro.txboard.handler.TransactionMetricsHttpHandler;
import com.sdlc.pro.txboard.listener.TransactionLogListener;
import com.sdlc.pro.txboard.listener.TransactionLogPersistenceListener;
import com.sdlc.pro.txboard.repository.InMemoryTransactionLogRepository;
import com.sdlc.pro.txboard.repository.RedisTransactionLogRepository;
import com.sdlc.pro.txboard.repository.TransactionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import java.util.Map;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({WebMvcConfigurer.class, HttpRequestHandler.class})
public class SpringTxBoardWebConfiguration implements WebMvcConfigurer {
    private static final int ORDER = 0;
    private static final Logger log = LoggerFactory.getLogger(SpringTxBoardWebConfiguration.class);

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

    @Bean("sdlcProTransactionLogPersistenceListener")
    public TransactionLogListener transactionLogPersistenceListener(TransactionLogRepository transactionLogRepository) {
        return new TransactionLogPersistenceListener(transactionLogRepository);
    }

    @Bean("sdlcProTxBoardRestHandlerMapping")
    public HandlerMapping txBoardRestHandlerMapping(ObjectMapper objectMapper, TransactionLogRepository transactionLogRepository) {
        return new SimpleUrlHandlerMapping(Map.of(
                "/api/tx-summary", new TransactionMetricsHttpHandler(objectMapper, transactionLogRepository),
                "/api/tx-logs", new TransactionLogsHttpHandler(objectMapper, transactionLogRepository),
                "/api/tx-charts", new TransactionChartHttpHandler(objectMapper, transactionLogRepository)
        ), ORDER);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/tx-board/ui/**")
                .addResourceLocations("classpath:/META-INF/tx-board/ui/")
                .setCachePeriod(0);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/tx-board/ui", "/tx-board/ui/index.html");
    }
}
