package com.sdlc.pro.txboard.util;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Simple heuristic-based N+1 query detector.
 *
 * It normalizes SQL by:
 *  - lowercasing
 *  - collapsing whitespace
 *  - replacing literals/numbers with '?'
 *  - keeping table/column keywords
 *
 * Then it counts how many times the same normalized pattern appears within a transaction.
 * If any pattern repeats more than the specified threshold (e.g., >= 5), we treat it as a potential N+1.
 */
public final class NPlusOneAnalyzer {
    private static final Pattern NUMBER = Pattern.compile("\\b\\d+\\b");
    private static final Pattern STRING = Pattern.compile("'[^']*'");
    private static final Pattern IN_CLAUSE_VALUES = Pattern.compile("\\(\\s*(?:\\?|\'[^']*\'|\\d+)(?:\\s*,\\s*(?:\\?|\'[^']*\'|\\d+))*\\s*\\)");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final int DEFAULT_REPEAT_THRESHOLD = 5;

    private NPlusOneAnalyzer() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static boolean detectPotentialNPlusOne(List<String> queries) {
        return detectPotentialNPlusOne(queries, DEFAULT_REPEAT_THRESHOLD);
    }

    public static boolean detectPotentialNPlusOne(List<String> queries, int repeatThreshold) {
        int threshold = Math.max(2, repeatThreshold); // at least 2 repetitions
        if (queries == null || queries.size() < threshold) {
            return false;
        }
        Map<String, Integer> counts = patternCounts(queries);
        return counts.values().stream().anyMatch(c -> c >= threshold);
    }

    public static Map<String, Integer> patternCounts(List<String> queries) {
        Map<String, Integer> counts = new HashMap<>();
        if (queries == null) return counts;
        for (String q : queries) {
            if (q == null) continue;
            String norm = normalize(q);
            counts.merge(norm, 1, Integer::sum);
        }
        return counts;
    }

    public static String normalize(String sql) {
        if (sql == null) return "";
        String s = sql.trim().toLowerCase(Locale.ROOT);
        // Remove line breaks and collapse whitespace
        s = WHITESPACE.matcher(s).replaceAll(" ");
        // Replace string literals with '?'
        s = STRING.matcher(s).replaceAll("?");
        // Replace numbers with '?'
        s = NUMBER.matcher(s).replaceAll("?");
        // Compress IN clause value lists to '(?)'
        s = IN_CLAUSE_VALUES.matcher(s).replaceAll("(?)");
        // Remove spaces around punctuation for consistency
        s = s.replaceAll("\\s*,\\s*", ",")
             .replaceAll("\\s*\\(\\s*", "(")
             .replaceAll("\\s*\\)\\s*", ")")
             .replaceAll("\\s*=\\s*", "=")
             .replaceAll("\\s+where\\s+", " where ")
             .replaceAll("\\s+from\\s+", " from ")
             .replaceAll("\\s+select\\s+", " select ")
             .replaceAll("\\s+join\\s+", " join ")
             .replaceAll("\\s+on\\s+", " on ");
        // Keep only the operation and the where/join clause skeleton which mostly drives n+1
        // Optionally truncate overly long strings
        if (s.length() > 400) {
            s = s.substring(0, 400);
        }
        return s;
    }
}
