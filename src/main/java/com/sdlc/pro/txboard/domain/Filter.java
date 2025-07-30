package com.sdlc.pro.txboard.domain;

import java.util.Objects;

public final class Filter implements FilterNode {
    private final String property;
    private final Object value;
    private final Operator operator;

    private Filter(String property, Object value, Operator operator) {
        Objects.requireNonNull(property);
        Objects.requireNonNull(operator);
        Objects.requireNonNull(value);
        this.property = property;
        this.value = value;
        this.operator = operator;
    }

    public static Filter of(String property, Object value, Operator operator) {
        return new Filter(property, value, operator);
    }

    public String getProperty() {
        return property;
    }

    public Object getValue() {
        return value;
    }

    public Operator getOperator() {
        return operator;
    }

    public enum Operator {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        GREATER_THAN_OR_EQUALS,
        LESS_THAN,
        LESS_THAN_OR_EQUALS,
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH
    }
}

