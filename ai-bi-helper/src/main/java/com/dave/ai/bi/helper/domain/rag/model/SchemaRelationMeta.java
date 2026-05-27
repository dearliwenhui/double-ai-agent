package com.dave.ai.bi.helper.domain.rag.model;

import java.util.List;

/**
 * Canonical join relation metadata built from schema chunks.
 */
public record SchemaRelationMeta(String sourceTable,
                                 String sourceField,
                                 String targetTable,
                                 String targetField,
                                 String relationType) {

    public SchemaRelationMeta {
        sourceTable = normalize(sourceTable);
        sourceField = normalize(sourceField);
        targetTable = normalize(targetTable);
        targetField = normalize(targetField);
        relationType = normalize(relationType);
        if (relationType.isBlank()) {
            relationType = "join";
        }
    }

    public String expression() {
        return sourceTable + "." + sourceField + " -> " + targetTable + "." + targetField;
    }

    public boolean involves(String tableName) {
        return sourceTable.equalsIgnoreCase(normalize(tableName)) || targetTable.equalsIgnoreCase(normalize(tableName));
    }

    public String otherTable(String tableName) {
        String normalized = normalize(tableName);
        if (sourceTable.equalsIgnoreCase(normalized)) {
            return targetTable;
        }
        if (targetTable.equalsIgnoreCase(normalized)) {
            return sourceTable;
        }
        return "";
    }

    public List<String> tables() {
        return List.of(sourceTable, targetTable);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
