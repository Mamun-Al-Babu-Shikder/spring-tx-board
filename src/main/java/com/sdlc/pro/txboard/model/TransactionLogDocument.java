package com.sdlc.pro.txboard.model;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.Searchable;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.List;

@Document
public class TransactionLogDocument {

    @Id
    private String id;

    @Indexed
    private Integer txId;

    @Searchable
    private String method;

    @Indexed
    private String propagation;

    @Indexed
    private String isolation;

    @Indexed
    private long startTime;

    @Indexed
    private long endTime;

    @Indexed
    private long duration;

    private ConnectionSummary connectionSummary;

    @Indexed
    private String connectionOriented;

    @Indexed
    private String status;

    @Searchable
    private String thread;

    private List<String> executedQueries = new ArrayList<>();

    private List<TransactionLogDocument> child = new ArrayList<>();

    private List<TransactionEvent> events = new ArrayList<>();

    private boolean alarmingTransaction;

    private Boolean havingAlarmingConnection;

    private List<String> postTransactionQueries = new ArrayList<>();

    public TransactionLogDocument() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getConnectionOriented() {
        return connectionOriented;
    }

    public void setConnectionOriented(String connectionOriented) {
        this.connectionOriented = connectionOriented;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public List<String> getExecutedQueries() {
        return executedQueries;
    }

    public void setExecutedQueries(List<String> executedQueries) {
        this.executedQueries = executedQueries != null ? executedQueries : new ArrayList<>();
    }

    public List<TransactionLogDocument> getChild() {
        return child;
    }

    public void setChild(List<TransactionLogDocument> child) {
        this.child = child != null ? child : new ArrayList<>();
    }

    public List<TransactionEvent> getEvents() {
        return events;
    }

    public void setEvents(List<TransactionEvent> events) {
        this.events = events != null ? events : new ArrayList<>();
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

    public List<String> getPostTransactionQueries() {
        return postTransactionQueries;
    }

    public void setPostTransactionQueries(List<String> postTransactionQueries) {
        this.postTransactionQueries = postTransactionQueries != null ? postTransactionQueries : new ArrayList<>();
    }
}
