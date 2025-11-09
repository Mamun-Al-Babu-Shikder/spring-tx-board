package com.sdlc.pro.txboard.repository;

import com.sdlc.pro.txboard.config.RedisTxBoardConfiguration;
import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.domain.TransactionLogPageRequest;
import com.sdlc.pro.txboard.domain.TransactionLogPageResponse;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.DurationDistribution;
import com.sdlc.pro.txboard.model.DurationRange;
import com.sdlc.pro.txboard.model.TransactionLog;
import com.sdlc.pro.txboard.model.TransactionSummary;
import com.sdlc.pro.txboard.util.TxLogUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(classes = {
        RedisTxBoardConfiguration.class,
        RedisTransactionLogRepository.class
})
@TestPropertySource("classpath:application-test.properties")
class RedisTransactionLogRepositoryTest {
    private static TransactionLogRepository logRepository;
    static TxBoardProperties properties = new TxBoardProperties();

    @Autowired
    private Environment environment;

    private static final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisTxBoardConfiguration.class))
            .withPropertyValues(
                    "sdlc.pro.spring.tx.board.enabled=true",
                    "sdlc.pro.spring.tx.board.storage=REDIS",
                    "sdlc.pro.spring.tx.board.redis.host=localhost",
                    "sdlc.pro.spring.tx.board.redis.port=16379"
            );

    @BeforeAll
    static void setup() {
        if(properties.getStorage() == TxBoardProperties.StorageType.REDIS){
            contextRunner.run(context -> {
                RedisTemplate<String, TransactionLog> txRedisTemplate = context.getBean(RedisTemplate.class);
                logRepository = new RedisTransactionLogRepository(txRedisTemplate, properties);
                logRepository.deleteAll();
                for (TransactionLog transactionLog : TxLogUtils.createTestTransactionLogs()) {
                    logRepository.save(transactionLog);
                }
            });
        }
    }

    @Test
    void testFindAll() {
        assumeTrue(
                "REDIS".equalsIgnoreCase(environment.getProperty("sdlc.pro.spring.tx.board.storage")),
                "Skipping RedisAutoConfigurationTest because storage is not REDIS"
        );

        contextRunner.run(context -> {
            RedisTemplate<String, TransactionLog> txRedisTemplate = context.getBean(RedisTemplate.class);
            logRepository = new RedisTransactionLogRepository(txRedisTemplate, properties);
            List<TransactionLog> transactionLogs = logRepository.findAll();
            assertThat(transactionLogs).isNotNull();
            assertThat(transactionLogs.size()).isEqualTo(6);
        });
    }

    @Test
    void testCount() {
        assumeTrue(
                "REDIS".equalsIgnoreCase(environment.getProperty("sdlc.pro.spring.tx.board.storage")),
                "Skipping RedisAutoConfigurationTest because storage is not REDIS"
        );

        contextRunner.run(context -> {
            RedisTemplate<String, TransactionLog> txRedisTemplate = context.getBean(RedisTemplate.class);
            logRepository = new RedisTransactionLogRepository(txRedisTemplate, properties);
            long totalTransactionLog = logRepository.count();
            assertEquals(6L, totalTransactionLog);
        });
    }

    @Test
    void testCountByTransactionStatus() {
        assumeTrue(
                "REDIS".equalsIgnoreCase(environment.getProperty("sdlc.pro.spring.tx.board.storage")),
                "Skipping RedisAutoConfigurationTest because storage is not REDIS"
        );

        contextRunner.run(context -> {
            RedisTemplate<String, TransactionLog> txRedisTemplate = context.getBean(RedisTemplate.class);
            logRepository = new RedisTransactionLogRepository(txRedisTemplate, properties);
            long totalCommittedTransactionLog = logRepository.countByTransactionStatus(TransactionPhaseStatus.COMMITTED);
            long totalRolledBackTransactionLog = logRepository.countByTransactionStatus(TransactionPhaseStatus.ROLLED_BACK);
            long totalErroredTransactionLog = logRepository.countByTransactionStatus(TransactionPhaseStatus.ERRORED);

            assertEquals(4L, totalCommittedTransactionLog);
            assertEquals(1L, totalRolledBackTransactionLog);
            assertEquals(1L, totalErroredTransactionLog);
        });
    }

    @Test
    void testAverageDuration() {
        assumeTrue(
                "REDIS".equalsIgnoreCase(environment.getProperty("sdlc.pro.spring.tx.board.storage")),
                "Skipping RedisAutoConfigurationTest because storage is not REDIS"
        );

        contextRunner.run(context -> {
            RedisTemplate<String, TransactionLog> txRedisTemplate = context.getBean(RedisTemplate.class);
            logRepository = new RedisTransactionLogRepository(txRedisTemplate, properties);
            int averageDuration = (int) logRepository.averageDuration();
            assertEquals(919, averageDuration);
        });
    }

    @Test
    void testFindAllWithPagination() {
        assumeTrue(
                "REDIS".equalsIgnoreCase(environment.getProperty("sdlc.pro.spring.tx.board.storage")),
                "Skipping RedisAutoConfigurationTest because storage is not REDIS"
        );

        contextRunner.run(context -> {
            RedisTemplate<String, TransactionLog> txRedisTemplate = context.getBean(RedisTemplate.class);
            logRepository = new RedisTransactionLogRepository(txRedisTemplate, properties);
            TransactionLogPageRequest pageRequest = TransactionLogPageRequest.of(1, 2);
            TransactionLogPageResponse pageResponse = logRepository.findAll(pageRequest);
            assertThat(pageResponse).isNotNull();
            assertThat(pageResponse.getContent().size()).isEqualTo(2);
            assertThat(pageResponse.getTotalElements()).isEqualTo(6);
        });
    }

    @Test
    void testGetTransactionSummary() {
        assumeTrue(
                "REDIS".equalsIgnoreCase(environment.getProperty("sdlc.pro.spring.tx.board.storage")),
                "Skipping RedisAutoConfigurationTest because storage is not REDIS"
        );

        contextRunner.run(context -> {
            RedisTemplate<String, TransactionLog> txRedisTemplate = context.getBean(RedisTemplate.class);
            logRepository = new RedisTransactionLogRepository(txRedisTemplate, properties);
            TransactionSummary summary = logRepository.getTransactionSummary();
            assertThat(summary).isNotNull();
            assertThat(summary.getCommittedCount()).isEqualTo(4L);
            assertThat(summary.getRolledBackCount()).isEqualTo(1L);
            assertThat(summary.getErroredCount()).isEqualTo(1L);
            assertThat(summary.getTotalDuration()).isEqualTo(5515L);
        });
    }

    @Test
    void testGetDurationDistribution() {
        assumeTrue(
                "REDIS".equalsIgnoreCase(environment.getProperty("sdlc.pro.spring.tx.board.storage")),
                "Skipping RedisAutoConfigurationTest because storage is not REDIS"
        );

        contextRunner.run(context -> {
            RedisTemplate<String, TransactionLog> txRedisTemplate = context.getBean(RedisTemplate.class);
            logRepository = new RedisTransactionLogRepository(txRedisTemplate, properties);
            List<DurationDistribution> durationDistributions = logRepository.getDurationDistributions();
            assertThat(durationDistributions).isNotNull();
            assertThat(durationDistributions.size()).isEqualTo(properties.getDurationBuckets().size());

            assertThat(
                    durationDistributions.stream()
                            .filter(e -> Objects.equals(e.range(), new DurationRange(0, 100)))
                            .findFirst()
                            .map(DurationDistribution::count)
                            .orElse(0L)
            ).isEqualTo(2);

            assertThat(
                    durationDistributions.stream()
                            .filter(e -> Objects.equals(e.range(), new DurationRange(100, 500)))
                            .findFirst()
                            .map(DurationDistribution::count)
                            .orElse(0L)
            ).isEqualTo(1);
        });
    }

    @AfterAll
    static void testDeleteAll() {
        if(properties.getStorage() == TxBoardProperties.StorageType.REDIS) {
            contextRunner.run(context -> {
                RedisTemplate<String, TransactionLog> txRedisTemplate = context.getBean(RedisTemplate.class);
                logRepository = new RedisTransactionLogRepository(txRedisTemplate, properties);
                logRepository.deleteAll();
                long totalTransactionLog = logRepository.count();
                assertEquals(0L, totalTransactionLog);
            });
        }
    }
}