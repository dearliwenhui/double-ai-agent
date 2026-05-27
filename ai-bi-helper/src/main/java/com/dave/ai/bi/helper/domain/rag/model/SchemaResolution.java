package com.dave.ai.bi.helper.domain.rag.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Final schema scope resolved for a single user query.
 */
public record SchemaResolution(String primaryFactTable,
                               List<String> resolvedTables,
                               List<SchemaRelationMeta> resolvedRelationMetas) {

    public SchemaResolution {
        primaryFactTable = normalizeText(primaryFactTable);
        resolvedTables = immutableDistinct(resolvedTables);
        resolvedRelationMetas = resolvedRelationMetas == null ? List.of() : List.copyOf(resolvedRelationMetas);
    }

    public List<String> resolvedRelations() {
        return resolvedRelationMetas.stream()
                .map(SchemaRelationMeta::expression)
                .distinct()
                .toList();
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
