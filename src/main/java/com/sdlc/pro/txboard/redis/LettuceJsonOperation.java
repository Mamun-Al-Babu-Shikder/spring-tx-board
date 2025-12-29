package com.sdlc.pro.txboard.redis;

import com.google.gson.Gson;
import com.sdlc.pro.txboard.domain.PageRequest;
import com.sdlc.pro.txboard.domain.PageResponse;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.MapOutput;
import io.lettuce.core.output.MultiOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;

import java.util.*;

public class LettuceJsonOperation extends AbstractRedisJsonOperation {
    private static final Logger log = LoggerFactory.getLogger(LettuceJsonOperation.class);
    private static final byte[] VALUES = new byte[]{118, 97, 108, 117, 101, 115};
    private static final byte[] TOTAL_RESULTS = {116, 111, 116, 97, 108, 95, 114, 101, 115, 117, 108, 116, 115};

    public LettuceJsonOperation(RedisConnectionFactory connectionFactory, Gson mapper) {
        super(connectionFactory, mapper);
    }

    @Override
    public <T> void createIndex(Class<T> clazz) {
        var command = super.buildIndexCommand(clazz);
        try {
            var status = performCommand(command, new StatusOutput<>(ByteArrayCodec.INSTANCE));
            if (Objects.equals(status, "OK")) {
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
        RedisCommand command = super.buildSaveCommand(entity);
        performCommand(command, new StatusOutput<>(ByteArrayCodec.INSTANCE));
        return new String(command.args()[0]);
    }

    @Override
    public <T> String saveWithExpire(T entity, long second) {
        String key = save(entity);
        RedisCommand expireCommand = RedisCommand.builder(RedisInstruction.EXPIRE)
                .addArgs(key, second)
                .build();
        performCommand(expireCommand, new MultiOutput<>(ByteArrayCodec.INSTANCE));
        return key;
    }

    @Override
    public <T> PageResponse<T> findPageable(Class<T> entityType, PageRequest request) {
        RedisCommand command = buildPageableFetchCommand(entityType, request);
        Map<?, ?> map = (Map<?, ?>) performCommand(command, new MapOutput<>(ByteArrayCodec.INSTANCE));

        long totalElements = 0;
        List<T> content = new LinkedList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() instanceof byte[] values && Arrays.equals(values, VALUES)) {
                try {
                    T entity = super.mapper.getAdapter(entityType).fromJson(new String((byte[]) entry.getKey()));
                    content.add(entity);
                } catch (Exception ex) {
                    throw new RedisDataException("Failed to process Redis data!", ex);
                }
            } else if (entry.getKey() instanceof byte[] values && Arrays.equals(values, TOTAL_RESULTS)) {
                totalElements = (long) entry.getValue();
            }
        }

        return new PageResponse<T>(content, request, totalElements);
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
        Map<?, ?> map = (Map<?, ?>) performCommand(command, new MapOutput<>(ByteArrayCodec.INSTANCE));
        return map.entrySet()
                .stream()
                .filter(e -> e.getKey() instanceof byte[] kay && Arrays.equals(kay, TOTAL_RESULTS))
                .map(e -> (Long) e.getValue())
                .findFirst()
                .orElse(0L);
    }

    @Override
    public <T> double sum(Class<T> entityType, String fieldName) {
        RedisCommand command = buildAggregateSumCommand(entityType, fieldName);
        Map<?, ?> map = (Map<?, ?>) this.performCommand(command, new MapOutput<>(ByteArrayCodec.INSTANCE));
        return map.entrySet()
                .stream()
                .filter(e -> e.getValue() instanceof byte[] kay && Arrays.equals(kay, VALUES))
                .map(e -> Double.valueOf(new String((byte[]) e.getKey())))
                .findFirst()
                .orElse(0.0);
    }

    private long performCountCommand(RedisCommand command) {
        Map<?, ?> map = (Map<?, ?>) this.performCommand(command, new MapOutput<>(ByteArrayCodec.INSTANCE));
        return map.entrySet()
                .stream()
                .filter(e -> e.getKey() instanceof byte[] key && Arrays.equals(key, TOTAL_RESULTS))
                .map(e -> (Long) e.getValue())
                .findFirst()
                .orElse(0L);
    }

    private Object performCommand(RedisCommand command, CommandOutput<byte[], byte[], ?> commandOutput) {
        try (LettuceConnection connection = (LettuceConnection) this.connectionFactory.getConnection()) {
            var nativeConnection = connection.getNativeConnection();
            return nativeConnection.dispatch(
                    RedisModuleCommand.of(command.instruction()),
                    commandOutput,
                    new CommandArgs<>(ByteArrayCodec.INSTANCE)
                            .addValues(command.args())

            ).get();
        } catch (Throwable ex) {
            throw new RuntimeException("Redis command perform exception", ex);
        }
    }

    private static class RedisModuleCommand implements ProtocolKeyword {
        private final byte[] bytes;
        private final String name;

        private static RedisModuleCommand of(RedisInstruction redisInstruction) {
            return new RedisModuleCommand(redisInstruction);
        }

        private RedisModuleCommand(RedisInstruction redisInstruction) {
            this.bytes = redisInstruction.getInstruction().getBytes();
            this.name = redisInstruction.name();
        }

        @Override
        public byte[] getBytes() {
            return this.bytes;
        }

        @Override
        public String name() {
            return this.name;
        }
    }
}
