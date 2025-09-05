package com.sdlc.pro.txboard.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdlc.pro.txboard.config.SpringTxBoardWebConfiguration;
import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.handler.AlarmingThresholdHttpHandler;
import com.sdlc.pro.txboard.handler.TransactionChartHttpHandler;
import com.sdlc.pro.txboard.handler.TransactionLogsHttpHandler;
import com.sdlc.pro.txboard.handler.TransactionSummaryHttpHandler;
import com.sdlc.pro.txboard.listener.TransactionLogListener;
import com.sdlc.pro.txboard.listener.TransactionLogPersistenceListener;
import com.sdlc.pro.txboard.repository.InMemoryTransactionLogRepository;
import com.sdlc.pro.txboard.repository.RedisTransactionLogRepository;
import com.sdlc.pro.txboard.repository.TransactionLogRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpringTxBoardAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringTxBoardAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void shouldNotAutoConfigWhenPlatformTransactionManagerClassMissing() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(PlatformTransactionManager.class))
                .run(context -> assertThat(context).doesNotHaveBean("sdlcProTxPhaseListener"));
    }

    @Test
    void shouldCreateTxBoardPropertiesWhenEnabled() {
        contextRunner
                .withPropertyValues("sdlc.pro.spring.tx.board.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(TxBoardProperties.class));
    }

    @Test
    void shouldCreateTransactionPhaseListenerWhenEnabled() {
        contextRunner
                .withPropertyValues("sdlc.pro.spring.tx.board.enabled=true")
                .run(context -> assertThat(context).hasBean("sdlcProTxPhaseListener"));
    }

    @Test
    void shouldCreateTransactionPhaseListenerWithoutWebContextIfNotConfigured() {
        contextRunner
                .withPropertyValues("sdlc.pro.spring.tx.board.enabled=true")
                .withClassLoader(new FilteredClassLoader(ObjectMapper.class, WebMvcConfigurer.class, HttpRequestHandler.class))
                .run(context -> assertThat(context).hasBean("sdlcProTxPhaseListener"));
    }

    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    class SpringTxBoardAutoConfigurationWithSpringTxBoardWebConfiguration {

        @Test
        void shouldCreateSpringTxBoardWebConfigurationWhenEnabled() {
            contextRunner
                    .withPropertyValues("sdlc.pro.spring.tx.board.enabled=true")
                    .run(context -> assertThat(context).hasSingleBean(SpringTxBoardWebConfiguration.class));
        }

        @Test
        void shouldCreateTransactionLogRepository() {
            contextRunner.run(context -> {
                assertThat(context).hasBean("sdlcProSpringTxLogRepository");
                assertThat(context.getBean("sdlcProSpringTxLogRepository")).isInstanceOf(TransactionLogRepository.class);
            });
        }

        @Test
        void defaultTransactionLogRepositoryShouldBeInMemoryTransactionLogRepository(CapturedOutput capturedOutput) {
            contextRunner.run(context -> {
                assertThat(context.getBean("sdlcProSpringTxLogRepository"))
                        .isInstanceOf(InMemoryTransactionLogRepository.class);

                assertThat(capturedOutput.getOut())
                        .contains("Spring Tx Board is configured to use IN_MEMORY storage for transaction logs.")
                        .contains("The InMemoryTransactionLogRepository has been created and initialized to support in-memory storage of transaction logs.");

            });
        }

        @Test
        void shouldCreateInMemoryTransactionLogRepository() {
            contextRunner
                    .withPropertyValues("sdlc.pro.spring.tx.board.storage=IN-MEMORY")
                    .run(context -> assertThat(context.getBean("sdlcProSpringTxLogRepository"))
                            .isInstanceOf(InMemoryTransactionLogRepository.class)
                    );
        }

        @Test
        void shouldCreateRedisTransactionLogRepository() {
            contextRunner
                    .withPropertyValues("sdlc.pro.spring.tx.board.storage=REDIS")
                    .run(context -> assertThat(context.getBean("sdlcProSpringTxLogRepository"))
                            .isInstanceOf(RedisTransactionLogRepository.class)
                    );
        }

        @Test
        void shouldCreateTransactionLogPersistenceListener() {
            contextRunner.run(context -> {
                TransactionLogListener logListener = context.getBean("sdlcProTransactionLogPersistenceListener", TransactionLogListener.class);
                assertThat(logListener).isInstanceOf(TransactionLogPersistenceListener.class);

                TransactionLogRepository repo = context.getBean(TransactionLogRepository.class);
                assertThat(logListener).extracting("repository").isEqualTo(repo);
            });
        }

        @Test
        void shouldCreateHandlerMappingWithAPIsHandler() {
            contextRunner.run(context -> {
                HandlerMapping handlerMapping = context.getBean("sdlcProTxBoardRestHandlerMapping", HandlerMapping.class);
                assertThat(handlerMapping).isNotNull();
                assertThat(handlerMapping).isInstanceOf(SimpleUrlHandlerMapping.class);
                Map<String, Object> urlMap = ((SimpleUrlHandlerMapping) handlerMapping).getHandlerMap();

                assertThat(urlMap.get("/api/spring-tx-board/config/alarming-threshold")).isInstanceOf(AlarmingThresholdHttpHandler.class);
                assertThat(urlMap.get("/api/spring-tx-board/tx-summary")).isInstanceOf(TransactionSummaryHttpHandler.class);
                assertThat(urlMap.get("/api/spring-tx-board/tx-logs")).isInstanceOf(TransactionLogsHttpHandler.class);
                assertThat(urlMap.get("/api/spring-tx-board/tx-charts")).isInstanceOf(TransactionChartHttpHandler.class);
            });
        }
    }
}
