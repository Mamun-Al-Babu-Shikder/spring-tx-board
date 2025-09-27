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
        // (modules, property naming strategies, etc). Otherwise use any ObjectMapper bean, or
        // fall back to a plain new ObjectMapper that attempts to register available modules
        // (including the jsr310 JavaTimeModule) so java.time types are supported.
        ObjectMapper objectMapper = null;
        Jackson2ObjectMapperBuilder builder = builderProvider.getIfAvailable();
        if (builder != null) {
            objectMapper = builder.build();
            log.debug("SpringTxBoard: using ObjectMapper built from Jackson2ObjectMapperBuilder");
        } else {
            ObjectMapper provided = objectMapperProvider.getIfAvailable();
            if (provided != null) {
                log.debug("SpringTxBoard: cloning provided ObjectMapper and configuring fallback modules if necessary");
                try {
                    // Copy the provided mapper so we don't mutate application-configured instance
                    objectMapper = provided.copy();
                } catch (UnsupportedOperationException e) {
                    // Some ObjectMapper implementations may not support copy(); fall back to using the provided instance
                    objectMapper = provided;
                }
            } else {
                ObjectMapper m = new ObjectMapper();
                log.debug("SpringTxBoard: no ObjectMapper available, creating fallback ObjectMapper and attempting to register modules");
                try {
                    m.findAndRegisterModules();
                    m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                } catch (NoClassDefFoundError | Exception ignored) {
                    // fallback
                }
                objectMapper = m;
            }
        }

        // Ensure the resolved ObjectMapper supports Java 8+ date/time types by registering
        // any available modules and preferring ISO-8601 formatting. This is safe to call
        // on an ObjectMapper already configured by the application - it will simply
        // register missing modules if present on the classpath.
        try {
            objectMapper.findAndRegisterModules();
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            // If the standard JSR-310 module isn't present, register a tiny fallback module
            // so Instant and other java.time types serialize as ISO-8601 strings instead
            // of throwing an exception.
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
