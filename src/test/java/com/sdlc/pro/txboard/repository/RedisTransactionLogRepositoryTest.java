package com.sdlc.pro.txboard.repository;

import com.google.gson.Gson;
import com.redis.om.spring.RedisModulesConfiguration;
import com.redis.om.spring.search.stream.EntityStream;
import com.sdlc.pro.txboard.config.GsonConfig;
import com.sdlc.pro.txboard.config.RedisOMConfiguration;
import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.domain.*;

import com.sdlc.pro.txboard.enums.IsolationLevel;
import com.sdlc.pro.txboard.enums.PropagationBehavior;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.DurationDistribution;
import com.sdlc.pro.txboard.model.DurationRange;
import com.sdlc.pro.txboard.model.TransactionLog;
import com.sdlc.pro.txboard.model.TransactionSummary;
import com.sdlc.pro.txboard.util.TxLogUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(classes = {
        RedisAutoConfiguration.class,
        RedisModulesConfiguration.class,
        RedisOMConfiguration.class,
        GsonConfig.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource("classpath:application-test.properties")
class RedisTransactionLogRepositoryTest {

    @Autowired
    ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Autowired
    ObjectProvider<TxRedisDocumentRepository> repositoryProvider;

    @Autowired
    ObjectProvider<EntityStream> entityProvider;

    @Autowired
    ObjectProvider<Gson> gsonProvider;

    @Autowired
    ObjectProvider<Environment> environmentProvider;

    RedisTransactionLogRepository logRepo;

    Environment environment;
    TxBoardProperties properties = new TxBoardProperties();

    @BeforeAll
    void setup() throws IOException {
        environment = environmentProvider.getIfAvailable();
        if(environment != null && !Objects.equals(environment.getProperty("sdlc.pro.spring.tx.board.storage"), "REDIS")){
            return;
        }

        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        TxRedisDocumentRepository redisRepository = repositoryProvider.getIfAvailable();
        EntityStream entityStream = entityProvider.getIfAvailable();
        Gson gson = gsonProvider.getIfAvailable();

        Set<String> keys = redisTemplate.keys("txboard:*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        logRepo = new RedisTransactionLogRepository(properties, redisTemplate, redisRepository, entityStream, gson);

        logRepo.deleteAll();
        for (TransactionLog transactionLog : TxLogUtils.createTestTransactionLogs()) {
            logRepo.save(transactionLog);
        }
    }

    @Test
    void testFindAll() {
        assumeTrue(Objects.equals(environment.getProperty("sdlc.pro.spring.tx.board.storage"), "REDIS"),
                "Skipping RedisAutoConfigurationTest because storage is not REDIS");

        List<TransactionLog> transactionLogs = logRepo.findAll();
        assertThat(transactionLogs).isNotNull();
        assertThat(transactionLogs.size()).isEqualTo(6);
    }


    @Test
    void testCount() {
        assumeTrue(Objects.equals(environment.getProperty("sdlc.pro.spring.tx.board.storage"), "REDIS"),
                "Skipping RedisAutoConfigurationTest because storage is not REDIS");

        long totalTransactionLog = logRepo.count();
        assertEquals(6L, totalTransactionLog);
    }

    @Test
    void testCountByTransactionStatus() {
        assumeTrue(Objects.equals(environment.getProperty("sdlc.pro.spring.tx.board.storage"), "REDIS"),
                "Skipping RedisAutoConfigurationTest because storage is not REDIS");

        long totalCommittedTransactionLog = logRepo.countByTransactionStatus(TransactionPhaseStatus.COMMITTED);
        long totalRolledBackTransactionLog = logRepo.countByTransactionStatus(TransactionPhaseStatus.ROLLED_BACK);
        long totalErroredTransactionLog = logRepo.countByTransactionStatus(TransactionPhaseStatus.ERRORED);

        assertEquals(4L, totalCommittedTransactionLog);
        assertEquals(1L, totalRolledBackTransactionLog);
        assertEquals(1L, totalErroredTransactionLog);
    }

    @Test
    void testAverageDuration() {
        assumeTrue(Objects.equals(environment.getProperty("sdlc.pro.spring.tx.board.storage"), "REDIS"),
                "Skipping RedisAutoConfigurationTest because storage is not REDIS");

        long averageDuration = (long) logRepo.averageDuration();
        assertEquals(919, averageDuration);
    }

    @Test
    void testFindAllWithFilter() {
        assumeTrue(Objects.equals(environment.getProperty("sdlc.pro.spring.tx.board.storage"), "REDIS"),
                "Skipping RedisAutoConfigurationTest because storage is not REDIS");

        int page = 0;
        int size = 2;
        String sort = null; //"startTime,desc";

        String search = null; //"OrderService.createOrder";

        String status = null; //"COMMITTED";

        String propagation = null; //"REQUIRES_NEW";

        String isolation = null; //"READ_COMMITTED";

        Boolean connectionOriented = null; //false;

        FilterNode filter = buildFilter(search, status, propagation, isolation, connectionOriented);

        var pageRequest = PageRequest.of(page, size, parseSort(sort), filter);

        PageResponse<TransactionLog> pageResponse = logRepo.findAll(pageRequest);

        assertThat(pageResponse).isNotNull();
        assertThat(pageResponse.getContent().size()).isEqualTo(2);
        assertThat(pageResponse.getTotalElements()).isEqualTo(6);
    }

    private Sort parseSort(String sort) {
        return sort != null ? Sort.from(sort) : Sort.UNSORTED;
    }

    private static FilterNode buildFilter(String search, String status, String propagation, String isolation, Boolean connectionOriented) {
        List<FilterNode> filters = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            filters.add(FilterGroup.of(
                    List.of(
                            Filter.of("method", search, Filter.Operator.CONTAINS),
                            Filter.of("thread", search, Filter.Operator.CONTAINS)
                    ),
                    FilterGroup.Logic.OR
            ));
        }

        if (status != null && !status.isBlank()) {
            try {
                filters.add(Filter.of("status", TransactionPhaseStatus.valueOf(status), Filter.Operator.EQUALS));
            } catch (Exception e) {
                throw new IllegalArgumentException("The value of status must be " + Arrays.toString(TransactionPhaseStatus.values()));
            }
        }

        if (propagation != null && !propagation.isBlank()) {
            try {
                filters.add(Filter.of("propagation", PropagationBehavior.valueOf(propagation), Filter.Operator.EQUALS));
            } catch (Exception e) {
                throw new IllegalArgumentException("The value of propagation must be " + Arrays.toString(PropagationBehavior.values()));
            }
        }

        if (isolation != null && !isolation.isBlank()) {
            try {
                filters.add(Filter.of("isolation", IsolationLevel.valueOf(isolation), Filter.Operator.EQUALS));
            } catch (Exception e) {
                throw new IllegalArgumentException("The value of isolation must be " + Arrays.toString(IsolationLevel.values()));
            }
        }

        if (connectionOriented != null) {
            filters.add(Filter.of("connectionOriented", connectionOriented, Filter.Operator.EQUALS));
        }

        return filters.isEmpty() ? FilterNode.UNFILTERED : FilterGroup.of(filters, FilterGroup.Logic.AND);
    }

    @Test
    void testTransactionSummary() {
        assumeTrue(Objects.equals(environment.getProperty("sdlc.pro.spring.tx.board.storage"), "REDIS"),
                "Skipping RedisAutoConfigurationTest because storage is not REDIS");

        TransactionSummary summary = logRepo.getTransactionSummary();
        assertEquals(3L, summary.getCommittedCount());
        assertEquals(1L, summary.getErroredCount());
        assertEquals(1L, summary.getRolledBackCount());
    }

    @Test
    void testDurationDistribution() {
        assumeTrue(Objects.equals(environment.getProperty("sdlc.pro.spring.tx.board.storage"), "REDIS"),
                "Skipping RedisAutoConfigurationTest because storage is not REDIS");

        List<DurationDistribution> durationDistributions = logRepo.getDurationDistributions();

        DurationRange range1 = new DurationRange(0, 100);
        DurationRange range2 = new DurationRange(100, 500);

        long range1Count = durationDistributions.stream().filter(r -> r.range().equals(range1)).findFirst().get().count();
        long range2Count = durationDistributions.stream().filter(r -> r.range().equals(range2)).findFirst().get().count();

        assertEquals(2L, range1Count);
        assertEquals(1L, range2Count);
    }
}