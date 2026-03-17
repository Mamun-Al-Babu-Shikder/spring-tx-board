package com.sdlc.pro.txboard.redis;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class RedisEntityInfo {
    private static final String INDEX_SUFFIX = "Idx";

    private final Class<?> entityType;
    private final RedisEntity redisEntity;
    private final Field idField;
    private final Map<String, IndexedFieldInfo> indexedFieldInfoMap;

    private String indexName;
    private String recordPrefix;

    RedisEntityInfo(Class<?> entityType) {
        this.redisEntity = entityType.getDeclaredAnnotation(RedisEntity.class);
        this.entityType = entityType;
        this.idField = resolveIdField(entityType);
        this.idField.setAccessible(true);
        this.indexedFieldInfoMap = resolveIndexedFields(entityType);
    }

    private Field resolveIdField(Class<?> entityType) {
        List<Field> idFields = Arrays.stream(entityType.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(RedisId.class))
                .toList();

        if (idFields.size() != 1) {
            throw new IllegalArgumentException("The RedisEntity %s must have only one @RedisId field".formatted(entityType.getName()));
        }

        return idFields.get(0);
    }

    private Map<String, IndexedFieldInfo> resolveIndexedFields(Class<?> entityType) {
        Map<String, IndexedFieldInfo> indexedFieldInfos = findIndexFieldsRecursively(entityType, null)
                .stream()
                .collect(Collectors.toMap(IndexedFieldInfo::getPath, Function.identity()));

        if (indexedFieldInfos.isEmpty()) {
            throw new IllegalArgumentException("RedisEntity %s required at least one IndexFiled".formatted(entityType.getName()));
        }

        return indexedFieldInfos;
    }

    private List<IndexedFieldInfo> findIndexFieldsRecursively(Class<?> clazz, String path) {
        List<IndexedFieldInfo> indexedFieldInfos = new LinkedList<>();
        for (Field field : clazz.getDeclaredFields()) {
            IndexFiled indexFiled = field.getDeclaredAnnotation(IndexFiled.class);
            String absolutePath = path == null ? field.getName() : path + "." + field.getName();
            if (indexFiled != null) {
                if (indexFiled.schemaFieldType() == SchemaFieldType.NESTED) {
                    List<IndexedFieldInfo> nestedIndexedFieldInfos = findIndexFieldsRecursively(
                            field.getType(),
                            absolutePath
                    );

                    indexedFieldInfos.addAll(nestedIndexedFieldInfos);
                } else {
                    indexedFieldInfos.add(new IndexedFieldInfo(absolutePath, field, indexFiled));
                }
            }
        }

        return indexedFieldInfos;
    }

    Class<?> getEntityType() {
        return this.entityType;
    }

    String getIndexName() {
        if (this.indexName == null) {
            this.indexName = this.redisEntity.indexName().isBlank() ?
                    this.entityType.getName().concat(INDEX_SUFFIX) : this.redisEntity.indexName();
        }

        return this.indexName;
    }

    String getRecordPrefix() {
        if (this.recordPrefix == null) {
            this.recordPrefix = (redisEntity.recordPrefix().isBlank() ?
                    this.entityType.getName() : this.redisEntity.recordPrefix()).concat(":");
        }

        return this.recordPrefix;
    }

    Class<?> getIdType() {
        return this.idField.getType();
    }

    String getEntityName() {
        return this.entityType.getName();
    }

    String getEntitySimpleName() {
        return this.entityType.getSimpleName();
    }

    Field getIdField() {
        return this.idField;
    }

    String getIdFieldName() {
        return this.idField.getName();
    }

    Collection<IndexedFieldInfo> getIndexedFieldInfos() {
        return this.indexedFieldInfoMap.values();
    }

    SchemaFieldType getRedisSchemaFieldTypeOf(String fieldName) {
        IndexedFieldInfo fieldInfo = indexedFieldInfoMap.get(fieldName);
        if (fieldInfo == null) {
            throw new IllegalArgumentException("Field %s is not found at @RedisEntity %s".formatted(fieldName, entityType.getName()));
        }

        return fieldInfo.getRedisType();
    }
}
