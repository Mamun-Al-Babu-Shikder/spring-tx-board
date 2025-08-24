package com.sdlc.pro.txboard.model;

public class ConnectionSummary {
    private final int acquisitionCount;
    private final int alarmingConnectionCount;
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