package com.sdlc.pro.txboard.model;

import com.sdlc.pro.txboard.redis.IndexFiled;
import com.sdlc.pro.txboard.redis.RedisEntity;
import com.sdlc.pro.txboard.redis.RedisId;

import java.io.Serializable;
import java.util.List;

import static com.sdlc.pro.txboard.redis.SchemaFieldType.*;

@RedisEntity(
        indexName = "spring_tx_board_transaction_log_idx",
        recordPrefix = "SpringTxBoardTransactionLog"
)
public class RedisTransactionLog implements Serializable {
    @RedisId
    private Integer txId;
    @IndexFiled(sortable = true)
    private String method;
    @IndexFiled(schemaFieldType = TAG, sortable = true)
    private String propagation;
    @IndexFiled(schemaFieldType = TAG, sortable = true)
    private String isolation;
    @IndexFiled(schemaFieldType = NUMERIC, sortable = true)
    private long startTime;
    private long endTime;
    @IndexFiled(schemaFieldType = NUMERIC, sortable = true)
    private long duration;
    @IndexFiled(schemaFieldType = NESTED)
    private ConnectionSummary connectionSummary;
    @IndexFiled(schemaFieldType = TAG)
    private Boolean connectionOriented;
    @IndexFiled(schemaFieldType = TAG, sortable = true)
    private String status;
    @IndexFiled(sortable = true)
    private String thread;
    private List<String> executedQuires;
    private List<RedisTransactionLog> child;
    private List<TransactionEvent> events;
    @IndexFiled(schemaFieldType = TAG)
    private boolean alarmingTransaction;
    private Boolean havingAlarmingConnection;
    private List<String> postTransactionQuires;

    public RedisTransactionLog() {

    }

    public Integer getTxId() {
        return txId;
    }

    public void setTxId(Integer txId) {
        this.txId = txId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPropagation() {
        return propagation;
    }

    public void setPropagation(String propagation) {
        this.propagation = propagation;
    }

    public String getIsolation() {
        return isolation;
    }

    public void setIsolation(String isolation) {
        this.isolation = isolation;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public ConnectionSummary getConnectionSummary() {
        return connectionSummary;
    }

    public void setConnectionSummary(ConnectionSummary connectionSummary) {
        this.connectionSummary = connectionSummary;
    }

    public Boolean getConnectionOriented() {
        return connectionOriented;
    }

    public void setConnectionOriented(Boolean connectionOriented) {
        this.connectionOriented = connectionOriented;
    }

    public String getStatus() {
        return status;
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

    public List<RedisTransactionLog> getChild() {
        return child;
    }

    public void setChild(List<RedisTransactionLog> child) {
        this.child = child;
    }

    public List<TransactionEvent> getEvents() {
        return events;
    }

    public void setEvents(List<TransactionEvent> events) {
        this.events = events;
    }

    public boolean isAlarmingTransaction() {
        return alarmingTransaction;
    }

    public void setAlarmingTransaction(boolean alarmingTransaction) {
        this.alarmingTransaction = alarmingTransaction;
    }

    public Boolean getHavingAlarmingConnection() {
        return havingAlarmingConnection;
    }

    public void setHavingAlarmingConnection(Boolean havingAlarmingConnection) {
        this.havingAlarmingConnection = havingAlarmingConnection;
    }

    public List<String> getPostTransactionQuires() {
        return postTransactionQuires;
    }

    public void setPostTransactionQuires(List<String> postTransactionQuires) {
        this.postTransactionQuires = postTransactionQuires;
    }
}
