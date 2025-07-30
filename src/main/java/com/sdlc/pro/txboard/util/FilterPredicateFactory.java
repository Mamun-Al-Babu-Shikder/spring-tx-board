package com.sdlc.pro.txboard.util;

import com.sdlc.pro.txboard.domain.Filter;
import com.sdlc.pro.txboard.domain.FilterGroup;
import com.sdlc.pro.txboard.domain.FilterNode;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.function.Predicate;

public final class FilterPredicateFactory {

    public static <T> Predicate<T> buildPredicate(FilterNode node) {
        if (node instanceof Filter filter) {
            return buildPredicateFromFilter(filter);
        } else if (node instanceof FilterGroup group) {
            return buildGroupPredicate(group);
        }
        return (Predicate<T>) t -> true;
    }

    public static <T> Predicate<T> buildPredicateFromFilter(Filter filter) {
        return (T item) -> {
            try {
                Object fieldValue = getFieldValue(item, filter.getProperty());
                Object targetValue = filter.getValue();

                if (fieldValue == null) return false;

                return switch (filter.getOperator()) {
                    case EQUALS -> fieldValue.equals(targetValue);
                    case NOT_EQUALS -> !fieldValue.equals(targetValue);
                    case GREATER_THAN -> compare(fieldValue, targetValue) > 0;
                    case GREATER_THAN_OR_EQUALS -> compare(fieldValue, targetValue) >= 0;
                    case LESS_THAN -> compare(fieldValue, targetValue) < 0;
                    case LESS_THAN_OR_EQUALS -> compare(fieldValue, targetValue) <= 0;
                    case CONTAINS -> contains(fieldValue, targetValue);
                    case STARTS_WITH -> startsWith(fieldValue, targetValue);
                    case ENDS_WITH -> endsWith(fieldValue, targetValue);
                };
            } catch (Exception e) {
                return false;
            }
        };
    }

    private static <T> Predicate<T> buildGroupPredicate(FilterGroup group) {
        return group.getFilterNodes().stream()
                .<Predicate<T>>map(FilterPredicateFactory::buildPredicate)
                .reduce(group.getLogic() == FilterGroup.Logic.AND ? Predicate::and : Predicate::or)
                .orElse(group.getLogic() == FilterGroup.Logic.AND ? t -> true : t -> false);
    }

    private static Object getFieldValue(Object item, String fieldName) throws Exception {
        Field field = getField(item.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(item);
    }

    private static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field " + fieldName + " not found on " + clazz);
    }

    @SuppressWarnings("unchecked")
    private static int compare(Object a, Object b) {
        return ((Comparable<Object>) a).compareTo(b);
    }

    private static boolean contains(Object fieldValue, Object value) {
        if (fieldValue instanceof String s && value instanceof String v) {
            return s.toLowerCase().contains(v.toLowerCase());
        } else if (fieldValue instanceof Collection<?> c) {
            return c.contains(value);
        }
        return false;
    }

    private static boolean startsWith(Object fieldValue, Object value) {
        return (fieldValue instanceof String s && value instanceof String v) &&
                s.toLowerCase().startsWith(v.toLowerCase());
    }

    private static boolean endsWith(Object fieldValue, Object value) {
        return (fieldValue instanceof String s && value instanceof String v) &&
                s.toLowerCase().endsWith(v.toLowerCase());
    }
}
