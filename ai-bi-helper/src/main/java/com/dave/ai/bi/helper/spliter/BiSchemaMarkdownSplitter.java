package com.dave.ai.bi.helper.spliter;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class BiSchemaMarkdownSplitter extends TextSplitter {

    private static final Pattern TABLE_SECTION_PATTERN =
            Pattern.compile("(?m)^###\\s+TABLE:\\s+(.+?)\\s*$");

    private static final Pattern SUB_SECTION_PATTERN =
            Pattern.compile("(?m)^####\\s+(.+?)\\s*$");

    private static final Pattern TOP_LEVEL_SECTION_PATTERN =
            Pattern.compile("(?m)^##\\s+(.+?)\\s*$");

    private static final String SECTION_CN_NAME = "中文名称";
    private static final String SECTION_TABLE_TYPE = "表类型";
    private static final String SECTION_TABLE_USAGE = "表用途";
    private static final String SECTION_PRIMARY_KEY = "主键";
    private static final String SECTION_UNIQUE_KEY = "唯一键";
    private static final String SECTION_FIELD_DEFINITION = "字段定义";
    private static final String CHUNK_STRATEGY = "bi_schema_markdown_v2";
    private static final int FIELD_BATCH_SIZE = 8;

    private static final Map<String, Object> DEFAULT_METADATA = createDefaultMetadata();
    private static final Map<String, String> SECTION_KEYS = Map.ofEntries(
            Map.entry(SECTION_CN_NAME, "table_cn_name"),
            Map.entry(SECTION_TABLE_TYPE, "table_type"),
            Map.entry(SECTION_TABLE_USAGE, "table_usage"),
            Map.entry(SECTION_PRIMARY_KEY, "primary_key"),
            Map.entry(SECTION_UNIQUE_KEY, "unique_key"),
            Map.entry(SECTION_FIELD_DEFINITION, "field_definition"),
            Map.entry("关联关系", "relations"),
            Map.entry("业务口径", "business_rules"),
            Map.entry("常用查询场景", "query_scenarios")
    );

    public List<Document> split(String markdown) {
        return split(markdown, Collections.emptyMap());
    }

    public List<Document> split(String markdown, Map<String, Object> rootMetadata) {
        return split(createSourceDocument(markdown, rootMetadata));
    }

    @Override
    public List<Document> apply(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        List<Document> result = new ArrayList<>();
        for (Document document : documents) {
            result.addAll(split(document));
        }
        return result;
    }

    @Override
    public List<Document> split(Document document) {
        if (document == null || !document.isText()) {
            return List.of();
        }

        String markdown = normalize(document.getText());
        if (markdown.isBlank()) {
            return List.of();
        }

        Map<String, Object> rootMetadata = prepareRootMetadata(document);
        List<TableBlock> tableBlocks = parseTableBlocks(markdown);

        if (tableBlocks.isEmpty()) {
            return splitNonSchemaDocument(document, markdown, rootMetadata);
        }

        List<Document> chunks = new ArrayList<>();
        AtomicInteger chunkIndex = new AtomicInteger();

        int firstTableStart = tableBlocks.get(0).start();
        if (firstTableStart > 0) {
            String header = markdown.substring(0, firstTableStart).trim();
            if (!header.isBlank()) {
                chunks.addAll(splitTopLevelSections(document, header, rootMetadata, "document_header", chunkIndex));
            }
        }

        for (int tableIndex = 0; tableIndex < tableBlocks.size(); tableIndex++) {
            chunks.addAll(splitTableBlock(document, tableBlocks.get(tableIndex), rootMetadata, tableIndex, chunkIndex));
        }

        int lastEnd = tableBlocks.get(tableBlocks.size() - 1).end();
        if (lastEnd < markdown.length()) {
            String tail = markdown.substring(lastEnd).trim();
            if (!tail.isBlank()) {
                chunks.addAll(splitTopLevelSections(document, tail, rootMetadata, "tail_section", chunkIndex));
            }
        }

        return chunks.isEmpty() ? splitNonSchemaDocument(document, markdown, rootMetadata) : chunks;
    }

    @Override
    protected List<String> splitText(String text) {
        return split(text).stream()
                .map(Document::getText)
                .toList();
    }

    private List<Document> splitNonSchemaDocument(Document source, String markdown, Map<String, Object> rootMetadata) {
        AtomicInteger chunkIndex = new AtomicInteger();
        List<Document> chunks = splitTopLevelSections(source, markdown, rootMetadata, "document_section", chunkIndex);
        if (!chunks.isEmpty()) {
            return chunks;
        }
        return List.of(createChunk(
                source,
                markdown,
                chunkMetadata(rootMetadata, chunkIndex.getAndIncrement(), "document_full")
        ));
    }

    private List<Document> splitTopLevelSections(Document source, String content, Map<String, Object> rootMetadata,
                                                 String chunkType, AtomicInteger chunkIndex) {
        List<Document> docs = new ArrayList<>();
        List<SectionBlock> blocks = parseSections(content, TOP_LEVEL_SECTION_PATTERN);

        if (blocks.isEmpty()) {
            docs.add(createChunk(
                    source,
                    content,
                    chunkMetadata(rootMetadata, chunkIndex.getAndIncrement(), chunkType)
            ));
            return docs;
        }

        for (SectionBlock block : blocks) {
            if (sectionBody(block.content()).isBlank()) {
                continue;
            }
            Map<String, Object> metadata = chunkMetadata(rootMetadata, chunkIndex.getAndIncrement(), chunkType);
            metadata.put("section", block.title());
            metadata.put("sectionKey", normalizeSectionKey(block.title()));
            docs.add(createChunk(source, block.content(), metadata));
        }
        return docs;
    }

    private List<Document> splitTableBlock(Document source, TableBlock tableBlock, Map<String, Object> rootMetadata,
                                           int tableIndex, AtomicInteger chunkIndex) {
        List<Document> docs = new ArrayList<>();
        List<SubSectionBlock> subSections = parseSubSections(tableBlock.content());
        Map<String, String> sectionBodyMap = new LinkedHashMap<>();

        for (SubSectionBlock subSection : subSections) {
            sectionBodyMap.put(subSection.title(), extractSectionBody(subSection.content()));
        }

        String tableName = tableBlock.tableName();
        String tableCnName = firstNonBlankLine(sectionBodyMap.get(SECTION_CN_NAME));
        String tableType = firstNonBlankLine(sectionBodyMap.get(SECTION_TABLE_TYPE));
        String tableUsage = firstNonBlankLine(sectionBodyMap.get(SECTION_TABLE_USAGE));
        String primaryKey = firstNonBlankLine(sectionBodyMap.get(SECTION_PRIMARY_KEY));
        String uniqueKey = firstNonBlankLine(sectionBodyMap.get(SECTION_UNIQUE_KEY));

        Map<String, Object> commonMetadata = new LinkedHashMap<>();
        commonMetadata.put("tableName", tableName);
        commonMetadata.put("tableIndex", tableIndex);
        putIfHasText(commonMetadata, "tableCnName", tableCnName);
        putIfHasText(commonMetadata, "tableType", tableType);
        putIfHasText(commonMetadata, "tableUsage", tableUsage);
        putIfHasText(commonMetadata, "primaryKey", primaryKey);
        putIfHasText(commonMetadata, "uniqueKey", uniqueKey);

        docs.add(createChunk(
                source,
                buildTableOverview(tableName, tableCnName, tableType, tableUsage, primaryKey, uniqueKey),
                chunkMetadata(rootMetadata, chunkIndex.getAndIncrement(), "table_overview", commonMetadata)
        ));

        for (SubSectionBlock subSection : subSections) {
            if (SECTION_FIELD_DEFINITION.equals(subSection.title())) {
                docs.addAll(splitFieldDefinitionSection(
                        source,
                        tableName,
                        tableCnName,
                        tableType,
                        subSection,
                        rootMetadata,
                        commonMetadata,
                        chunkIndex
                ));
                continue;
            }

            Map<String, Object> metadata = chunkMetadata(
                    rootMetadata,
                    chunkIndex.getAndIncrement(),
                    "table_subsection",
                    commonMetadata
            );
            metadata.put("section", subSection.title());
            metadata.put("sectionKey", normalizeSectionKey(subSection.title()));
            docs.add(createChunk(
                    source,
                    contextualizeTableSection(tableName, tableCnName, tableType, subSection.content()),
                    metadata
            ));
        }

        return docs;
    }

    private List<Document> splitFieldDefinitionSection(Document source, String tableName, String tableCnName, String tableType,
                                                       SubSectionBlock subSection, Map<String, Object> rootMetadata,
                                                       Map<String, Object> commonMetadata, AtomicInteger chunkIndex) {
        String body = extractSectionBody(subSection.content());
        List<String> lines = Arrays.stream(body.split("\\R", -1))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        int headerIndex = findFirstMarkdownTableLine(lines);
        if (headerIndex < 0 || headerIndex + 2 > lines.size()) {
            return List.of(createPlainSubSectionChunk(
                    source,
                    tableName,
                    tableCnName,
                    tableType,
                    subSection,
                    rootMetadata,
                    commonMetadata,
                    chunkIndex
            ));
        }

        int lastTableLine = findLastContiguousMarkdownTableLine(lines, headerIndex);
        if (headerIndex + 1 >= lastTableLine) {
            return List.of(createPlainSubSectionChunk(
                    source,
                    tableName,
                    tableCnName,
                    tableType,
                    subSection,
                    rootMetadata,
                    commonMetadata,
                    chunkIndex
            ));
        }

        List<String> prefixLines = new ArrayList<>(lines.subList(0, headerIndex));
        String headerRow = lines.get(headerIndex);
        String separatorRow = lines.get(headerIndex + 1);
        List<String> fieldRows = new ArrayList<>(lines.subList(headerIndex + 2, lastTableLine + 1));
        List<String> suffixLines = new ArrayList<>(lines.subList(lastTableLine + 1, lines.size()));

        int batchCount = (int) Math.ceil((double) fieldRows.size() / FIELD_BATCH_SIZE);
        List<Document> docs = new ArrayList<>();

        for (int batchIndex = 0; batchIndex < batchCount; batchIndex++) {
            int fromIndex = batchIndex * FIELD_BATCH_SIZE;
            int toIndex = Math.min(fromIndex + FIELD_BATCH_SIZE, fieldRows.size());
            List<String> batchRows = fieldRows.subList(fromIndex, toIndex);

            String chunkText = buildFieldDefinitionChunk(
                    tableName,
                    tableCnName,
                    tableType,
                    prefixLines,
                    headerRow,
                    separatorRow,
                    batchRows,
                    batchIndex + 1,
                    batchCount,
                    batchIndex == batchCount - 1 ? suffixLines : List.of()
            );

            Map<String, Object> metadata = chunkMetadata(
                    rootMetadata,
                    chunkIndex.getAndIncrement(),
                    "table_field_batch",
                    commonMetadata
            );
            metadata.put("section", subSection.title());
            metadata.put("sectionKey", normalizeSectionKey(subSection.title()));
            metadata.put("fieldBatchIndex", batchIndex);
            metadata.put("fieldBatchCount", batchCount);
            metadata.put("fieldStartIndex", fromIndex);
            metadata.put("fieldEndIndex", toIndex - 1);
            metadata.put("fieldNames", extractFieldNames(batchRows));

            docs.add(createChunk(source, chunkText, metadata));
        }

        return docs;
    }

    private Document createPlainSubSectionChunk(Document source, String tableName, String tableCnName, String tableType,
                                                SubSectionBlock subSection, Map<String, Object> rootMetadata,
                                                Map<String, Object> commonMetadata, AtomicInteger chunkIndex) {
        Map<String, Object> metadata = chunkMetadata(
                rootMetadata,
                chunkIndex.getAndIncrement(),
                "table_subsection",
                commonMetadata
        );
        metadata.put("section", subSection.title());
        metadata.put("sectionKey", normalizeSectionKey(subSection.title()));
        return createChunk(
                source,
                contextualizeTableSection(tableName, tableCnName, tableType, subSection.content()),
                metadata
        );
    }

    private List<TableBlock> parseTableBlocks(String markdown) {
        List<TableBlock> result = new ArrayList<>();
        Matcher matcher = TABLE_SECTION_PATTERN.matcher(markdown);

        List<Integer> starts = new ArrayList<>();
        List<String> tableNames = new ArrayList<>();

        while (matcher.find()) {
            starts.add(matcher.start());
            tableNames.add(matcher.group(1).trim());
        }

        for (int index = 0; index < starts.size(); index++) {
            int start = starts.get(index);
            int end = index + 1 < starts.size() ? starts.get(index + 1) : markdown.length();
            String content = markdown.substring(start, end).trim();
            result.add(new TableBlock(tableNames.get(index), content, start, end));
        }
        return result;
    }

    private List<SubSectionBlock> parseSubSections(String tableContent) {
        List<SectionBlock> sections = parseSections(tableContent, SUB_SECTION_PATTERN);
        return sections.stream()
                .map(section -> new SubSectionBlock(section.title(), section.content()))
                .toList();
    }

    private List<SectionBlock> parseSections(String content, Pattern pattern) {
        List<SectionBlock> result = new ArrayList<>();
        Matcher matcher = pattern.matcher(content);

        List<Integer> starts = new ArrayList<>();
        List<String> titles = new ArrayList<>();

        while (matcher.find()) {
            starts.add(matcher.start());
            titles.add(matcher.group(1).trim());
        }

        for (int index = 0; index < starts.size(); index++) {
            int start = starts.get(index);
            int end = index + 1 < starts.size() ? starts.get(index + 1) : content.length();
            result.add(new SectionBlock(titles.get(index), content.substring(start, end).trim()));
        }
        return result;
    }

    private String buildTableOverview(String tableName, String tableCnName, String tableType, String tableUsage,
                                      String primaryKey, String uniqueKey) {
        StringBuilder builder = new StringBuilder("### TABLE: ").append(tableName).append('\n');
        appendSection(builder, SECTION_CN_NAME, tableCnName);
        appendSection(builder, SECTION_TABLE_TYPE, tableType);
        appendSection(builder, SECTION_TABLE_USAGE, tableUsage);
        appendSection(builder, SECTION_PRIMARY_KEY, primaryKey);
        appendSection(builder, SECTION_UNIQUE_KEY, uniqueKey);
        return builder.toString().trim();
    }

    private String contextualizeTableSection(String tableName, String tableCnName, String tableType, String sectionContent) {
        StringBuilder builder = new StringBuilder("### TABLE: ").append(tableName).append('\n');
        appendSection(builder, SECTION_CN_NAME, tableCnName);
        appendSection(builder, SECTION_TABLE_TYPE, tableType);
        builder.append(sectionContent.trim());
        return builder.toString().trim();
    }

    private String buildFieldDefinitionChunk(String tableName, String tableCnName, String tableType, List<String> prefixLines,
                                             String headerRow, String separatorRow, List<String> fieldRows,
                                             int batchNumber, int batchCount, List<String> suffixLines) {
        List<String> lines = new ArrayList<>();
        lines.add("### TABLE: " + tableName);

        if (hasText(tableCnName)) {
            lines.add("#### " + SECTION_CN_NAME);
            lines.add(tableCnName);
        }
        if (hasText(tableType)) {
            lines.add("#### " + SECTION_TABLE_TYPE);
            lines.add(tableType);
        }

        lines.add(batchCount > 1
                ? "#### " + SECTION_FIELD_DEFINITION + " (" + batchNumber + "/" + batchCount + ")"
                : "#### " + SECTION_FIELD_DEFINITION);
        lines.addAll(prefixLines);
        lines.add(headerRow);
        lines.add(separatorRow);
        lines.addAll(fieldRows);
        lines.addAll(suffixLines);

        return lines.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"))
                .trim();
    }

    private List<String> extractFieldNames(List<String> rows) {
        List<String> names = new ArrayList<>();
        for (String row : rows) {
            List<String> cells = Arrays.stream(row.split("\\|"))
                    .map(String::trim)
                    .filter(cell -> !cell.isBlank())
                    .toList();
            if (!cells.isEmpty()) {
                names.add(cells.get(0));
            }
        }
        return names;
    }

    private int findFirstMarkdownTableLine(List<String> lines) {
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).startsWith("|")) {
                return index;
            }
        }
        return -1;
    }

    private int findLastContiguousMarkdownTableLine(List<String> lines, int startIndex) {
        int index = startIndex;
        while (index + 1 < lines.size() && lines.get(index + 1).startsWith("|")) {
            index++;
        }
        return index;
    }

    private String extractSectionBody(String sectionContent) {
        String[] parts = sectionContent.split("\\R", 2);
        return parts.length > 1 ? parts[1].trim() : "";
    }

    private String sectionBody(String sectionContent) {
        return extractSectionBody(sectionContent);
    }

    private String firstNonBlankLine(String text) {
        if (!hasText(text)) {
            return null;
        }
        return Arrays.stream(text.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> prepareRootMetadata(Document document) {
        Map<String, Object> metadata = new LinkedHashMap<>(DEFAULT_METADATA);
        metadata.putAll(sanitizeMetadata(document.getMetadata()));
        metadata.put("sourceDocumentId", document.getId());
        metadata.put("splitter", getClass().getSimpleName());
        metadata.put("chunkStrategy", CHUNK_STRATEGY);
        return metadata;
    }

    private Map<String, Object> chunkMetadata(Map<String, Object> rootMetadata, int chunkIndex, String chunkType) {
        return chunkMetadata(rootMetadata, chunkIndex, chunkType, Collections.emptyMap());
    }

    private Map<String, Object> chunkMetadata(Map<String, Object> rootMetadata, int chunkIndex, String chunkType,
                                              Map<String, Object> extraMetadata) {
        Map<String, Object> metadata = new LinkedHashMap<>(rootMetadata);
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("chunkType", chunkType);
        metadata.putAll(sanitizeMetadata(extraMetadata));
        return metadata;
    }

    private Document createChunk(Document source, String text, Map<String, Object> metadata) {
        Document chunk = new Document(text.trim(), metadata);
        if (isCopyContentFormatter()) {
            chunk.setContentFormatter(source.getContentFormatter());
        }
        return chunk;
    }

    private Document createSourceDocument(String markdown, Map<String, Object> rootMetadata) {
        return new Document(normalize(markdown), sanitizeMetadata(rootMetadata));
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").trim();
    }

    private static Map<String, Object> createDefaultMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("docType", "schema");
        metadata.put("project", "ai-transfer");
        metadata.put("source", "企业智能BI数据库表结构说明文档");
        return Collections.unmodifiableMap(metadata);
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Collections.emptyMap();
        }
        return metadata.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }

    private String normalizeSectionKey(String section) {
        return SECTION_KEYS.getOrDefault(section, section);
    }

    private void appendSection(StringBuilder builder, String title, String body) {
        if (!hasText(body)) {
            return;
        }
        builder.append("#### ").append(title).append('\n')
                .append(body.trim()).append('\n');
    }

    private void putIfHasText(Map<String, Object> metadata, String key, String value) {
        if (hasText(value)) {
            metadata.put(key, value);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record TableBlock(String tableName, String content, int start, int end) {
    }

    private record SectionBlock(String title, String content) {
    }

    private record SubSectionBlock(String title, String content) {
    }
}
