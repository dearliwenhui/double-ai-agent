package com.dave.ai.bi.helper.domain.rag.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Structured retrieval intent used by the schema routing pipeline.
 *
 * @param rewrittenQuery  condensed retrieval-friendly query
 * @param topics          business topics or domains
 * @param metrics         metric hints
 * @param dimensions      dimension hints
 * @param filters         filter hints
 * @param needTime        whether time context is required
 * @param needRelation    whether multi-table relation context is required
 * @param needAggregation whether the query needs aggregation semantics
 * @param queryMode       detail / aggregate / trend
 */
public record QueryIntent(String rewrittenQuery,
                          List<String> topics,
                          List<String> metrics,
                          List<String> dimensions,
                          List<String> filters,
                          boolean needTime,
                          boolean needRelation,
                          boolean needAggregation,
                          String queryMode) {

    public QueryIntent {
        rewrittenQuery = normalizeText(rewrittenQuery);
        topics = immutableDistinct(topics);
        metrics = immutableDistinct(metrics);
        dimensions = immutableDistinct(dimensions);
        filters = immutableDistinct(filters);
        queryMode = normalizeText(queryMode);
        if (queryMode.isBlank()) {
            queryMode = "detail";
        }
    }

    public List<String> keywords() {
        Set<String> values = new LinkedHashSet<>();
        values.addAll(topics);
        values.addAll(metrics);
        values.addAll(dimensions);
        values.addAll(filters);
        if (!rewrittenQuery.isBlank()) {
            values.add(rewrittenQuery);
        }
        return List.copyOf(values);
    }

    private static List<String> immutableDistinct(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeText(value);
            if (!normalized.isBlank()) {
                unique.add(normalized);
            }
        }
        return List.copyOf(new ArrayList<>(unique));
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
