package com.sdlc.pro.txboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ConfigurationProperties(prefix = "sdlc.pro.spring.tx.board")
public class TxBoardProperties {
    private boolean enable = true;
    private AlarmingThreshold alarmingThreshold = new AlarmingThreshold();
    private StorageType storage = StorageType.IN_MEMORY;
    private boolean enableListenerLog = false;
    private List<Integer> durationBuckets = Arrays.asList(100, 500, 1000, 2000, 5000);
    private LogType logType = LogType.SIMPLE;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public AlarmingThreshold getAlarmingThreshold() {
        return alarmingThreshold;
    }

    public void setAlarmingThreshold(AlarmingThreshold alarmingThreshold) {
        this.alarmingThreshold = alarmingThreshold;
    }

    public StorageType getStorage() {
        return storage;
    }

    public void setStorage(StorageType storage) {
        this.storage = storage;
    }

    public boolean isEnableListenerLog() {
        return enableListenerLog;
    }

    public void setEnableListenerLog(boolean enableListenerLog) {
        this.enableListenerLog = enableListenerLog;
    }

    public List<Integer> getDurationBuckets() {
        return durationBuckets;
    }

    public void setDurationBuckets(List<Integer> durationBuckets) {
        if (durationBuckets == null || durationBuckets.isEmpty() || durationBuckets.size() > 5) {
            throw new IllegalArgumentException("The duration bucket size must be between 1-5");
        }

        if (durationBuckets.stream().anyMatch(d -> d <= 0)) {
            throw new IllegalArgumentException("The duration bucket values must positive integer");
        }

        Collections.sort(durationBuckets);
        this.durationBuckets = Collections.unmodifiableList(durationBuckets);
    }

    public LogType getLogType() {
        return logType;
    }

    public void setLogType(LogType logType) {
        this.logType = logType == null ? LogType.SIMPLE : logType;
    }

    public enum StorageType {
        IN_MEMORY, REDIS
    }

    public enum LogType {
        SIMPLE, DETAILS
    }

    public static class AlarmingThreshold {
        private long transaction = 1000L;
        private long connection = 1000L;

        public long getTransaction() {
            return transaction;
        }

        public void setTransaction(long transaction) {
            this.transaction = transaction;
        }

        public long getConnection() {
            return connection;
        }

        public void setConnection(long connection) {
            this.connection = connection;
        }
    }
}
