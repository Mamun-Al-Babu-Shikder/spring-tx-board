package com.sdlc.pro.txboard.repository;

import com.sdlc.pro.txboard.domain.FilterNode;
import com.sdlc.pro.txboard.domain.PageRequest;
import com.sdlc.pro.txboard.domain.PageResponse;
import com.sdlc.pro.txboard.model.SqlExecutionLog;
import com.sdlc.pro.txboard.util.FilterPredicateFactory;
import com.sdlc.pro.txboard.util.SortUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemorySqlExecutionLogRepository implements SqlExecutionLogRepository, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(InMemorySqlExecutionLogRepository.class);

    private final List<SqlExecutionLog> sqlExecutionLogs;

    public InMemorySqlExecutionLogRepository() {
        this.sqlExecutionLogs = new CopyOnWriteArrayList<>();
    }

    @Override
    public void save(SqlExecutionLog sqlExecutionLog) {
        Objects.requireNonNull(sqlExecutionLog, "Required non-null SqlExecutionLog");
        this.sqlExecutionLogs.add(sqlExecutionLog);
    }

    @Override
    public PageResponse<SqlExecutionLog> findAll(PageRequest pageRequest) {
        Objects.requireNonNull(pageRequest, "Required non-null PageRequest");
        List<SqlExecutionLog> logs = pageRequest.getFilter() == FilterNode.UNFILTERED ? this.sqlExecutionLogs :
                this.sqlExecutionLogs.stream()
                        .filter(FilterPredicateFactory.buildPredicate(pageRequest.getFilter()))
                        .toList();

        List<SqlExecutionLog> sortedLogs = SortUtils.sort(logs, pageRequest.getSort());
        List<SqlExecutionLog> content = getSqlExecutionLogPage(sortedLogs, pageRequest);

        int totalElements = sortedLogs.size();
        return new PageResponse<>(content, pageRequest, totalElements);
    }

    private static List<SqlExecutionLog> getSqlExecutionLogPage(List<SqlExecutionLog> logs, PageRequest pageRequest) {
        try {
            int start = pageRequest.getPageNumber() * pageRequest.getPageSize();
            int end = (pageRequest.getPageNumber() + 1) * pageRequest.getPageSize();
            return logs.subList(start, Math.min(end, logs.size()));
        } catch (IndexOutOfBoundsException | IllegalArgumentException ex) {
            return List.of();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("The InMemorySqlExecutionLogRepository has been created and initialized to support in-memory storage of sql execution logs.");
    }
}
