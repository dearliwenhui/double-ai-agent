package com.dave.ai.bi.helper.domain.rag.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Canonical table metadata built from schema chunks.
 */
public record SchemaTableMeta(String tableName,
                              String tableCnName,
                              String tableType,
                              String tableUsage,
                              String primaryKey,
                              String uniqueKey,
                              List<String> topics,
                              List<String> fieldNames,
                              List<String> metricFields,
                              List<String> dimensionFields,
                              List<String> timeFields,
                              List<String> businessRules) {

    public SchemaTableMeta {
        tableName = normalizeText(tableName);
        tableCnName = normalizeText(tableCnName);
        tableType = normalizeText(tableType);
        tableUsage = normalizeText(tableUsage);
        primaryKey = normalizeText(primaryKey);
        uniqueKey = normalizeText(uniqueKey);
        topics = immutableDistinct(topics);
        fieldNames = immutableDistinct(fieldNames);
        metricFields = immutableDistinct(metricFields);
        dimensionFields = immutableDistinct(dimensionFields);
        timeFields = immutableDistinct(timeFields);
        businessRules = immutableDistinct(businessRules);
    }

    public boolean isFactTable() {
        return tableName.startsWith("fact_");
    }

    public boolean isDimensionTable() {
        return tableName.startsWith("dim_");
    }

    public boolean hasField(String fieldName) {
        String normalized = normalizeText(fieldName);
        return fieldNames.stream().anyMatch(field -> field.equalsIgnoreCase(normalized));
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
