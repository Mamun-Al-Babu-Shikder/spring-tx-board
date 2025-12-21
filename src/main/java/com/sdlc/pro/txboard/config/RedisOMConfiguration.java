package com.sdlc.pro.txboard.config;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

@ConditionalOnExpression(
        "${sdlc.pro.spring.tx.board.enabled:true} and '${sdlc.pro.spring.tx.board.storage:IN-MEMORY}' == 'REDIS'"
)
@Configuration
@EnableRedisDocumentRepositories(
        basePackages = {
                "com.sdlc.pro.txboard.model",
                "com.sdlc.pro.txboard.repository"
        }
)
@EnableConfigurationProperties(TxBoardProperties.class)
public class RedisOMConfiguration {
    private final TxBoardProperties properties;

    public RedisOMConfiguration(TxBoardProperties properties) {
        this.properties = properties;
    }

    @Primary
    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        String host = properties.getRedis().getHost();
        int port = properties.getRedis().getPort();
        String password = properties.getRedis().getPassword();
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        config.setPassword(password);

        return new JedisConnectionFactory(config);
    }
}