package com.sdlc.pro.txboard.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdlc.pro.txboard.domain.Filter;
import com.sdlc.pro.txboard.domain.PageRequest;
import com.sdlc.pro.txboard.domain.Sort;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractRedisJsonOperation implements RedisJsonOperation {
    protected final RedisConnectionFactory connectionFactory;
    protected final ObjectMapper mapper;

    private final ConcurrentMap<Class<?>, RedisEntityInfo> entityInfoMap;

    public AbstractRedisJsonOperation(RedisConnectionFactory connectionFactory, ObjectMapper mapper) {
        this.connectionFactory = connectionFactory;
        this.mapper = mapper;
        this.entityInfoMap = new ConcurrentHashMap<>();
    }

    @Override
    public final <T> void registerRedisEntityClass(Class<T> entityType) {
        Objects.requireNonNull(entityType, "Given @RedisEntity type should not be null");
        if (!entityType.isAnnotationPresent(RedisEntity.class)) {
            throw new IllegalArgumentException("Given type %s is not annotated with @RedisEntity".formatted(entityType.getName()));
        }
        RedisEntityInfo entityInfo = new RedisEntityInfo(entityType);
        this.entityInfoMap.put(entityType, entityInfo);
    }

    public RedisEntityInfo redisEntityInfoOf(Class<?> entityType) {
        RedisEntityInfo entityInfo = this.entityInfoMap.get(entityType);
        if (entityInfo == null) {
            throw new IllegalArgumentException("There have no registered @RedisEntity for given type %s".formatted(entityType.getName()));
        }
        return entityInfo;
    }

    private <T> byte[] generateSerializableKey(T t, RedisEntityInfo entityInfo) {
        try {
            Field idField = entityInfo.getIdField();
            Object id = idField.get(t);
            return (entityInfo.getRecordPrefix() + id).getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("Field to generate redis key for given @RedisEntity %s".formatted(entityInfo.getEntityName()), e);
        }
    }

    protected <T> byte[] generateSerializableValue(T t) {
        try {
            return mapper.writeValueAsBytes(t);
        } catch (Throwable ex) {
            throw new RuntimeException("Serialization exception");
        }
    }

    protected RedisCommand buildIndexCommand(Class<?> entityType) {
        RedisEntityInfo entityInfo = redisEntityInfoOf(entityType);
        byte[] indexName = entityInfo.getIndexName().getBytes();
        String recordPrefix = entityInfo.getRecordPrefix();
        RedisCommand.Builder builder = RedisCommand.builder(RedisInstruction.FT_CREATE)
                .addArg(indexName)
                .addArg("on")
                .addArg("JSON")
                .addArg("PREFIX")
                .addArg("1")
                .addArg(recordPrefix)
                .addArg("SCHEMA");

        for (IndexedFieldInfo info : entityInfo.getIndexedFieldInfos()) {
            String path = info.getPath();
            String type = info.getRedisType().name();
            builder.addArg("$.%s".formatted(path))
                    .addArg("AS")
                    .addArg(path)
                    .addArg(type);

            if (info.isSortable()) {
                builder.addArg("SORTABLE");
            }
        }

        return builder.build();
    }

    protected <T> RedisCommand buildSaveCommand(T t) {
        Objects.requireNonNull(t, "The @RedisEntity instance should not be null");
        Class<?> entityType = t.getClass();
        RedisEntityInfo entityInfo = redisEntityInfoOf(entityType);
        byte[] key = this.generateSerializableKey(t, entityInfo);
        byte[] value = this.generateSerializableValue(t);
        return RedisCommand.builder(RedisInstruction.JSON_SET)
                .addArg(key)
                .addArg("$")
                .addArg(value)
                .build();
    }

    protected RedisCommand buildPageableFetchCommand(Class<?> entityType, PageRequest request) {
        RedisEntityInfo entityInfo = redisEntityInfoOf(entityType);
        RedisCommand.Builder builder = RedisCommand.builder(RedisInstruction.FT_SEARCH);

        builder.addArg(entityInfo.getIndexName());

        String query = RedisQueryBuilder.toRedisQuery(request.getFilter(), entityInfo);
        builder.addArg(query);

        Sort sort = request.getSort();
        if (sort.isSortable()) {
            builder.addArgs("SORTBY", sort.getProperty(), sort.getDirection());
        }

        int offset = request.getPageNumber() * request.getPageSize();
        builder.addArgs("LIMIT", offset, request.getPageSize());

        return builder.build();
    }

    protected RedisCommand buildSimpleCountCommand(Class<?> entityType) {
        return buildCountCommandForField(entityType, null, null);
    }

    protected RedisCommand buildCountCommandForField(Class<?> entityType, String fieldName, Object value) {
        RedisEntityInfo entityInfo = redisEntityInfoOf(entityType);
        return RedisCommand.builder(RedisInstruction.FT_SEARCH)
                .addArg(entityInfo.getIndexName())
                .addArg(
                        fieldName == null ? "*" : RedisQueryBuilder.toRedisQuery(
                                Filter.of(fieldName, value, Filter.Operator.EQUALS),
                                entityInfo
                        )
                )
                .addArgs("LIMIT", 0, 0)
                .build();
    }

    protected RedisCommand buildCountCommandForRange(Class<?> entityType, String fieldName, long lowerLimit, long upperLimit) {
        RedisEntityInfo entityInfo = redisEntityInfoOf(entityType);
        if (entityInfo.getRedisSchemaFieldTypeOf(fieldName) != SchemaFieldType.NUMERIC) {
            throw new IllegalArgumentException("The field %s should be NUMERIC type to perform COUNT operation for according to provided range");
        }

        return RedisCommand.builder(RedisInstruction.FT_SEARCH)
                .addArg(entityInfo.getIndexName())
                .addArg("(@%s:[%d %d])".formatted(fieldName, lowerLimit, upperLimit))
                .addArgs("LIMIT", 0, 0)
                .build();
    }

    protected RedisCommand buildAggregateSumCommand(Class<?> entityType, String fieldName) {
        RedisEntityInfo entityInfo = redisEntityInfoOf(entityType);
        if (entityInfo.getRedisSchemaFieldTypeOf(fieldName) != SchemaFieldType.NUMERIC) {
            throw new IllegalArgumentException("The field %s should be NUMERIC type to perform aggregate SUM operation");
        }

        return RedisCommand.builder(RedisInstruction.FT_AGGREGATE)
                .addArg(entityInfo.getIndexName())
                .addArg("*")
                .addArgs("GROUPBY", "0", "REDUCE", "SUM", "1")
                .addArgs(fieldName, "AS", "__sum__")
                .build();
    }
}
