package com.dave.ai.bi.helper.domain.rag.service;

import com.dave.ai.bi.helper.domain.rag.gateway.SchemaMetadataProvider;
import com.dave.ai.bi.helper.domain.rag.model.QueryIntent;
import com.dave.ai.bi.helper.domain.rag.model.SchemaRelationMeta;
import com.dave.ai.bi.helper.domain.rag.model.SchemaResolution;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the final minimal schema context passed to SQL generation and review.
 */
public class SchemaContextAssembler {

    private static final int MAX_FIELD_BLOCKS = 4;
    private static final int MAX_SQL_EXAMPLES = 2;

    private final SchemaMetadataProvider schemaRegistry;

    public SchemaContextAssembler(SchemaMetadataProvider schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public String assemble(QueryIntent intent, SchemaResolution resolution, List<Document> rankedDocuments) {
        List<Document> documents = rankedDocuments == null ? List.of() : rankedDocuments;
        StringBuilder builder = new StringBuilder();

        appendIntent(builder, intent);
        appendPrimaryFactTable(builder, resolution, documents);
        appendDimensionTables(builder, resolution, documents);
        appendFieldBlocks(builder, intent, resolution, documents);
        appendJoinPaths(builder, resolution);
        appendBusinessRules(builder, resolution, documents);
        appendSqlExamples(builder, resolution, documents);
        appendFinalScope(builder, resolution);

        return builder.toString().trim();
    }

    private void appendIntent(StringBuilder builder, QueryIntent intent) {
        builder.append("## Retrieval Intent\n");
        appendBullet(builder, "rewrittenQuery", intent.rewrittenQuery());
        appendBullet(builder, "topics", String.join(", ", intent.topics()));
        appendBullet(builder, "metrics", String.join(", ", intent.metrics()));
        appendBullet(builder, "dimensions", String.join(", ", intent.dimensions()));
        appendBullet(builder, "filters", String.join(", ", intent.filters()));
        appendBullet(builder, "queryMode", intent.queryMode());
        builder.append('\n');
    }

    private void appendPrimaryFactTable(StringBuilder builder, SchemaResolution resolution, List<Document> documents) {
        if (resolution.primaryFactTable().isBlank()) {
            return;
        }
        String content = findSingleTableOverview(resolution.primaryFactTable(), documents);
        if (content.isBlank()) {
            content = buildOverviewFallback(resolution.primaryFactTable());
        }
        if (content.isBlank()) {
            return;
        }
        builder.append("## Primary Fact Table\n")
                .append(content.trim())
                .append("\n\n");
    }

    private void appendDimensionTables(StringBuilder builder, SchemaResolution resolution, List<Document> documents) {
        List<String> dimensions = resolution.resolvedTables().stream()
                .filter(table -> !table.equals(resolution.primaryFactTable()))
                .toList();
        if (dimensions.isEmpty()) {
            return;
        }
        List<String> contents = new ArrayList<>();
        for (String tableName : dimensions) {
            String content = findSingleTableOverview(tableName, documents);
            if (content.isBlank()) {
                content = buildOverviewFallback(tableName);
            }
            if (!content.isBlank()) {
                contents.add(content.trim());
            }
        }
        if (contents.isEmpty()) {
            return;
        }
        builder.append("## Candidate Dimension Tables\n");
        for (String content : contents) {
            builder.append(content).append("\n\n");
        }
    }

    private void appendFieldBlocks(StringBuilder builder,
                                   QueryIntent intent,
                                   SchemaResolution resolution,
                                   List<Document> documents) {
        List<String> fieldBlocks = documents.stream()
                .filter(document -> "table_field_batch".equals(text(document.getMetadata().get("chunkType"))))
                .filter(document -> resolution.resolvedTables().contains(text(document.getMetadata().get("tableName"))))
                .limit(MAX_FIELD_BLOCKS)
                .map(Document::getText)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .toList();

        if (fieldBlocks.isEmpty()) {
            fieldBlocks = buildFieldFallbacks(intent, resolution);
        }
        if (fieldBlocks.isEmpty()) {
            return;
        }
        builder.append("## Required Fields\n");
        for (String fieldBlock : fieldBlocks) {
            builder.append(fieldBlock).append("\n\n");
        }
    }

    private void appendJoinPaths(StringBuilder builder, SchemaResolution resolution) {
        if (resolution.resolvedRelationMetas().isEmpty()) {
            return;
        }
        builder.append("## Join Paths\n");
        for (SchemaRelationMeta relation : resolution.resolvedRelationMetas()) {
            builder.append("- ").append(relation.expression()).append('\n');
        }
        builder.append('\n');
    }

    private void appendBusinessRules(StringBuilder builder, SchemaResolution resolution, List<Document> documents) {
        List<String> contents = documents.stream()
                .filter(document -> "business_rules".equals(text(document.getMetadata().get("sectionKey"))))
                .filter(document -> resolution.resolvedTables().contains(text(document.getMetadata().get("tableName"))))
                .map(Document::getText)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .toList();

        if (contents.isEmpty()) {
            contents = buildBusinessRuleFallbacks(resolution.resolvedTables());
        }
        if (contents.isEmpty()) {
            return;
        }
        builder.append("## Business Rules\n");
        for (String content : contents) {
            builder.append(content).append("\n\n");
        }
    }

    private void appendSqlExamples(StringBuilder builder, SchemaResolution resolution, List<Document> documents) {
        List<String> examples = documents.stream()
                .filter(document -> "sql_relation_example".equals(text(document.getMetadata().get("chunkType"))))
                .filter(document -> withinRelationScope(document, resolution))
                .limit(MAX_SQL_EXAMPLES)
                .map(Document::getText)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .toList();
        if (examples.isEmpty()) {
            return;
        }
        builder.append("## SQL Relation Examples\n");
        for (String example : examples) {
            builder.append(example).append("\n\n");
        }
    }

    private void appendFinalScope(StringBuilder builder, SchemaResolution resolution) {
        if (resolution.resolvedTables().isEmpty()) {
            return;
        }
        builder.append("## Final Table Scope\n")
                .append(String.join(", ", resolution.resolvedTables()))
                .append('\n');
    }

    private List<String> buildFieldFallbacks(QueryIntent intent, SchemaResolution resolution) {
        List<String> fallbacks = new ArrayList<>();
        for (String tableName : resolution.resolvedTables()) {
            schemaRegistry.table(tableName).ifPresent(table -> {
                LinkedHashSet<String> fields = new LinkedHashSet<>();
                fields.addAll(intersection(table.fieldNames(), intent.metrics()));
                fields.addAll(intersection(table.fieldNames(), intent.dimensions()));
                if (intent.needTime()) {
                    fields.addAll(table.timeFields());
                }
                if (!table.primaryKey().isBlank()) {
                    fields.add(table.primaryKey());
                }
                for (SchemaRelationMeta relation : resolution.resolvedRelationMetas()) {
                    if (relation.sourceTable().equals(tableName)) {
                        fields.add(relation.sourceField());
                    }
                    if (relation.targetTable().equals(tableName)) {
                        fields.add(relation.targetField());
                    }
                }
                if (fields.isEmpty()) {
                    fields.addAll(table.fieldNames().stream().limit(6).toList());
                }
                if (!fields.isEmpty()) {
                    fallbacks.add(buildFieldFallback(tableName, fields));
                }
            });
        }
        return fallbacks;
    }

    private List<String> buildBusinessRuleFallbacks(Collection<String> tableNames) {
        List<String> contents = new ArrayList<>();
        for (String tableName : tableNames) {
            schemaRegistry.table(tableName).ifPresent(table -> {
                if (table.businessRules().isEmpty()) {
                    return;
                }
                StringBuilder builder = new StringBuilder("### TABLE: ")
                        .append(table.tableName())
                        .append('\n')
                        .append("#### BUSINESS_RULES\n");
                for (String rule : table.businessRules()) {
                    builder.append("- ").append(rule).append('\n');
                }
                contents.add(builder.toString().trim());
            });
        }
        return contents;
    }

    private String findSingleTableOverview(String tableName, List<Document> documents) {
        return documents.stream()
                .filter(document -> "table_overview".equals(text(document.getMetadata().get("chunkType"))))
                .filter(document -> tableName.equals(text(document.getMetadata().get("tableName"))))
                .map(Document::getText)
                .findFirst()
                .orElse("");
    }

    private String buildOverviewFallback(String tableName) {
        return schemaRegistry.table(tableName)
                .map(table -> {
                    StringBuilder builder = new StringBuilder("### TABLE: ").append(table.tableName()).append('\n');
                    appendBlock(builder, "tableCnName", table.tableCnName());
                    appendBlock(builder, "tableType", table.tableType());
                    appendBlock(builder, "tableUsage", table.tableUsage());
                    appendBlock(builder, "primaryKey", table.primaryKey());
                    appendBlock(builder, "uniqueKey", table.uniqueKey());
                    appendBlock(builder, "topics", String.join(", ", table.topics()));
                    return builder.toString().trim();
                })
                .orElse("");
    }

    private String buildFieldFallback(String tableName, Collection<String> fields) {
        StringBuilder builder = new StringBuilder("### TABLE: ").append(tableName).append('\n')
                .append("#### REQUIRED_FIELDS\n");
        for (String field : fields) {
            builder.append("- ").append(field).append('\n');
        }
        return builder.toString().trim();
    }

    private boolean withinRelationScope(Document document, SchemaResolution resolution) {
        String sourceTable = text(document.getMetadata().get("sourceTable"));
        String targetTable = text(document.getMetadata().get("targetTable"));
        return resolution.resolvedTables().contains(sourceTable) && resolution.resolvedTables().contains(targetTable);
    }

    private List<String> intersection(Collection<String> left, Collection<String> right) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String item : left) {
            for (String candidate : right) {
                if (item.equalsIgnoreCase(candidate)) {
                    values.add(item);
                }
            }
        }
        return new ArrayList<>(values);
    }

    private void appendBullet(StringBuilder builder, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append("- ").append(key).append(": ").append(value).append('\n');
    }

    private void appendBlock(StringBuilder builder, String title, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append("#### ").append(title).append('\n')
                .append(value).append('\n');
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
