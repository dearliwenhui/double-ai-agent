package com.dave.ai.bi.helper.spliter;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiSchemaMarkdownSplitterTest {

    private final BiSchemaMarkdownSplitter splitter = new BiSchemaMarkdownSplitter();

    @Test
    void shouldSplitSchemaMarkdownForSpringAiPipeline() throws IOException {
        String markdown = new ClassPathResource("document/schema.md")
                .getContentAsString(StandardCharsets.UTF_8);

        Document source = new Document(markdown, Map.of(
                "project", "custom-project",
                "fileName", "schema.md"
        ));

        List<Document> chunks = splitter.apply(List.of(source));

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() > 10);
        assertTrue(chunks.stream().anyMatch(doc -> "table_overview".equals(doc.getMetadata().get("chunkType"))));
        assertTrue(chunks.stream().anyMatch(doc -> "table_field_batch".equals(doc.getMetadata().get("chunkType"))));
        assertTrue(chunks.stream().allMatch(doc -> source.getId().equals(doc.getMetadata().get("sourceDocumentId"))));
        assertTrue(chunks.stream().allMatch(doc -> "custom-project".equals(doc.getMetadata().get("project"))));
        assertTrue(chunks.stream()
                .filter(doc -> "table_field_batch".equals(doc.getMetadata().get("chunkType")))
                .anyMatch(doc -> doc.getText().contains("### TABLE: bb_product")));
    }

    @Test
    void shouldFallbackToSingleChunkForPlainMarkdown() {
        Document source = new Document("# Title\n\nJust some plain text.", Map.of("source", "manual"));

        List<Document> chunks = splitter.split(source);

        assertEquals(1, chunks.size());
        assertEquals("document_full", chunks.get(0).getMetadata().get("chunkType"));
        assertEquals(source.getId(), chunks.get(0).getMetadata().get("sourceDocumentId"));
    }
}
