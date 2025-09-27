package com.sdlc.pro.txboard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sdlc.pro.txboard.handler.AlarmingThresholdHttpHandler;
import com.sdlc.pro.txboard.handler.TransactionChartHttpHandler;
import com.sdlc.pro.txboard.handler.TransactionLogsHttpHandler;
import com.sdlc.pro.txboard.handler.TransactionSummaryHttpHandler;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.Map;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({WebMvcConfigurer.class, HttpRequestHandler.class, ObjectMapper.class})
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
    public HandlerMapping txBoardRestHandlerMapping(ObjectProvider<Jackson2ObjectMapperBuilder> builderProvider,
                                                    ObjectProvider<ObjectMapper> objectMapperProvider,
                                                    TxBoardProperties txBoardProperties,
                                                    TransactionLogRepository transactionLogRepository) {
        // Prefer a Jackson2ObjectMapperBuilder when available so we respect user customizations
        // (modules, property naming strategies, etc). Otherwise use any ObjectMapper bean or
        // create a default one.
        Jackson2ObjectMapperBuilder builder = builderProvider.getIfAvailable();
        ObjectMapper objectMapper = (builder != null)
                ? builder.build()
                : objectMapperProvider.getIfAvailable(ObjectMapper::new);

        // Best-effort: register available modules (including JSR-310) and prefer ISO-8601
        try {
            objectMapper.findAndRegisterModules();
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            try {
                Class.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule");
            } catch (ClassNotFoundException e) {
                objectMapper.registerModule(new TxBoardFallbackJavaTimeModule());
                log.info("SpringTxBoard: registered fallback TxBoardFallbackJavaTimeModule because jackson-datatype-jsr310 was not found on the classpath");
            }
        } catch (NoClassDefFoundError | Exception ignored) {
            // ignore - best-effort only
        }

        return new SimpleUrlHandlerMapping(Map.of(
                "/api/spring-tx-board/config/alarming-threshold", new AlarmingThresholdHttpHandler(objectMapper, txBoardProperties.getAlarmingThreshold()),
                "/api/spring-tx-board/tx-summary", new TransactionSummaryHttpHandler(objectMapper, transactionLogRepository),
                "/api/spring-tx-board/tx-logs", new TransactionLogsHttpHandler(objectMapper, transactionLogRepository),
                "/api/spring-tx-board/tx-charts", new TransactionChartHttpHandler(objectMapper, transactionLogRepository)
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
