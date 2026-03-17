package com.sdlc.pro.txboard.domain;

import java.util.Arrays;
import java.util.Objects;

public class Sort {
    public static final Sort UNSORTED = new Sort();

    private final String property;
    private final Direction direction;

    public Sort() {
        this.property = null;
        this.direction = null;
    }

    private Sort(String property, Direction direction) {
        Objects.requireNonNull(property);
        Objects.requireNonNull(direction);
        if (property.isBlank()) {
            throw new IllegalArgumentException("The sort property must not be blank");
        }
        this.property = property;
        this.direction = direction;
    }

    public boolean isSortable() {
        return this != UNSORTED && this.property != null && this.direction != null;
    }

    public static Sort by(String property, Direction direction) {
        return new Sort(property, direction);
    }

    public static Sort from(String value) {
        Objects.requireNonNull(value);
        String[] propDir = value.split(",");
        if (propDir.length != 2) {
            throw new IllegalArgumentException("Can't extract property and sort direction from given value: " + value);
        }
        return Sort.by(propDir[0].trim(), propDir[1].trim());
    }

    public static Sort by(String property, String direction) {
        return Sort.by(
                property,
                Arrays.stream(Direction.values()).anyMatch(e-> e.name().equalsIgnoreCase(direction))
                ? Direction.valueOf(direction.toUpperCase()) : Direction.ASC
        );
    }

    public String getProperty() {
        return property;
    }

    public Direction getDirection() {
        return direction;
    }

    public enum Direction {
        ASC, DESC;
    }
}
