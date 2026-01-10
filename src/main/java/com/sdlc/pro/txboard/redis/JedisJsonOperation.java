package com.sdlc.pro.txboard.redis;

import com.google.gson.Gson;
import com.sdlc.pro.txboard.domain.PageRequest;
import com.sdlc.pro.txboard.domain.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class JedisJsonOperation extends AbstractRedisJsonOperation {
    private static final Logger log = LoggerFactory.getLogger(JedisJsonOperation.class);

    public JedisJsonOperation(RedisConnectionFactory connectionFactory, Gson mapper) {
        super(connectionFactory, mapper);
    }

    @Override
    public <T> void createIndex(Class<T> clazz) {
        RedisCommand command = buildIndexCommand(clazz);
        try {
            Object status = performCommand(command);
            if (Objects.equals(new String((byte[]) status), "OK")) {
                log.info("Index created successfully for @RedisEntity type '{}'", clazz.getName());
            } else {
                log.warn("Unexpected error occurred while creating index for @RedisEntity type '{}'", clazz.getName());
            }
        } catch (Throwable ex) {
            if (Objects.equals("Index already exists", ex.getCause().getCause().getMessage())) {
                log.warn("Index already exist for @RedisEntity type '{}'", clazz.getName());
            } else {
                throw ex;
            }
        }
    }

    @Override
    public <T> String save(T entity) {
        RedisCommand command = buildSaveCommand(entity);
        performCommand(command);
        return new String(command.args()[0]);
    }

    @Override
    public <T> String saveWithExpire(T entity, long second) {
        String key = save(entity);
        RedisCommand expireCommand = RedisCommand.builder(RedisInstruction.EXPIRE)
                .addArgs(key, second)
                .build();
        performCommand(expireCommand);
        return key;
    }

    @Override
    public <T> PageResponse<T> findPageable(Class<T> entityType, PageRequest request) {
        RedisCommand command = buildPageableFetchCommand(entityType, request);

        long totalElements = 0;
        List<T> content = new LinkedList<>();

        Object result = performCommand(command);
        if (result instanceof List<?> resultList) {
            for (Object data : resultList) {
                if (data instanceof Long total) {
                    totalElements = total;
                } else if (data instanceof List<?> dataList && !dataList.isEmpty()) {
                    try {
                        byte[] jsonBytes = (byte[]) dataList.get(dataList.size() - 1);
                        T entity = super.mapper.getAdapter(entityType).fromJson(new String(jsonBytes));
                        content.add(entity);
                    } catch (Exception ex) {
                        throw new RedisDataException("Failed to process Redis data!", ex);
                    }
                }
            }
        }

        return new PageResponse<>(content, request, totalElements);
    }

    @Override
    public <T> long count(Class<T> entityType) {
        RedisCommand command = buildSimpleCountCommand(entityType);
        return performCountCommand(command);
    }

    @Override
    public <T> long countByFieldValue(Class<T> entityType, String fieldName, Object value) {
        RedisCommand command = buildCountCommandForField(entityType, fieldName, value);
        return performCountCommand(command);
    }

    @Override
    public <T> long countByRange(Class<T> entityType, String fieldName, long lowerLimit, long upperLimit) {
        RedisCommand command = buildCountCommandForRange(entityType, fieldName, lowerLimit, upperLimit);
        return performCountCommand(command);
    }

    @Override
    public <T> double sum(Class<T> entityType, String fieldName) {
        RedisCommand command = buildAggregateSumCommand(entityType, fieldName);
        Object result = performCommand(command);
        if (result instanceof List<?> resultList) {
            for (var data : resultList) {
                if (data instanceof List<?> sumData && !sumData.isEmpty() &&
                        sumData.get(sumData.size() - 1) instanceof byte[] sumBytes) {
                   String sum = new String(sumBytes);
                   return Double.parseDouble(sum);
                }
            }
        }

        return 0.0;
    }

    private long performCountCommand(RedisCommand command) {
        Object result = performCommand(command);
        if (result instanceof List<?> resultList) {
            for (Object data : resultList) {
                if (data instanceof Long count) {
                    return count;
                }
            }
        }

        return 0L;
    }

    private Object performCommand(RedisCommand command) {
        RedisConnection connection = RedisConnectionUtils.getConnection(super.connectionFactory);
        try {
            return connection.execute(command.instruction().getInstruction(), command.args());
        } catch (Throwable ex) {
            throw new RedisDataException("Redis command perform exception", ex);
        } finally {
            RedisConnectionUtils.releaseConnection(connection, super.connectionFactory);
        }
    }
}
