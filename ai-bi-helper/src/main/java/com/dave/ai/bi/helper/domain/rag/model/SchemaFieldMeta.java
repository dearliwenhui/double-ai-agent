package com.dave.ai.bi.helper.domain.rag.model;

/**
 * Canonical field metadata built from schema chunks.
 */
public record SchemaFieldMeta(String tableName,
                              String fieldName,
                              boolean metric,
                              boolean dimension,
                              boolean time,
                              boolean primaryLike) {

    public SchemaFieldMeta {
        tableName = normalize(tableName);
        fieldName = normalize(fieldName);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
