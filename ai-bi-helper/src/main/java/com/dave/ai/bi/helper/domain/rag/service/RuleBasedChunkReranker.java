package com.dave.ai.bi.helper.domain.rag.service;

import com.dave.ai.bi.helper.domain.rag.model.QueryIntent;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight rule-based reranker for schema chunks.
 */
public class RuleBasedChunkReranker {

    public List<Document> rerank(QueryIntent queryIntent,
                                 List<Document> documents,
                                 List<String> resolvedTables,
                                 List<String> resolvedRelations,
                                 String primaryFactTable,
                                 int topN) {
        List<Document> ranked = new ArrayList<>(documents);
        ranked.sort(Comparator
                .comparingInt((Document document) -> score(queryIntent, document, resolvedTables, resolvedRelations, primaryFactTable))
                .reversed()
                .thenComparing(document -> text(document.getId())));
        if (ranked.size() <= topN) {
            return ranked;
        }
        return new ArrayList<>(ranked.subList(0, topN));
    }

    private int score(QueryIntent intent,
                      Document document,
                      List<String> resolvedTables,
                      List<String> resolvedRelations,
                      String primaryFactTable) {
        int score = 0;
        Map<String, Object> metadata = document.getMetadata();
        String chunkType = text(metadata.get("chunkType"));
        String sectionKey = text(metadata.get("sectionKey"));
        String tableName = text(metadata.get("tableName"));
        String sourceTable = text(metadata.get("sourceTable"));
        String targetTable = text(metadata.get("targetTable"));
        String documentText = normalize(document.getText());

        Set<String> topicValues = normalizeSet(metadata.get("topics"));
        Set<String> fieldNames = normalizeSet(metadata.get("fieldNames"));
        Set<String> metricFields = normalizeSet(metadata.get("metricFields"));
        Set<String> dimensionFields = normalizeSet(metadata.get("dimensionFields"));
        Set<String> timeFields = normalizeSet(metadata.get("timeFields"));
        Set<String> relationTables = normalizeSet(metadata.get("relationTables"));
        Set<String> resolvedTableSet = normalizeSet(resolvedTables);
        Set<String> resolvedRelationSet = normalizeSet(resolvedRelations);

        if ("table_relation".equals(chunkType) || "sql_relation_example".equals(chunkType)) {
            score += 140;
        }
        if ("table_field_batch".equals(chunkType)) {
            score += 90;
        }
        if ("table_overview".equals(chunkType)) {
            score += 70;
        }
        if ("business_rules".equals(sectionKey)) {
            score += 55;
        }
        if ("query_scenarios".equals(sectionKey)) {
            score -= 20;
        }

        if (!primaryFactTable.isBlank() && primaryFactTable.equalsIgnoreCase(tableName)) {
            score += 180;
        }
        if (resolvedTableSet.contains(normalize(tableName))) {
            score += 110;
        }
        if (!sourceTable.isBlank() && !targetTable.isBlank()
                && resolvedTableSet.contains(normalize(sourceTable))
                && resolvedTableSet.contains(normalize(targetTable))) {
            score += 130;
        }
        if (!relationTables.isEmpty() && resolvedTableSet.containsAll(relationTables)) {
            score += 90;
        }

        score += matchScore(intent.topics(), topicValues, documentText, 45);
        score += matchScore(intent.metrics(), union(fieldNames, metricFields), documentText, 70);
        score += matchScore(intent.dimensions(), union(fieldNames, dimensionFields), documentText, 60);
        score += matchScore(intent.filters(), fieldNames, documentText, 35);

        if (intent.needTime() && (!timeFields.isEmpty() || containsAny(documentText, "dim_date", "date_id", "year", "month", "day"))) {
            score += 95;
        }
        if (intent.needRelation() && ("table_relation".equals(chunkType) || "sql_relation_example".equals(chunkType))) {
            score += 90;
        }
        if (intent.needAggregation() && containsAny(documentText, "sum", "count", "avg", "sales_amount", "quantity", "discount")) {
            score += 50;
        }
        if ("trend".equalsIgnoreCase(intent.queryMode()) && containsAny(documentText, "year", "month", "quarter", "day", "trend")) {
            score += 40;
        }

        String relationExpression = normalize(sourceTable + "." + text(metadata.get("sourceField"))
                + " -> " + targetTable + "." + text(metadata.get("targetField")));
        if (resolvedRelationSet.contains(relationExpression)) {
            score += 150;
        }

        return score;
    }

    private int matchScore(Collection<String> hints, Set<String> metadataTokens, String documentText, int weight) {
        int score = 0;
        for (String hint : hints) {
            String normalized = normalize(hint);
            if (normalized.isBlank()) {
                continue;
            }
            if (metadataTokens.contains(normalized) || documentText.contains(normalized)) {
                score += weight;
            }
        }
        return score;
    }

    private Set<String> union(Set<String> left, Set<String> right) {
        LinkedHashSet<String> result = new LinkedHashSet<>(left);
        result.addAll(right);
        return result;
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private Set<String> normalizeSet(Object value) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                String normalized = normalize(String.valueOf(item));
                if (!normalized.isBlank()) {
                    result.add(normalized);
                }
            }
            return result;
        }
        if (value instanceof String stringValue) {
            String normalized = stringValue.trim();
            if (normalized.startsWith("[") && normalized.endsWith("]")) {
                normalized = normalized.substring(1, normalized.length() - 1);
            }
            for (String part : normalized.split(",")) {
                String token = normalize(part);
                if (!token.isBlank()) {
                    result.add(token);
                }
            }
        }
        return result;
    }

    private Set<String> normalizeSet(Collection<String> values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String normalize(String value) {
        return text(value).toLowerCase(Locale.ROOT);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
