package com.dave.ai.bi.helper.application.rag.dto;

import com.dave.ai.bi.helper.domain.rag.model.QueryIntent;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * Final retrieval output passed into SQL generation and evaluation.
 */
public record SchemaRetrievalContext(QueryIntent queryIntent,
                                     List<Document> rankedDocuments,
                                     String assembledContext,
                                     List<String> resolvedTables,
                                     List<String> resolvedRelations,
                                     String primaryFactTable) {
}
