package com.sdlc.pro.txboard.model;

import com.sdlc.pro.txboard.redis.IndexFiled;
import com.sdlc.pro.txboard.redis.RedisEntity;
import com.sdlc.pro.txboard.redis.RedisId;

import java.io.Serializable;
import java.util.List;

import static com.sdlc.pro.txboard.redis.SchemaFieldType.NUMERIC;
import static com.sdlc.pro.txboard.redis.SchemaFieldType.TAG;

@RedisEntity(
        indexName = "spring_tx_board_sql_exe_log_idx",
        recordPrefix = "SpringTxBoardSqlExecutionLog"
)
public class RedisSqlExecutionLog implements Serializable {
    @RedisId
    private String id;
    @IndexFiled(schemaFieldType = NUMERIC, sortable = true)
    private long conAcquiredTime;
    @IndexFiled(schemaFieldType = NUMERIC, sortable = true)
    private long conReleaseTime;
    @IndexFiled(schemaFieldType = NUMERIC, sortable = true)
    private long conOccupiedTime;
    @IndexFiled(schemaFieldType = TAG)
    private boolean alarmingConnection;
    @IndexFiled(sortable = true)
    private String thread;
    private List<String> executedQuires;

    public RedisSqlExecutionLog() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getConAcquiredTime() {
        return conAcquiredTime;
    }

    public void setConAcquiredTime(long conAcquiredTime) {
        this.conAcquiredTime = conAcquiredTime;
    }

    public long getConReleaseTime() {
        return conReleaseTime;
    }

    public void setConReleaseTime(long conReleaseTime) {
        this.conReleaseTime = conReleaseTime;
    }

    public long getConOccupiedTime() {
        return conOccupiedTime;
    }

    public void setConOccupiedTime(long conOccupiedTime) {
        this.conOccupiedTime = conOccupiedTime;
    }

    public boolean isAlarmingConnection() {
        return alarmingConnection;
    }

    public void setAlarmingConnection(boolean alarmingConnection) {
        this.alarmingConnection = alarmingConnection;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public List<String> getExecutedQuires() {
        return executedQuires;
    }

    public void setExecutedQuires(List<String> executedQuires) {
        this.executedQuires = executedQuires;
    }
}
