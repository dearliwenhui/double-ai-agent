package com.dave.ai.bi.helper.infrastructure.rag.repository;

import com.dave.ai.bi.helper.domain.rag.model.SchemaFieldMeta;
import com.dave.ai.bi.helper.domain.rag.model.SchemaRelationMeta;
import com.dave.ai.bi.helper.domain.rag.model.SchemaTableMeta;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class SchemaRegistryBuilder {

    SchemaRegistryData build(List<Document> documents) {
        Map<String, MutableTableMeta> tables = new LinkedHashMap<>();
        Map<String, SchemaFieldMeta> fields = new LinkedHashMap<>();
        Map<String, SchemaRelationMeta> relations = new LinkedHashMap<>();

        if (documents == null) {
            return new SchemaRegistryData(Map.of(), Map.of(), List.of());
        }

        for (Document document : documents) {
            if (document == null) {
                continue;
            }
            Map<String, Object> metadata = document.getMetadata();
            String tableName = text(metadata.get("tableName"));
            String chunkType = text(metadata.get("chunkType"));
            String sectionKey = text(metadata.get("sectionKey"));

            if (!tableName.isBlank()) {
                MutableTableMeta table = tables.computeIfAbsent(tableName, MutableTableMeta::new);
                table.tableCnName = firstNonBlank(table.tableCnName, text(metadata.get("tableCnName")));
                table.tableType = firstNonBlank(table.tableType, text(metadata.get("tableType")));
                table.tableUsage = firstNonBlank(table.tableUsage, text(metadata.get("tableUsage")));
                table.primaryKey = firstNonBlank(table.primaryKey, text(metadata.get("primaryKey")));
                table.uniqueKey = firstNonBlank(table.uniqueKey, text(metadata.get("uniqueKey")));
                table.topics.addAll(inferTopics(tableName));
                table.topics.addAll(toStringList(metadata.get("topics")));
            }

            if ("table_field_batch".equals(chunkType) && !tableName.isBlank()) {
                MutableTableMeta table = tables.computeIfAbsent(tableName, MutableTableMeta::new);
                List<String> fieldNames = toStringList(metadata.get("fieldNames"));
                List<String> metricFields = toStringList(metadata.get("metricFields"));
                List<String> dimensionFields = toStringList(metadata.get("dimensionFields"));
                List<String> timeFields = toStringList(metadata.get("timeFields"));

                table.fieldNames.addAll(fieldNames);
                table.metricFields.addAll(metricFields);
                table.dimensionFields.addAll(dimensionFields);
                table.timeFields.addAll(timeFields);

                for (String fieldName : fieldNames) {
                    fields.put(fieldKey(tableName, fieldName), new SchemaFieldMeta(
                            tableName,
                            fieldName,
                            containsIgnoreCase(metricFields, fieldName),
                            containsIgnoreCase(dimensionFields, fieldName),
                            containsIgnoreCase(timeFields, fieldName),
                            isPrimaryLike(fieldName)
                    ));
                }
            }

            if ("business_rules".equals(sectionKey) && !tableName.isBlank()) {
                MutableTableMeta table = tables.computeIfAbsent(tableName, MutableTableMeta::new);
                table.businessRules.addAll(extractBusinessRules(document.getText()));
            }

            if (isRelationChunk(chunkType)) {
                String sourceTable = text(metadata.get("sourceTable"));
                String sourceField = text(metadata.get("sourceField"));
                String targetTable = text(metadata.get("targetTable"));
                String targetField = text(metadata.get("targetField"));
                if (!sourceTable.isBlank() && !sourceField.isBlank() && !targetTable.isBlank() && !targetField.isBlank()) {
                    SchemaRelationMeta relation = new SchemaRelationMeta(
                            sourceTable,
                            sourceField,
                            targetTable,
                            targetField,
                            text(metadata.get("relationType"))
                    );
                    relations.putIfAbsent(relation.expression(), relation);
                    tables.computeIfAbsent(sourceTable, MutableTableMeta::new).topics.addAll(inferTopics(sourceTable));
                    tables.computeIfAbsent(targetTable, MutableTableMeta::new).topics.addAll(inferTopics(targetTable));
                }
            }
        }

        Map<String, SchemaTableMeta> immutableTables = new LinkedHashMap<>();
        for (MutableTableMeta table : tables.values()) {
            immutableTables.put(table.tableName, table.toSchemaTableMeta());
        }
        return new SchemaRegistryData(
                Collections.unmodifiableMap(new LinkedHashMap<>(immutableTables)),
                Collections.unmodifiableMap(new LinkedHashMap<>(fields)),
                List.copyOf(relations.values())
        );
    }

    private boolean containsIgnoreCase(Collection<String> candidates, String value) {
        String normalized = normalize(value);
        for (String candidate : candidates) {
            if (normalize(candidate).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private List<String> inferTopics(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> topics = new LinkedHashSet<>();
        String normalized = tableName.toLowerCase(Locale.ROOT);
        String[] parts = normalized.split("_");
        for (String part : parts) {
            if (part.isBlank() || "fact".equals(part) || "dim".equals(part)
                    || "ods".equals(part) || "dwd".equals(part) || "dws".equals(part) || "ads".equals(part)) {
                continue;
            }
            topics.add(part);
        }
        if (normalized.contains("date") || normalized.contains("time")) {
            topics.add("time");
        }
        return new ArrayList<>(topics);
    }

    private List<String> extractBusinessRules(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> rules = new LinkedHashSet<>();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-")) {
                String value = trimmed.substring(1).trim();
                if (!value.isBlank()) {
                    rules.add(value);
                }
            }
        }
        return new ArrayList<>(rules);
    }

    private boolean isRelationChunk(String chunkType) {
        return "table_relation".equals(chunkType) || "sql_relation_example".equals(chunkType);
    }

    private List<String> toStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            LinkedHashSet<String> result = new LinkedHashSet<>();
            for (Object item : collection) {
                String text = normalize(String.valueOf(item));
                if (!text.isBlank()) {
                    result.add(text);
                }
            }
            return new ArrayList<>(result);
        }
        String text = normalize(String.valueOf(value));
        if (text.isBlank()) {
            return List.of();
        }
        if (text.startsWith("[") && text.endsWith("]")) {
            text = text.substring(1, text.length() - 1);
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String part : text.split(",")) {
            String normalized = normalize(part);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return new ArrayList<>(result);
    }

    private boolean isPrimaryLike(String fieldName) {
        String normalized = normalize(fieldName);
        return normalized.endsWith("_id") || "id".equals(normalized);
    }

    private String fieldKey(String tableName, String fieldName) {
        return normalize(tableName) + "::" + normalize(fieldName);
    }

    private String firstNonBlank(String left, String right) {
        return left != null && !left.isBlank() ? left : right;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    record SchemaRegistryData(Map<String, SchemaTableMeta> tables,
                              Map<String, SchemaFieldMeta> fields,
                              List<SchemaRelationMeta> relations) {
    }

    private static final class MutableTableMeta {
        private final String tableName;
        private String tableCnName = "";
        private String tableType = "";
        private String tableUsage = "";
        private String primaryKey = "";
        private String uniqueKey = "";
        private final LinkedHashSet<String> topics = new LinkedHashSet<>();
        private final LinkedHashSet<String> fieldNames = new LinkedHashSet<>();
        private final LinkedHashSet<String> metricFields = new LinkedHashSet<>();
        private final LinkedHashSet<String> dimensionFields = new LinkedHashSet<>();
        private final LinkedHashSet<String> timeFields = new LinkedHashSet<>();
        private final LinkedHashSet<String> businessRules = new LinkedHashSet<>();

        private MutableTableMeta(String tableName) {
            this.tableName = tableName;
        }

        private SchemaTableMeta toSchemaTableMeta() {
            return new SchemaTableMeta(
                    tableName,
                    tableCnName,
                    tableType,
                    tableUsage,
                    primaryKey,
                    uniqueKey,
                    List.copyOf(topics),
                    List.copyOf(fieldNames),
                    List.copyOf(metricFields),
                    List.copyOf(dimensionFields),
                    List.copyOf(timeFields),
                    List.copyOf(businessRules)
            );
        }
    }
}
