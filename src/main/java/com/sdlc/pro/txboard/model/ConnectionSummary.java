package com.sdlc.pro.txboard.model;

import com.sdlc.pro.txboard.redis.IndexFiled;

import static com.sdlc.pro.txboard.redis.SchemaFieldType.NUMERIC;

public class ConnectionSummary {
    @IndexFiled(schemaFieldType = NUMERIC)
    private final int acquisitionCount;
    @IndexFiled(schemaFieldType = NUMERIC)
    private final int alarmingConnectionCount;
    @IndexFiled(schemaFieldType = NUMERIC)
    private final long occupiedTime;

    public ConnectionSummary(int acquisitionCount, int alarmingConnectionCount, long occupiedTime) {
        this.acquisitionCount = acquisitionCount;
        this.alarmingConnectionCount = alarmingConnectionCount;
        this.occupiedTime = occupiedTime;
    }

    public int getAcquisitionCount() { return acquisitionCount; }
    public int getAlarmingConnectionCount() { return alarmingConnectionCount; }
    public long getOccupiedTime() { return occupiedTime; }
}