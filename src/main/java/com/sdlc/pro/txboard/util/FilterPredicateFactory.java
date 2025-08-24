package com.sdlc.pro.txboard.util;

import com.sdlc.pro.txboard.domain.Filter;
import com.sdlc.pro.txboard.domain.FilterGroup;
import com.sdlc.pro.txboard.domain.FilterNode;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.function.Predicate;

public final class FilterPredicateFactory {

    public static <T> Predicate<T> buildPredicate(FilterNode node) {
        if (node instanceof Filter) {
            return buildPredicateFromFilter((Filter) node);
        } else if (node instanceof FilterGroup) {
            return buildGroupPredicate((FilterGroup) node);
        }
        return (Predicate<T>) t -> true;
    }

    public static <T> Predicate<T> buildPredicateFromFilter(Filter filter) {
        return (T item) -> {
            try {
                Object fieldValue = getFieldValue(item, filter.getProperty());
                Object targetValue = filter.getValue();

                if (fieldValue == null) return false;

                switch (filter.getOperator()) {
                    case EQUALS:
                        return fieldValue.equals(targetValue);
                    case NOT_EQUALS:
                        return !fieldValue.equals(targetValue);
                    case GREATER_THAN:
                        return compare(fieldValue, targetValue) > 0;
                    case GREATER_THAN_OR_EQUALS:
                        return compare(fieldValue, targetValue) >= 0;
                    case LESS_THAN:
                        return compare(fieldValue, targetValue) < 0;
                    case LESS_THAN_OR_EQUALS:
                        return compare(fieldValue, targetValue) <= 0;
                    case CONTAINS:
                        return contains(fieldValue, targetValue);
                    case STARTS_WITH:
                        return startsWith(fieldValue, targetValue);
                    case ENDS_WITH:
                        return endsWith(fieldValue, targetValue);
                    default:
                        return false;
                }
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
        if (fieldValue instanceof String && value instanceof String) {
            String s = ((String) fieldValue);
            String v = ((String) value);
            return s.toLowerCase().contains(v.toLowerCase());
        } else if (fieldValue instanceof Collection) {
            Collection<?> c = (Collection<?>) fieldValue;
            return c.contains(value);
        }
        return false;
    }

    private static boolean startsWith(Object fieldValue, Object value) {
        if (fieldValue instanceof String && value instanceof String) {
            String s = (String) fieldValue;
            String v = (String) value;
            return s.toLowerCase().startsWith(v.toLowerCase());
        }
        return false;
    }

    private static boolean endsWith(Object fieldValue, Object value) {
        if (fieldValue instanceof String && value instanceof String) {
            String s = (String) fieldValue;
            String v = (String) value;
            return s.toLowerCase().endsWith(v.toLowerCase());
        }
        return false;
    }
}
