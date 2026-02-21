package com.sdlc.pro.txboard.redis;

import com.sdlc.pro.txboard.domain.Filter;
import com.sdlc.pro.txboard.domain.FilterGroup;
import com.sdlc.pro.txboard.domain.FilterNode;

import java.util.List;
import java.util.stream.Collectors;

public final class RedisQueryBuilder {

    public static String toRedisQuery(FilterNode node, RedisEntityInfo entityInfo) {
        if (node instanceof FilterNode.UnFilter) {
            return "*"; // no filters
        }
        return build(node, entityInfo);
    }

    private static String build(FilterNode node, RedisEntityInfo entityInfo) {
        if (node instanceof FilterGroup) {
            return buildGroup((FilterGroup) node, entityInfo);
        }
        if (node instanceof Filter) {
            return buildFilter((Filter) node, entityInfo);
        }
        throw new IllegalArgumentException("Unknown FilterNode type: " + node);
    }

    private static String buildGroup(FilterGroup group, RedisEntityInfo entityInfo) {
        List<String> parts = group.getFilterNodes().stream()
                .map(node -> build(node, entityInfo))
                .filter(s -> !s.trim().isEmpty() && !s.equals("*"))
                .collect(Collectors.toList());

        if (parts.isEmpty()) return "*";

        String joiner = group.getLogic() == FilterGroup.Logic.AND ? " " : " | ";

        return "(" + String.join(joiner, parts) + ")";
    }

    private static String buildFilter(Filter f, RedisEntityInfo entityInfo) {
        String field = f.getProperty();
        Object value = f.getValue();
        Filter.Operator op = f.getOperator();

        SchemaFieldType fieldType = entityInfo.getRedisSchemaFieldTypeOf(field);

        switch (op) {
            case EQUALS:
                return String.format(fieldType == SchemaFieldType.TAG ? "(@%s:{%s})" : "(@%s:%s)", field, escapeValue(value));

            case NOT_EQUALS:
                return String.format(fieldType == SchemaFieldType.TAG ? "(-@%s:{%s})" : "(-@%s:%s)", field, escapeValue(value));

            case GREATER_THAN:
                return String.format("@%s:[(%s inf]", field, numeric(value));

            case GREATER_THAN_OR_EQUALS:
                return String.format("@%s:[%s inf]", field, numeric(value));

            case LESS_THAN:
                return String.format("@%s:[-inf (%s]", field, numeric(value));

            case LESS_THAN_OR_EQUALS:
                return String.format("@%s:[-inf %s]", field, numeric(value));

            case CONTAINS:
                return String.format("(@%s:*%s*)", field, sanitizeText(value));

            case STARTS_WITH:
                return String.format("(@%s:%s*)", field, sanitizeText(value));

            case ENDS_WITH:
                return String.format("(@%s:*%s)", field, sanitizeText(value));
        }

        return "";
    }


    private static String escapeValue(Object v) {
        if (v instanceof Number) return v.toString();
        if (v instanceof Boolean) return v.toString().toLowerCase();
        return sanitizeText(v);
    }

    public static String sanitizeText(Object value) {
        String specialChars = ",-.<>{}[]\"':;!@#$%^&*()+= ~|?/\\";
        StringBuilder escaped = new StringBuilder();
        for (char c : value.toString().toCharArray()) {
            if (specialChars.indexOf(c) != -1) {
                escaped.append('\\');
            }
            escaped.append(c);
        }

        return escaped.toString();
    }

    private static String numeric(Object v) {
        if (!(v instanceof Number)) {
            throw new IllegalArgumentException("Numeric comparison requires Number type, got " + v.getClass());
        }
        return v.toString();
    }
}
