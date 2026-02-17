package com.sdlc.pro.txboard.config;

import com.google.gson.*;
import com.sdlc.pro.txboard.controller.SpringTxBoardController;
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
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.http.CacheControl;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.reflect.Type;
import java.net.URI;
import java.time.Instant;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@Import({SpringTxBoardController.class})
public class SpringTxBoardWebConfiguration implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(SpringTxBoardWebConfiguration.class);
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean("sdlcProSpringTxLogRepository")
    @ConditionalOnMissingBean(TransactionLogRepository.class)
    public TransactionLogRepository transactionLogRepository(TxBoardProperties txBoardProperties) {
        TxBoardProperties.StorageType storageType = txBoardProperties.getStorage();
        log.info("Spring Tx Board is configured to use {} storage for transaction logs.", storageType);

        return switch (storageType) {
            case IN_MEMORY -> new InMemoryTransactionLogRepository(txBoardProperties);
            case REDIS -> new RedisTransactionLogRepository(this.resolveRedisJsonOperation(), txBoardProperties);
        };
    }

    @Bean("sdlcProSqlExecutionLogRepository")
    @ConditionalOnMissingBean(SqlExecutionLogRepository.class)
    public SqlExecutionLogRepository sqlExecutionLogRepository(TxBoardProperties txBoardProperties) {
        TxBoardProperties.StorageType storageType = txBoardProperties.getStorage();
        log.info("Spring Tx Board is configured to use {} storage for sql execution logs.", storageType);

        return switch (storageType) {
            case IN_MEMORY -> new InMemorySqlExecutionLogRepository();
            case REDIS -> new RedisSqlExecutionLogRepository(this.resolveRedisJsonOperation(), txBoardProperties);
        };
    }

    private RedisJsonOperation resolveRedisJsonOperation() {
        return this.applicationContext.getBean("sdlcProRedisJsonOperation", RedisJsonOperation.class);
    }

    @Bean
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnProperty(prefix = "sdlc.pro.spring.tx.board", name = "storage", havingValue = "redis", matchIfMissing = true)
    public RedisJsonOperation sdlcProRedisJsonOperation() {
        Gson mapper = gsonMapper();
        RedisJsonOperation redisJsonOperation;
        RedisConnectionFactory redisConnectionFactory = this.applicationContext.getBean(RedisConnectionFactory.class);
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

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class WebMvcConfig implements WebMvcConfigurer {
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

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public static class WebFluxConfig implements WebFluxConfigurer {
        @Override
        public void addResourceHandlers(org.springframework.web.reactive.config.ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/tx-board/ui/**")
                    .addResourceLocations("classpath:/META-INF/tx-board/ui/")
                    .setCacheControl(CacheControl.noCache());
        }

        @Bean
        public RouterFunction<ServerResponse> redirectUI() {
            return route(GET("/tx-board/ui"),
                    req -> ServerResponse.permanentRedirect(URI.create("/tx-board/ui/index.html")).build());
        }
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
