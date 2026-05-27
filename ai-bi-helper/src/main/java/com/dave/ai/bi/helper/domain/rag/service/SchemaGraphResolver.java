package com.dave.ai.bi.helper.domain.rag.service;

import com.dave.ai.bi.helper.domain.rag.gateway.SchemaMetadataProvider;
import com.dave.ai.bi.helper.domain.rag.model.QueryIntent;
import com.dave.ai.bi.helper.domain.rag.model.SchemaRelationMeta;
import com.dave.ai.bi.helper.domain.rag.model.SchemaResolution;
import com.dave.ai.bi.helper.domain.rag.model.SchemaTableMeta;
import org.springframework.ai.document.Document;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Resolves a minimum schema scope from retrieval hits plus registry metadata.
 */
public class SchemaGraphResolver {

    private static final int MAX_DIMENSION_TABLES = 4;

    private final SchemaMetadataProvider schemaRegistry;

    public SchemaGraphResolver(SchemaMetadataProvider schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public SchemaResolution resolve(QueryIntent intent,
                                    List<Document> tableDocs,
                                    List<Document> fieldDocs,
                                    List<Document> relationDocs) {
        Map<String, Integer> scoreByTable = new LinkedHashMap<>();
        boostTopicTables(intent, scoreByTable);
        boostFieldTables(intent.metrics(), 90, scoreByTable);
        boostFieldTables(intent.dimensions(), 75, scoreByTable);
        boostFieldTables(intent.filters(), 35, scoreByTable);
        boostExplicitTableMentions(intent.rewrittenQuery(), scoreByTable);
        if (intent.needTime() && schemaRegistry.hasTable("dim_date")) {
            boost(scoreByTable, "dim_date", 95);
        }

        scoreDocuments(tableDocs, scoreByTable, 90, 70, 55);
        scoreDocuments(fieldDocs, scoreByTable, 70, 60, 50);
        scoreDocuments(relationDocs, scoreByTable, 55, 50, 45);

        String anchorTable = chooseAnchorTable(intent, scoreByTable);
        LinkedHashSet<String> requestedTables = new LinkedHashSet<>();
        if (!anchorTable.isBlank()) {
            requestedTables.add(anchorTable);
        }
        requestedTables.addAll(selectDimensionTables(scoreByTable, anchorTable));
        requestedTables.addAll(extractRelationTables(relationDocs));
        requestedTables.addAll(resolveFieldDrivenTables(intent));

        if (intent.needTime() && schemaRegistry.hasTable("dim_date")) {
            requestedTables.add("dim_date");
        }
        requestedTables.removeIf(table -> !schemaRegistry.hasTable(table));

        if (anchorTable.isBlank() && !requestedTables.isEmpty()) {
            anchorTable = requestedTables.iterator().next();
        }

        List<SchemaRelationMeta> resolvedRelationMetas = resolveJoinPaths(anchorTable, requestedTables);
        LinkedHashSet<String> resolvedTables = new LinkedHashSet<>();
        if (!anchorTable.isBlank()) {
            resolvedTables.add(anchorTable);
        }
        for (SchemaRelationMeta relation : resolvedRelationMetas) {
            resolvedTables.add(relation.sourceTable());
            resolvedTables.add(relation.targetTable());
        }
        for (String requestedTable : requestedTables) {
            if (anchorTable.isBlank() || requestedTable.equals(anchorTable) || hasPath(anchorTable, requestedTable)) {
                resolvedTables.add(requestedTable);
            }
        }

        if (resolvedTables.isEmpty() && !anchorTable.isBlank()) {
            resolvedTables.add(anchorTable);
        }

        return new SchemaResolution(anchorTable, new ArrayList<>(resolvedTables), resolvedRelationMetas);
    }

    private void boostTopicTables(QueryIntent intent, Map<String, Integer> scoreByTable) {
        for (String topic : intent.topics()) {
            for (String tableName : schemaRegistry.findTablesByTopic(topic)) {
                int weight = tableName.startsWith("fact_") ? 120 : 70;
                boost(scoreByTable, tableName, weight);
            }
        }
    }

    private void boostFieldTables(List<String> fieldHints, int weight, Map<String, Integer> scoreByTable) {
        for (String fieldHint : fieldHints) {
            for (String tableName : schemaRegistry.findTablesByField(fieldHint)) {
                boost(scoreByTable, tableName, weight);
            }
        }
    }

    private void boostExplicitTableMentions(String query, Map<String, Integer> scoreByTable) {
        for (String tableName : schemaRegistry.matchTableNames(query)) {
            boost(scoreByTable, tableName, 150);
        }
    }

    private void scoreDocuments(List<Document> documents,
                                Map<String, Integer> scoreByTable,
                                int tableWeight,
                                int sourceWeight,
                                int targetWeight) {
        if (documents == null) {
            return;
        }
        for (Document document : documents) {
            if (document == null) {
                continue;
            }
            Map<String, Object> metadata = document.getMetadata();
            String tableName = text(metadata.get("tableName"));
            String sourceTable = text(metadata.get("sourceTable"));
            String targetTable = text(metadata.get("targetTable"));
            if (!tableName.isBlank()) {
                boost(scoreByTable, tableName, tableWeight);
            }
            if (!sourceTable.isBlank()) {
                boost(scoreByTable, sourceTable, sourceWeight);
            }
            if (!targetTable.isBlank()) {
                boost(scoreByTable, targetTable, targetWeight);
            }
        }
    }

    private String chooseAnchorTable(QueryIntent intent, Map<String, Integer> scoreByTable) {
        String preferredFact = schemaRegistry.preferredFactTable(intent.topics());
        if (!preferredFact.isBlank() && scoreByTable.containsKey(preferredFact)) {
            return preferredFact;
        }

        return scoreByTable.entrySet().stream()
                .filter(entry -> schemaRegistry.table(entry.getKey()).map(SchemaTableMeta::isFactTable).orElse(false))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseGet(() -> {
                    if (!preferredFact.isBlank()) {
                        return preferredFact;
                    }
                    return scoreByTable.entrySet().stream()
                            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                                    .thenComparing(Map.Entry::getKey))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse("");
                });
    }

    private List<String> selectDimensionTables(Map<String, Integer> scoreByTable, String anchorTable) {
        return scoreByTable.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(anchorTable))
                .filter(entry -> schemaRegistry.table(entry.getKey()).map(table -> !table.isFactTable()).orElse(true))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(MAX_DIMENSION_TABLES)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<String> resolveFieldDrivenTables(QueryIntent intent) {
        LinkedHashSet<String> tables = new LinkedHashSet<>();
        for (String field : intent.metrics()) {
            tables.addAll(schemaRegistry.findTablesByField(field));
        }
        for (String field : intent.dimensions()) {
            tables.addAll(schemaRegistry.findTablesByField(field));
        }
        if (intent.needTime()) {
            tables.addAll(schemaRegistry.findTablesByField("date_id"));
            tables.addAll(schemaRegistry.findTablesByField("year"));
            tables.addAll(schemaRegistry.findTablesByField("month"));
        }
        return new ArrayList<>(tables);
    }

    private List<String> extractRelationTables(List<Document> relationDocs) {
        if (relationDocs == null || relationDocs.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> tables = new LinkedHashSet<>();
        for (Document document : relationDocs) {
            Map<String, Object> metadata = document.getMetadata();
            addIfHasText(tables, text(metadata.get("tableName")));
            addIfHasText(tables, text(metadata.get("sourceTable")));
            addIfHasText(tables, text(metadata.get("targetTable")));
        }
        return new ArrayList<>(tables);
    }

    private List<SchemaRelationMeta> resolveJoinPaths(String anchorTable, Set<String> requestedTables) {
        if (anchorTable == null || anchorTable.isBlank() || requestedTables == null || requestedTables.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, SchemaRelationMeta> resolved = new LinkedHashMap<>();
        for (String tableName : requestedTables) {
            if (tableName.equals(anchorTable)) {
                continue;
            }
            List<SchemaRelationMeta> path = shortestPath(anchorTable, tableName);
            for (SchemaRelationMeta relation : path) {
                resolved.putIfAbsent(relation.expression(), relation);
            }
        }
        return new ArrayList<>(resolved.values());
    }

    private boolean hasPath(String anchorTable, String targetTable) {
        if (anchorTable == null || anchorTable.isBlank() || targetTable == null || targetTable.isBlank()) {
            return false;
        }
        if (anchorTable.equals(targetTable)) {
            return true;
        }
        return !shortestPath(anchorTable, targetTable).isEmpty();
    }

    private List<SchemaRelationMeta> shortestPath(String startTable, String endTable) {
        if (startTable.equals(endTable)) {
            return List.of();
        }
        Queue<String> queue = new ArrayDeque<>();
        Map<String, String> previousTable = new HashMap<>();
        Map<String, SchemaRelationMeta> previousRelation = new HashMap<>();

        queue.add(startTable);
        previousTable.put(startTable, startTable);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (SchemaRelationMeta relation : schemaRegistry.relationsFor(current)) {
                String nextTable = relation.otherTable(current);
                if (nextTable.isBlank() || previousTable.containsKey(nextTable)) {
                    continue;
                }
                previousTable.put(nextTable, current);
                previousRelation.put(nextTable, relation);
                if (nextTable.equals(endTable)) {
                    return buildPath(startTable, endTable, previousTable, previousRelation);
                }
                queue.add(nextTable);
            }
        }
        return List.of();
    }

    private List<SchemaRelationMeta> buildPath(String startTable,
                                               String endTable,
                                               Map<String, String> previousTable,
                                               Map<String, SchemaRelationMeta> previousRelation) {
        ArrayList<SchemaRelationMeta> path = new ArrayList<>();
        String current = endTable;
        while (!current.equals(startTable)) {
            SchemaRelationMeta relation = previousRelation.get(current);
            if (relation == null) {
                return List.of();
            }
            path.add(0, relation);
            current = previousTable.get(current);
            if (current == null || current.isBlank()) {
                return List.of();
            }
        }
        return path;
    }

    private void boost(Map<String, Integer> scoreByTable, String tableName, int delta) {
        if (tableName == null || tableName.isBlank()) {
            return;
        }
        scoreByTable.merge(tableName, delta, Integer::sum);
    }

    private void addIfHasText(Set<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value);
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
