package com.sdlc.pro.txboard.redis;

import com.sdlc.pro.txboard.domain.PageRequest;
import com.sdlc.pro.txboard.domain.PageResponse;

public interface RedisJsonOperation {

    <T> void registerRedisEntityClass(Class<T> entityType);

    <T> void createIndex(Class<T> entityType);

    <T> String save(T entity);

    <T> String saveWithExpire(T entity, long second);

    <T> PageResponse<T> findPageable(Class<T> clazz, PageRequest request);

    <T> long count(Class<T> entityType);

    <T> long countByField(Class<T> entityType, String fieldName, Object value);

    <T> double sum(Class<T> entityType, String fieldName);
}
