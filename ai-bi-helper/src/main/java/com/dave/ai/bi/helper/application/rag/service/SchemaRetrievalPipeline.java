package com.dave.ai.bi.helper.application.rag.service;

import com.dave.ai.bi.helper.application.rag.dto.SchemaRetrievalContext;
import com.dave.ai.bi.helper.domain.rag.gateway.QueryRewriter;
import com.dave.ai.bi.helper.domain.rag.gateway.SchemaMetadataProvider;
import com.dave.ai.bi.helper.domain.rag.model.QueryIntent;
import com.dave.ai.bi.helper.domain.rag.model.SchemaResolution;
import com.dave.ai.bi.helper.domain.rag.service.RuleBasedChunkReranker;
import com.dave.ai.bi.helper.domain.rag.service.SchemaContextAssembler;
import com.dave.ai.bi.helper.domain.rag.service.SchemaGraphResolver;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Multi-stage schema routing pipeline for NL2SQL.
 */
@Service
public class SchemaRetrievalPipeline {

    private static final int TABLE_TOP_K = 8;
    private static final int FIELD_TOP_K = 12;
    private static final int RELATION_TOP_K = 10;
    private static final int BUSINESS_TOP_K = 6;
    private static final int FINAL_TOP_K = 8;

    private final VectorStore vectorStore;
    private final QueryRewriter queryRewriter;
    private final RuleBasedChunkReranker reranker;
    private final SchemaGraphResolver graphResolver;
    private final SchemaContextAssembler contextAssembler;
    private final SchemaMetadataProvider schemaRegistry;

    public SchemaRetrievalPipeline(VectorStore vectorStore,
                                   QueryRewriter queryRewriter,
                                   SchemaMetadataProvider schemaRegistry) {
        this.vectorStore = vectorStore;
        this.queryRewriter = queryRewriter;
        this.schemaRegistry = schemaRegistry;
        this.reranker = new RuleBasedChunkReranker();
        this.graphResolver = new SchemaGraphResolver(schemaRegistry);
        this.contextAssembler = new SchemaContextAssembler(schemaRegistry);
    }

    public SchemaRetrievalContext retrieve(String userInput) {
        QueryIntent queryIntent = queryRewriter.rewrite(userInput);

        List<Document> tableDocs = similaritySearch(buildTableQuery(queryIntent), TABLE_TOP_K);
        List<Document> fieldDocs = similaritySearch(buildFieldQuery(queryIntent), FIELD_TOP_K);
        List<Document> relationDocs = similaritySearch(buildRelationQuery(queryIntent), RELATION_TOP_K);
        List<Document> businessDocs = similaritySearch(buildBusinessQuery(queryIntent), BUSINESS_TOP_K);

        SchemaResolution resolution = graphResolver.resolve(queryIntent, tableDocs, fieldDocs, relationDocs);
        List<Document> merged = mergeDocuments(tableDocs, fieldDocs, relationDocs, businessDocs);
        List<Document> scoped = filterDocumentsForResolution(merged, resolution);
        List<Document> ranked = reranker.rerank(
                queryIntent,
                scoped,
                resolution.resolvedTables(),
                resolution.resolvedRelations(),
                resolution.primaryFactTable(),
                FINAL_TOP_K
        );
        String assembledContext = contextAssembler.assemble(queryIntent, resolution, ranked);
        return new SchemaRetrievalContext(
                queryIntent,
                ranked,
                assembledContext,
                resolution.resolvedTables(),
                resolution.resolvedRelations(),
                resolution.primaryFactTable()
        );
    }

    private List<Document> similaritySearch(String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThresholdAll()
                .build());
        return documents == null ? List.of() : documents;
    }

    private String buildTableQuery(QueryIntent queryIntent) {
        return joinDistinct(
                queryIntent.rewrittenQuery(),
                String.join(" ", queryIntent.topics()),
                String.join(" ", queryIntent.metrics()),
                String.join(" ", queryIntent.dimensions()),
                String.join(" ", schemaRegistry.matchTableNames(queryIntent.rewrittenQuery())),
                "table overview fact dimension schema"
        );
    }

    private String buildFieldQuery(QueryIntent queryIntent) {
        return joinDistinct(
                queryIntent.rewrittenQuery(),
                String.join(" ", queryIntent.metrics()),
                String.join(" ", queryIntent.dimensions()),
                String.join(" ", queryIntent.filters()),
                queryIntent.needTime() ? "time date field" : "",
                "field metric dimension primary key foreign key"
        );
    }

    private String buildRelationQuery(QueryIntent queryIntent) {
        return joinDistinct(
                queryIntent.rewrittenQuery(),
                String.join(" ", queryIntent.topics()),
                String.join(" ", schemaRegistry.matchTableNames(queryIntent.rewrittenQuery())),
                "join relation foreign key path sql example"
        );
    }

    private String buildBusinessQuery(QueryIntent queryIntent) {
        return joinDistinct(
                queryIntent.rewrittenQuery(),
                String.join(" ", queryIntent.topics()),
                String.join(" ", queryIntent.metrics()),
                String.join(" ", queryIntent.dimensions()),
                "business rule metric definition"
        );
    }

    @SafeVarargs
    private final List<Document> mergeDocuments(List<Document>... groups) {
        Map<String, Document> deduplicated = new LinkedHashMap<>();
        for (List<Document> group : groups) {
            for (Document document : group) {
                String key = document.getId() + "::" + document.getText();
                deduplicated.putIfAbsent(key, document);
            }
        }
        return new ArrayList<>(deduplicated.values());
    }

    private List<Document> filterDocumentsForResolution(List<Document> merged, SchemaResolution resolution) {
        if (merged.isEmpty() || resolution.resolvedTables().isEmpty()) {
            return merged;
        }
        Set<String> scopeTables = new LinkedHashSet<>(resolution.resolvedTables());
        Set<String> scopeRelations = new LinkedHashSet<>(resolution.resolvedRelations());
        List<Document> filtered = merged.stream()
                .filter(document -> withinScope(document, scopeTables, scopeRelations))
                .toList();
        return filtered.isEmpty() ? merged : filtered;
    }

    private boolean withinScope(Document document, Set<String> scopeTables, Set<String> scopeRelations) {
        Map<String, Object> metadata = document.getMetadata();
        String tableName = text(metadata.get("tableName"));
        String chunkType = text(metadata.get("chunkType"));
        if (!tableName.isBlank() && scopeTables.contains(tableName)) {
            return true;
        }
        if ("table_relation".equals(chunkType) || "sql_relation_example".equals(chunkType)) {
            String sourceTable = text(metadata.get("sourceTable"));
            String sourceField = text(metadata.get("sourceField"));
            String targetTable = text(metadata.get("targetTable"));
            String targetField = text(metadata.get("targetField"));
            if (scopeTables.contains(sourceTable) && scopeTables.contains(targetTable)) {
                return true;
            }
            String relationExpression = sourceTable + "." + sourceField + " -> " + targetTable + "." + targetField;
            return scopeRelations.contains(relationExpression);
        }
        return false;
    }

    private String joinDistinct(String... parts) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String normalized = part.trim();
            if (!normalized.isBlank()) {
                tokens.add(normalized);
            }
        }
        return String.join(" ", tokens);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
