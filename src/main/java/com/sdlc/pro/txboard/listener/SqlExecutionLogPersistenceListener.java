package com.sdlc.pro.txboard.listener;

import com.sdlc.pro.txboard.model.SqlExecutionLog;
import com.sdlc.pro.txboard.repository.SqlExecutionLogRepository;

public class SqlExecutionLogPersistenceListener implements SqlExecutionLogListener {
    private final SqlExecutionLogRepository repository;

    public SqlExecutionLogPersistenceListener(SqlExecutionLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void listen(SqlExecutionLog sqlExecutionLog) {
        this.repository.save(sqlExecutionLog);
    }
}
