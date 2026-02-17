package com.sdlc.pro.txboard.repository;

import com.sdlc.pro.txboard.domain.Filter;
import com.sdlc.pro.txboard.domain.PageRequest;
import com.sdlc.pro.txboard.domain.PageResponse;
import com.sdlc.pro.txboard.domain.Sort;
import com.sdlc.pro.txboard.model.SqlExecutionLog;
import com.sdlc.pro.txboard.util.SqlLogUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemorySqlExecutionLogRepositoryTest {
    private static SqlExecutionLogRepository logRepository;

    @BeforeAll
    static void setup() {
        logRepository = new InMemorySqlExecutionLogRepository();
        for (SqlExecutionLog log : SqlLogUtils.createSqlExecutionLogs()) {
            logRepository.save(log);
        }
    }

    @Test
    void testFindAll() {
        PageResponse<SqlExecutionLog> pageResponse = logRepository.findAll(PageRequest.of(
                0, 10,
                Sort.by("conAcquiredTime", Sort.Direction.ASC)
        ));

        assertEquals(5, pageResponse.getTotalElements());
        assertEquals(0, pageResponse.getPage());
        assertEquals(10, pageResponse.getSize());
        assertEquals(1, pageResponse.getTotalPages());
        assertTrue(pageResponse.isFirst());
        assertTrue(pageResponse.isLast());
        assertFalse(pageResponse.hasNext());
        assertFalse(pageResponse.hasPrevious());

        List<SqlExecutionLog> executionLogs = pageResponse.getContent();
        assertEquals(5, executionLogs.size());

        SqlExecutionLog log = executionLogs.get(0);
        assertEquals(UUID.fromString("08c2c8ee-369f-4ac0-bd47-0763f964967a"), log.getId());
        assertEquals(Instant.parse("2026-02-17T10:45:00Z").plusMillis(10), log.getConAcquiredTime());
        assertEquals(Instant.parse("2026-02-17T10:45:00Z").plusMillis(130), log.getConReleaseTime());
        assertEquals(120, log.getConOccupiedTime());
        assertFalse(log.isAlarmingConnection());
        assertEquals("main", log.getThread());
        assertLinesMatch(List.of("select * from post"), log.getExecutedQuires());
    }

    @Test
    void testSearchByThread() {
        PageResponse<SqlExecutionLog> pageResponse = logRepository.findAll(PageRequest.of(
                0, 10,
                Sort.by("conAcquiredTime", Sort.Direction.ASC),
                Filter.of("thread", "main", Filter.Operator.CONTAINS)
        ));

        assertEquals(2, pageResponse.getTotalElements());

        List<SqlExecutionLog> executionLogs = pageResponse.getContent();
        assertEquals(2, executionLogs.size());
        assertEquals("main", executionLogs.get(0).getThread());
        assertEquals("main", executionLogs.get(1).getThread());
    }
}
