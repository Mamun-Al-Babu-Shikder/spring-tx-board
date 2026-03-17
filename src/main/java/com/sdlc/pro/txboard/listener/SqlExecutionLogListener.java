package com.sdlc.pro.txboard.listener;

import com.sdlc.pro.txboard.model.SqlExecutionLog;

public interface SqlExecutionLogListener {
    void listen(SqlExecutionLog sqlExecutionLog);
}
