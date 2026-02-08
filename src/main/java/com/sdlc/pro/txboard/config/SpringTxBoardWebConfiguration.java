package com.sdlc.pro.txboard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import com.sdlc.pro.txboard.handler.*;
import com.sdlc.pro.txboard.listener.SqlExecutionLogListener;
import com.sdlc.pro.txboard.listener.SqlExecutionLogPersistenceListener;
import com.sdlc.pro.txboard.listener.TransactionLogListener;
import com.sdlc.pro.txboard.listener.TransactionLogPersistenceListener;
import com.sdlc.pro.txboard.redis.JedisJsonOperation;
import com.sdlc.pro.txboard.redis.LettuceJsonOperation;
import com.sdlc.pro.txboard.redis.RedisJsonOperation;
import com.sdlc.pro.txboard.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({WebMvcConfigurer.class, HttpRequestHandler.class})
public class SpringTxBoardWebConfiguration implements ApplicationContextAware, WebMvcConfigurer {
    private static final int ORDER = 0;
    private static final Logger log = LoggerFactory.getLogger(SpringTxBoardWebConfiguration.class);
    private ApplicationContext applicationContext;

    @Bean("sdlcProSpringTxLogRepository")
    @ConditionalOnMissingBean(TransactionLogRepository.class)
    public TransactionLogRepository transactionLogRepository(TxBoardProperties txBoardProperties) {
        TxBoardProperties.StorageType storageType = txBoardProperties.getStorage();
        log.info("Spring Tx Board is configured to use {} storage for transaction logs.", storageType);

        switch (storageType) {
            case IN_MEMORY:
                return new InMemoryTransactionLogRepository(txBoardProperties);
            case REDIS:
                return new RedisTransactionLogRepository(this.resolveRedisJsonOperation(), txBoardProperties);
            default:
                throw new IllegalStateException("Unsupported storage type: " + storageType);
        }
    }

    @Bean("sdlcProSqlExecutionLogRepository")
    @ConditionalOnMissingBean(SqlExecutionLogRepository.class)
    public SqlExecutionLogRepository sqlExecutionLogRepository(TxBoardProperties txBoardProperties) {
        TxBoardProperties.StorageType storageType = txBoardProperties.getStorage();
        log.info("Spring Tx Board is configured to use {} storage for sql execution logs.", storageType);

        switch (storageType) {
            case IN_MEMORY: return new InMemorySqlExecutionLogRepository();
            case REDIS: return new RedisSqlExecutionLogRepository(this.resolveRedisJsonOperation(), txBoardProperties);
            default: throw new IllegalStateException("Unsupported storage type: " + storageType);
        }
    }

    private RedisJsonOperation resolveRedisJsonOperation() {
        return this.applicationContext.getBean("sdlcProRedisJsonOperation", RedisJsonOperation.class);
    }

    @Bean
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnProperty(prefix = "sdlc.pro.spring.tx.board", name = "storage", havingValue = "redis", matchIfMissing = true)
    public RedisJsonOperation sdlcProRedisJsonOperation(RedisConnectionFactory redisConnectionFactory) {
        Gson mapper = gsonMapper();
        RedisJsonOperation redisJsonOperation;
        if (redisConnectionFactory instanceof JedisConnectionFactory) {
            redisJsonOperation = new JedisJsonOperation(redisConnectionFactory, mapper);
        } else if (redisConnectionFactory instanceof LettuceConnectionFactory) {
            redisJsonOperation = new LettuceJsonOperation(redisConnectionFactory, mapper);
        } else {
            throw new IllegalArgumentException("Found unsupported RedisConnectionFactory instance. Required type must be JedisConnectionFactory or LettuceConnectionFactory");
        }

        return redisJsonOperation;
    }

    private Gson gsonMapper() {
        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantAdapter())
                .serializeNulls()
                .create();
    }

    @Bean("sdlcProTransactionLogPersistenceListener")
    public TransactionLogListener transactionLogPersistenceListener(TransactionLogRepository transactionLogRepository) {
        return new TransactionLogPersistenceListener(transactionLogRepository);
    }

    @Bean("sdlcProSqlExecutionLogPersistenceListener")
    public SqlExecutionLogListener sqlExecutionLogRepositoryPersistenceListener(SqlExecutionLogRepository sqlExecutionLogRepository) {
        return new SqlExecutionLogPersistenceListener(sqlExecutionLogRepository);
    }

    @Bean("sdlcProTxBoardRestHandlerMapping")
    public HandlerMapping txBoardRestHandlerMapping(ObjectMapper objectMapper, TxBoardProperties txBoardProperties,
                                                    TransactionLogRepository transactionLogRepository,
                                                    SqlExecutionLogRepository sqlExecutionLogRepository) {
        Map<String, Object> urlMap = new HashMap<>();
        urlMap.put("/api/tx-board/config/alarming-threshold", new AlarmingThresholdHttpHandler(objectMapper, txBoardProperties));
        urlMap.put("/api/tx-board/tx-summary", new TransactionMetricsHttpHandler(objectMapper, transactionLogRepository));
        urlMap.put("/api/tx-board/tx-logs", new TransactionLogsHttpHandler(objectMapper, transactionLogRepository));
        urlMap.put("/api/tx-board/sql-logs", new SqlExecutionLogHttpHandler(objectMapper, sqlExecutionLogRepository));
        urlMap.put("/api/tx-board/tx-charts", new TransactionChartHttpHandler(objectMapper, transactionLogRepository));
        return new SimpleUrlHandlerMapping(urlMap, ORDER);
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private static class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            return Instant.parse(json.getAsString());
        }
    }
}
