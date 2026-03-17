package com.sdlc.pro.txboard.redis;

import java.lang.reflect.Field;

class IndexedFieldInfo {
    private final String path;
    private final Field field;
    private final IndexFiled indexFiled;

    IndexedFieldInfo(String path, Field field, IndexFiled indexFiled) {
        this.path = path;
        this.field = field;
        this.indexFiled = indexFiled;
    }


    public String getPath() {
        return this.path;
    }

    String getFieldName() {
        return this.field.getName();
    }

    SchemaFieldType getRedisType() {
        return this.indexFiled.schemaFieldType();
    }

    boolean isSortable() {
        return this.indexFiled.sortable();
    }
}
