package com.sdlc.pro.txboard.util;

import com.sdlc.pro.txboard.model.SqlExecutionLog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SqlLogUtils {

    public static List<SqlExecutionLog> createSqlExecutionLogs() {
        Instant baseTime = Instant.parse("2026-02-17T10:45:00Z");

        List<SqlExecutionLog> logs = new ArrayList<>();
        logs.add(new SqlExecutionLog(
                UUID.fromString("08c2c8ee-369f-4ac0-bd47-0763f964967a"),
                baseTime.plusMillis(10),
                baseTime.plusMillis(130),
                false,
                "main",
                List.of("select * from post")
        ));
        logs.add(new SqlExecutionLog(
                UUID.fromString("df3990bf-bbdd-46c6-9105-768bbd71207c"),
                baseTime.plusMillis(50),
                baseTime.plusMillis(560),
                true,
                "pool-3-thread-21",
                List.of("select * from account", "select * from post")
        ));
        logs.add(new SqlExecutionLog(
                UUID.fromString("e42ad6f2-22fe-4b22-9682-0634fccb5f95"),
                baseTime.plusMillis(150),
                baseTime.plusMillis(390),
                false,
                "pool-1-thread-27",
                List.of("select * from post where id=1", "select * from comment")
        ));
        logs.add(new SqlExecutionLog(
                UUID.fromString("201a5305-d9cc-4d3a-bbd3-2c8e85a2099b"),
                baseTime.plusMillis(250),
                baseTime.plusMillis(740),
                true,
                "pool-2-thread-2",
                List.of("select * from comment where id=12")
        ));
        logs.add(new SqlExecutionLog(
                UUID.fromString("d6d000f9-fc1b-433c-a789-23fe44812315"),
                baseTime.plusMillis(550),
                baseTime.plusMillis(670),
                false,
                "main",
                List.of("select * from post where id=14")
        ));
        return logs;
    }
}
