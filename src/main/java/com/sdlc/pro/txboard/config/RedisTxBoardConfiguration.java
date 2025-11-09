package com.sdlc.pro.txboard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sdlc.pro.txboard.model.TransactionLog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@ConditionalOnProperty(prefix = "sdlc.pro.spring.tx.board", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TxBoardProperties.class)
public class RedisTxBoardConfiguration {

    private final TxBoardProperties properties;

    public RedisTxBoardConfiguration(TxBoardProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        var redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(properties.getRedis().getHost());
        redisConfig.setPort(properties.getRedis().getPort());

        String password = properties.getRedis().getPassword();
        if (password != null && !password.isBlank()) {
            redisConfig.setPassword(RedisPassword.of(password));
        }

        return new LettuceConnectionFactory(redisConfig);
    }

    @Bean
    public RedisTemplate<String, TransactionLog> txRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, TransactionLog> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<TransactionLog> valueSerializer =
                new Jackson2JsonRedisSerializer<>(mapper, TransactionLog.class);

        template.setValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }
}