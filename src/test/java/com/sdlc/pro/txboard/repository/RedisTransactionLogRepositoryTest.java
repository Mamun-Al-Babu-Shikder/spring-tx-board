package com.sdlc.pro.txboard.repository;

import com.google.gson.*;
import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.domain.PageRequest;
import com.sdlc.pro.txboard.domain.PageResponse;
import com.sdlc.pro.txboard.domain.Sort;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.RedisTransactionLog;
import com.sdlc.pro.txboard.model.TransactionEvent;
import com.sdlc.pro.txboard.model.TransactionLog;
import com.sdlc.pro.txboard.model.TransactionSummary;
import com.sdlc.pro.txboard.redis.LettuceJsonOperation;
import com.sdlc.pro.txboard.redis.RedisJsonOperation;
import com.sdlc.pro.txboard.util.TxLogUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Note: Before run this test please remove previous RedisTransactionLog(s) from Redis server.
 * You can find those log record with prefix 'SpringTxBoardTransactionLog' and
 * the index name is 'spring_tx_board_transaction_log_idx'
 */

class RedisTransactionLogRepositoryTest {
    private static List<TransactionLog> transactionLogs;
    private static LettuceConnectionFactory lettuceConnectionFactory;
    private static TransactionLogRepository logRepository;

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

    @BeforeAll
    static void setup() {
        TxBoardProperties properties = new TxBoardProperties();
        properties.getRedis().setEntityTtl(Duration.ofSeconds(10));
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new RedisTransactionLogRepositoryTest.InstantAdapter())
                .serializeNulls()
                .create();
        lettuceConnectionFactory = new LettuceConnectionFactory();
        lettuceConnectionFactory.start();
        RedisJsonOperation redisJsonOperation = new LettuceJsonOperation(lettuceConnectionFactory, gson);
        redisJsonOperation.registerRedisEntityClass(RedisTransactionLog.class);
        redisJsonOperation.createIndex(RedisTransactionLog.class);
        logRepository = new RedisTransactionLogRepository(redisJsonOperation, properties);
        transactionLogs = TxLogUtils.createTestTransactionLogs();
        for (TransactionLog transactionLog : transactionLogs) {
            logRepository.save(transactionLog);
        }
    }

    @AfterAll
    static void cleanup() {
        lettuceConnectionFactory.stop();
    }

    @Test
    void testTransactionSummary() {
        TransactionSummary transactionSummary = logRepository.getTransactionSummary();
        assertEquals(6L, transactionSummary.getTotalTransaction());
        assertEquals(4L, transactionSummary.getCommittedCount());
        assertEquals(1L, transactionSummary.getRolledBackCount());
        assertEquals(1L, transactionSummary.getErroredCount());
        // assertEquals(0L, transactionSummary.getAlarmingCount()); // currently this is not required
        assertEquals(4L, transactionSummary.getConnectionAcquisitionCount());
        // assertEquals(1L, transactionSummary.getAlarmingConnectionCount()); // currently this is not required

        assertEquals(5515L, transactionSummary.getTotalDuration());
        assertEquals(919.0, Math.floor(transactionSummary.getAverageDuration()));
        assertEquals(5500L, transactionSummary.getTotalConnectionOccupiedTime());
        assertEquals(1375.0, transactionSummary.getAverageConnectionOccupiedTime());
    }

    @Test
    void testCount() {
        long totalTransactionLog = logRepository.count();
        assertEquals(6L, totalTransactionLog);
    }

    @Test
    void testCountByTransactionStatus() {
        long committedCount = logRepository.countByTransactionStatus(TransactionPhaseStatus.COMMITTED);
        long rolledBackCount = logRepository.countByTransactionStatus(TransactionPhaseStatus.ROLLED_BACK);
        long erroredCount = logRepository.countByTransactionStatus(TransactionPhaseStatus.ERRORED);

        assertEquals(4L, committedCount);
        assertEquals(1L, rolledBackCount);
        assertEquals(1L, erroredCount);
    }

    @Test
    void testPaginationSupportedFindAllMethod() {
        PageResponse<TransactionLog> response = logRepository.findAll(PageRequest.of(
                0, 10,
                Sort.by("startTime", Sort.Direction.DESC))
        );

        transactionLogs.sort((a, b) -> Long.compare(b.getStartTime().toEpochMilli(), a.getStartTime().toEpochMilli()));
        List<TransactionLog> logs = response.getContent();

        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(1, response.getTotalPages());
        assertEquals(6, response.getTotalElements());
        assertEquals(transactionLogs.size(), logs.size());
        for (int i = 0; i < transactionLogs.size(); i++) {
            assertTrue(equals(transactionLogs.get(i), logs.get(i)));
        }
    }

    private boolean equals(TransactionLog a, TransactionLog b) {
        if (a == b) {
            return true;
        }

        if (!(Objects.equals(a.getTxId(), b.getTxId()) &&
                a.getMethod().equals(b.getMethod()) &&
                a.getThread().equals(b.getThread()) &&
                a.getPropagation() == b.getPropagation() &&
                a.getIsolation() == b.getIsolation() &&
                a.getStatus() == b.getStatus() &&
                a.getStartTime().equals(b.getStartTime()) &&
                a.getEndTime().equals(b.getEndTime()) &&
                a.getDuration() == b.getDuration() &&
                a.isAlarmingTransaction() == b.isAlarmingTransaction() &&
                a.getConnectionOriented() == b.getConnectionOriented() &&
                a.getHavingAlarmingConnection() == b.getHavingAlarmingConnection() &&
                a.getTotalQueryCount() == b.getTotalQueryCount() &&
                a.getTotalTransactionCount() == b.getTotalTransactionCount() &&
                Objects.equals(a.getExecutedQuires(), b.getExecutedQuires()) &&
                Objects.equals(a.getPostTransactionQuires(), b.getPostTransactionQuires()))) {
            return false;
        }

        // check the event are same or not
        List<TransactionEvent> e1 = a.getEvents();
        List<TransactionEvent> e2 = b.getEvents();
        if ((e1 == null && e2 != null) || (e1 != null && e2 == null)) {
            return false;
        }

        if (e1 != null) {
            if (e1.size() != e2.size()) {
                return false;
            }

            for (int i = 0; i < e1.size(); i++) {
                if (!equals(e1.get(i), e2.get(i))) {
                    return false;
                }
            }
        }

        // check the child are same or not
        List<TransactionLog> c1 = a.getChild();
        List<TransactionLog> c2 = b.getChild();
        if ((c1 == null && c2 != null) || (c1 != null && c2 == null)) {
            return false;
        }

        if (c1 != null) {
            if (c1.size() != c2.size()) {
                return false;
            }

            for (int i = 0; i < c1.size(); i++) {
                if (!equals(c1.get(i), c2.get(i))) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean equals(TransactionEvent a, TransactionEvent b) {
        if (a == b) {
            return true;
        }

        return a.getType() == b.getType() &&
                a.getTimestamp().toEpochMilli() == b.getTimestamp().toEpochMilli() &&
                a.getDetails().equals(b.getDetails());
    }
}
