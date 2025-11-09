package com.sdlc.pro.txboard.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(classes = RedisTxBoardConfiguration.class)
@TestPropertySource("classpath:application-test.properties")
class RedisTxBoardConfigurationTest {

    @Autowired
    private Environment environment;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisTxBoardConfiguration.class))
            .withPropertyValues("sdlc.pro.spring.tx.board.enabled=true");

    @Test
    void shouldCreateRedisBeansWhenStorageIsRedis() {
        assumeTrue(
                "REDIS".equalsIgnoreCase(environment.getProperty("sdlc.pro.spring.tx.board.storage")),
                "Skipping RedisAutoConfigurationTest because storage is not REDIS"
        );

        contextRunner
                .withPropertyValues("sdlc.pro.spring.tx.board.storage=REDIS")
                .run(context -> {
                    assertThat(context).hasSingleBean(RedisConnectionFactory.class);
                    assertThat(context).hasSingleBean(RedisTemplate.class);
                });
    }

    @Test
    void shouldPingRedisWhenStorageIsRedis() {
        assumeTrue(
                "REDIS".equalsIgnoreCase(environment.getProperty("sdlc.pro.spring.tx.board.storage")),
                "Skipping RedisAutoConfigurationTest because storage is not REDIS"
        );

        contextRunner
                .withPropertyValues("sdlc.pro.spring.tx.board.storage=REDIS")
                .run(context -> {
                    RedisConnectionFactory factory = context.getBean(RedisConnectionFactory.class);
                    try (var connection = factory.getConnection()) {
                        String pong = connection.ping();
                        assertThat(pong).isEqualToIgnoringCase("PONG");
                    }
                });
    }
}