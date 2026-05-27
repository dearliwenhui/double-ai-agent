package com.dave.ai.bi.helper.domain.rag.service;

import com.dave.ai.bi.helper.domain.rag.gateway.SchemaMetadataProvider;
import com.dave.ai.bi.helper.domain.rag.model.SchemaFieldMeta;
import com.dave.ai.bi.helper.domain.rag.model.SchemaRelationMeta;
import com.dave.ai.bi.helper.domain.rag.model.SchemaTableMeta;
import com.dave.ai.bi.helper.infrastructure.rag.repository.SchemaDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Domain service for schema metadata lookup.
 */
@Service
public class SchemaRegistry implements SchemaMetadataProvider {

    private final SchemaDocumentRepository repository;

    public SchemaRegistry(SchemaDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SchemaTableMeta> table(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(repository.tables().get(tableName));
    }

    @Override
    public boolean hasTable(String tableName) {
        return table(tableName).isPresent();
    }

    @Override
    public List<SchemaTableMeta> allTables() {
        return List.copyOf(repository.tables().values());
    }

    @Override
    public List<SchemaRelationMeta> allRelations() {
        return repository.relations();
    }

    @Override
    public List<SchemaRelationMeta> relationsFor(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return List.of();
        }
        return repository.relationIndex().getOrDefault(tableName, List.of());
    }

    @Override
    public List<String> availableTopics() {
        LinkedHashSet<String> topics = new LinkedHashSet<>();
        for (SchemaTableMeta table : repository.tables().values()) {
            topics.addAll(table.topics());
        }
        return new ArrayList<>(topics);
    }

    @Override
    public List<String> metricFieldNames() {
        return collectFieldNames(field -> field.metric());
    }

    @Override
    public List<String> dimensionFieldNames() {
        return collectFieldNames(field -> field.dimension());
    }

    @Override
    public List<String> timeFieldNames() {
        return collectFieldNames(field -> field.time());
    }

    @Override
    public List<String> findTablesByField(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return List.of();
        }
        String normalized = normalize(fieldName);
        LinkedHashSet<String> tables = new LinkedHashSet<>();
        for (SchemaFieldMeta field : repository.fields().values()) {
            if (normalize(field.fieldName()).equals(normalized)) {
                tables.add(field.tableName());
            }
        }
        return new ArrayList<>(tables);
    }

    public List<String> findTablesByTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return List.of();
        }
        String normalized = normalize(topic);
        LinkedHashSet<String> tableNames = new LinkedHashSet<>();
        for (SchemaTableMeta table : repository.tables().values()) {
            for (String candidate : table.topics()) {
                if (normalize(candidate).equals(normalized)) {
                    tableNames.add(table.tableName());
                    break;
                }
            }
        }
        return new ArrayList<>(tableNames);
    }

    public String preferredFactTable(List<String> topics) {
        Set<String> normalizedTopics = new LinkedHashSet<>();
        if (topics != null) {
            for (String topic : topics) {
                String normalized = normalize(topic);
                if (!normalized.isBlank()) {
                    normalizedTopics.add(normalized);
                }
            }
        }
        for (SchemaTableMeta table : repository.tables().values()) {
            if (!table.isFactTable()) {
                continue;
            }
            for (String topic : table.topics()) {
                if (normalizedTopics.contains(normalize(topic))) {
                    return table.tableName();
                }
            }
        }
        if (hasTable("fact_sales")) {
            return "fact_sales";
        }
        return repository.tables().values().stream()
                .filter(SchemaTableMeta::isFactTable)
                .map(SchemaTableMeta::tableName)
                .findFirst()
                .orElse("");
    }

    @Override
    public List<String> matchTableNames(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        String normalized = normalize(input);
        LinkedHashSet<String> matches = new LinkedHashSet<>();
        for (String tableName : repository.tables().keySet()) {
            if (normalized.contains(normalize(tableName))) {
                matches.add(tableName);
            }
        }
        return new ArrayList<>(matches);
    }

    private List<String> collectFieldNames(java.util.function.Predicate<SchemaFieldMeta> filter) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (SchemaFieldMeta field : repository.fields().values()) {
            if (filter.test(field)) {
                result.add(field.fieldName());
            }
        }
        return new ArrayList<>(result);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
