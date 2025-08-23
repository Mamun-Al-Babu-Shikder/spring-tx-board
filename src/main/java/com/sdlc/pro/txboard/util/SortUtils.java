package com.sdlc.pro.txboard.util;

import com.sdlc.pro.txboard.domain.Sort;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class SortUtils {

    public static <T> List<T> sort(List<T> data, Sort sort) {
        if (sort == null || !sort.isSortable()) return data;

        try {
            Comparator<T> comparator = buildComparator(sort);
            return data.stream()
                    .sorted(comparator)
                    .collect(toList());
        } catch (Exception e) {
            return data; // fail-safe: return unsorted
        }
    }

    private static <T> Comparator<T> buildComparator(Sort sort) throws NoSuchFieldException {
        return (a, b) -> {
            try {
                Object va = getMethodValue(a, sort.getProperty());
                Object vb = getMethodValue(b, sort.getProperty());

                if (va == null && vb == null) return 0;
                if (va == null) return sort.getDirection() == Sort.Direction.ASC ? -1 : 1;
                if (vb == null) return sort.getDirection() == Sort.Direction.ASC ? 1 : -1;

                @SuppressWarnings("unchecked")
                int result = ((Comparable<Object>) va).compareTo(vb);

                return sort.getDirection() == Sort.Direction.ASC ? result : -result;

            } catch (Exception e) {
                return 0;
            }
        };
    }

    private static Object getMethodValue(Object item, String fieldName) throws Exception {
        Method method = getMethod(item.getClass(), fieldName);
        method.setAccessible(true);
        return method.invoke(item);
    }

    private static Method getMethod(Class<?> clazz, String fieldName) throws NoSuchMethodException {
        String methodName = "get" + Character.toUpperCase(fieldName.toCharArray()[0]) + fieldName.substring(1);

        try {
            return clazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException ignored) {
            throw new NoSuchMethodException("Method " + methodName + " not found on " + clazz);
        }
    }
}
