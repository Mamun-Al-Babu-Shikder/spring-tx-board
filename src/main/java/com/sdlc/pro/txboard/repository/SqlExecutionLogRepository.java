package com.sdlc.pro.txboard.repository;

import com.sdlc.pro.txboard.domain.PageRequest;
import com.sdlc.pro.txboard.domain.PageResponse;
import com.sdlc.pro.txboard.model.SqlExecutionLog;

public interface SqlExecutionLogRepository {
    void save(SqlExecutionLog sqlExecutionLog);

    PageResponse<SqlExecutionLog> findAll(PageRequest request);
}
