package com.sdlc.pro.txboard.model;

import com.sdlc.pro.txboard.redis.IndexFiled;
import static com.sdlc.pro.txboard.redis.SchemaFieldType.NUMERIC;

import java.io.Serializable;

public record ConnectionSummary(
        @IndexFiled(schemaFieldType = NUMERIC) int acquisitionCount,
        @IndexFiled(schemaFieldType = NUMERIC) int alarmingConnectionCount,
        @IndexFiled(schemaFieldType = NUMERIC) long occupiedTime) implements Serializable {
}
