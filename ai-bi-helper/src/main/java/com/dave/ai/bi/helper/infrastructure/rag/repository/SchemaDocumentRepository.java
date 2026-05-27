package com.dave.ai.bi.helper.infrastructure.rag.repository;

import com.dave.ai.bi.helper.domain.rag.model.SchemaFieldMeta;
import com.dave.ai.bi.helper.domain.rag.model.SchemaRelationMeta;
import com.dave.ai.bi.helper.domain.rag.model.SchemaTableMeta;
import com.dave.ai.bi.helper.spliter.BiSchemaMarkdownSplitter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SchemaDocumentRepository {

    private final BiSchemaMarkdownSplitter splitter;
    private final SchemaRegistryBuilder builder;

    private volatile Map<String, SchemaTableMeta> tables = Map.of();
    private volatile Map<String, SchemaFieldMeta> fields = Map.of();
    private volatile List<SchemaRelationMeta> relations = List.of();
    private volatile Map<String, List<SchemaRelationMeta>> relationIndex = Map.of();

    public SchemaDocumentRepository(BiSchemaMarkdownSplitter splitter) {
        this.splitter = splitter;
        this.builder = new SchemaRegistryBuilder();
    }

    @PostConstruct
    public void loadDefaultSchema() {
        ClassPathResource resource = new ClassPathResource("document/schema.md");
        if (!resource.exists()) {
            log.warn("Default schema.md was not found on classpath.");
            return;
        }
        try {
            String markdown = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            refreshFromMarkdown(markdown, Map.of(
                    "fileName", "schema.md",
                    "source", "classpath:document/schema.md"
            ));
        } catch (IOException ex) {
            log.warn("Failed to load default schema registry from classpath schema.md", ex);
        }
    }

    public synchronized void refreshFromMarkdown(String markdown, Map<String, Object> rootMetadata) {
        if (markdown == null || markdown.isBlank()) {
            return;
        }
        List<Document> documents = splitter.split(markdown, rootMetadata == null ? Map.of() : rootMetadata);
        refreshFromDocuments(documents);
    }

    public synchronized void refreshFromDocuments(List<Document> documents) {
        SchemaRegistryBuilder.SchemaRegistryData data = builder.build(documents);
        if (data.tables().isEmpty() && data.relations().isEmpty()) {
            log.warn("Skipping schema registry refresh because no schema tables or relations were discovered.");
            return;
        }
        this.tables = data.tables();
        this.fields = data.fields();
        this.relations = data.relations();
        this.relationIndex = buildRelationIndex(data.relations());
        log.info("Schema registry refreshed. tables={}, fields={}, relations={}",
                tables.size(), fields.size(), relations.size());
    }

    public Map<String, SchemaTableMeta> tables() {
        return tables;
    }

    public Map<String, SchemaFieldMeta> fields() {
        return fields;
    }

    public List<SchemaRelationMeta> relations() {
        return relations;
    }

    public Map<String, List<SchemaRelationMeta>> relationIndex() {
        return relationIndex;
    }

    private Map<String, List<SchemaRelationMeta>> buildRelationIndex(List<SchemaRelationMeta> relations) {
        Map<String, List<SchemaRelationMeta>> index = new LinkedHashMap<>();
        for (SchemaRelationMeta relation : relations) {
            index.computeIfAbsent(relation.sourceTable(), key -> new ArrayList<>()).add(relation);
            index.computeIfAbsent(relation.targetTable(), key -> new ArrayList<>()).add(relation);
        }
        Map<String, List<SchemaRelationMeta>> immutable = new LinkedHashMap<>();
        for (Map.Entry<String, List<SchemaRelationMeta>> entry : index.entrySet()) {
            immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(immutable);
    }
}
