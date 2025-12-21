package com.sdlc.pro.txboard.util;

import com.redis.om.spring.search.stream.predicates.SearchFieldPredicate;
import com.sdlc.pro.txboard.domain.Filter;
import com.sdlc.pro.txboard.domain.FilterGroup;
import com.sdlc.pro.txboard.domain.FilterNode;
import com.sdlc.pro.txboard.domain.Sort;
import com.sdlc.pro.txboard.model.TransactionLogDocument;
import com.sdlc.pro.txboard.model.TransactionLogDocument$;
import redis.clients.jedis.search.aggr.SortedField;


import java.util.Comparator;
import java.util.List;

public final class RedisFactory {
    private RedisFactory() {}

    public static SearchFieldPredicate<TransactionLogDocument, ?> predicate(FilterNode node) {
        if (node instanceof Filter filter) {
            return predicateSingle(filter);
        }

        if (node instanceof FilterGroup group) {
            return predicateGroup(group);
        }

        return matchAll();
    }

    private static SearchFieldPredicate<TransactionLogDocument, ?> predicateSingle(Filter filter) {
        String field = filter.getProperty();
        Object value = filter.getValue();

        return switch (field) {
            case "status" -> TransactionLogDocument$.STATUS.eq(value.toString());

            case "thread" -> TransactionLogDocument$.THREAD.containing(value.toString());

            case "method" -> TransactionLogDocument$.METHOD.containing(value.toString());

            case "propagation" -> TransactionLogDocument$.PROPAGATION.eq(value.toString());

            case "isolation" -> TransactionLogDocument$.ISOLATION.eq(value.toString());

            case "connectionOriented" -> TransactionLogDocument$.CONNECTION_ORIENTED.eq(value.toString());

            default -> matchAll();
        };
    }

    private static SearchFieldPredicate<TransactionLogDocument, ?> predicateGroup(FilterGroup group) {
        List<FilterNode> nodes = group.getFilterNodes();

        if (nodes.isEmpty()) {
            return matchAll();
        }

        SearchFieldPredicate<TransactionLogDocument, ?> predicate = predicate(nodes.get(0));

        for (int i = 1; i < nodes.size(); i++) {
            SearchFieldPredicate<TransactionLogDocument, ?> nextPredicate = predicate(nodes.get(i));
            if (group.getLogic() == FilterGroup.Logic.AND) {
                predicate = predicate.andAny(nextPredicate);
            } else {
                predicate = predicate.orAny(nextPredicate);
            }
        }

        return predicate;
    }


    public static Comparator<TransactionLogDocument> sortProperty(Sort sort) {
        String property = sort.getProperty();
        return switch (property) {
            case "method" -> TransactionLogDocument$.METHOD;
            case "startTime" -> TransactionLogDocument$.START_TIME;
            case "duration" -> TransactionLogDocument$.DURATION;
            case "status" -> TransactionLogDocument$.STATUS;
            case "propagation" -> TransactionLogDocument$.PROPAGATION;
            case "isolation" -> TransactionLogDocument$.ISOLATION;
            case "thread" -> TransactionLogDocument$.THREAD;
            default -> throw new IllegalArgumentException("Unsupported sort property: " + property);
        };
    }

    public static SortedField.SortOrder sortDirection(Sort sort) {
        Sort.Direction direction = sort.getDirection();
        return switch (direction) {
            case ASC -> SortedField.SortOrder.ASC;
            case DESC -> SortedField.SortOrder.DESC;
        };
    }

    private static SearchFieldPredicate<TransactionLogDocument, ?> matchAll() {
        return TransactionLogDocument$.DURATION.between(Long.MIN_VALUE, Long.MAX_VALUE);
    }
}