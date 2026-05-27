package com.dave.ai.bi.helper.infrastructure.rag.llm;

import com.dave.ai.bi.helper.domain.rag.gateway.QueryRewriter;
import com.dave.ai.bi.helper.domain.rag.gateway.SchemaMetadataProvider;
import com.dave.ai.bi.helper.domain.rag.model.QueryIntent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Converts a user question into a retrieval-oriented schema intent.
 */
@Slf4j
@Component
public class SchemaAwareQueryRewriter implements QueryRewriter {

    private static final Map<String, String> TOPIC_ALIAS = createTopicAlias();
    private static final Map<String, String> METRIC_ALIAS = createMetricAlias();
    private static final Map<String, String> DIMENSION_ALIAS = createDimensionAlias();

    private final ChatClient.Builder chatClientBuilder;
    private final SchemaMetadataProvider schemaRegistry;
    private final ObjectMapper objectMapper;

    public SchemaAwareQueryRewriter(ChatClient.Builder chatClientBuilder, SchemaMetadataProvider schemaRegistry) {
        this.chatClientBuilder = chatClientBuilder;
        this.schemaRegistry = schemaRegistry;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public QueryIntent rewrite(String userInput) {
        try {
            String content = chatClientBuilder.build().prompt()
                    .system(buildSystemPrompt())
                    .user(buildUserPrompt(userInput))
                    .call()
                    .content();
            return parse(content, userInput);
        } catch (Exception ex) {
            log.warn("Schema-aware rewrite failed, fallback to heuristic rewrite. userInput={}", userInput, ex);
            return fallback(userInput);
        }
    }

    private String buildSystemPrompt() {
        return """
                You are a BI schema retrieval intent parser.
                Your job is to convert a user question into structured retrieval intent for schema routing.

                Rules:
                1. Return JSON only.
                2. Do not generate SQL.
                3. Focus on business topics, metric hints, dimension hints, filter hints, and query mode.
                4. Keep rewrittenQuery short and retrieval-friendly.
                5. Use lowercase topic and field-style tokens when possible.
                6. queryMode must be one of: detail, aggregate, trend.
                7. If the request implies time analysis, set needTime=true.
                8. If the request needs dimension attributes or multi-table joins, set needRelation=true.
                9. If the request asks for totals, counts, averages, rankings, or trends, set needAggregation=true when appropriate.

                Available topic hints:
                %s

                Known metric field hints:
                %s

                Known dimension field hints:
                %s
                """.formatted(
                String.join(", ", schemaRegistry.availableTopics()),
                String.join(", ", schemaRegistry.metricFieldNames()),
                String.join(", ", schemaRegistry.dimensionFieldNames())
        );
    }

    private String buildUserPrompt(String userInput) {
        return """
                Convert the following question into JSON:
                {
                  "rewrittenQuery": "short retrieval phrase",
                  "topics": ["..."],
                  "metrics": ["..."],
                  "dimensions": ["..."],
                  "filters": ["..."],
                  "needTime": true,
                  "needRelation": true,
                  "needAggregation": true,
                  "queryMode": "aggregate"
                }

                User question:
                %s
                """.formatted(textOrDefault(userInput, ""));
    }

    private QueryIntent parse(String content, String userInput) {
        try {
            JsonNode root = objectMapper.readTree(content);
            return new QueryIntent(
                    textOrDefault(root.path("rewrittenQuery").asText(null), fallbackRewrittenQuery(userInput)),
                    normalizeTopics(toDistinctList(root.path("topics"))),
                    normalizeMetrics(toDistinctList(root.path("metrics"))),
                    normalizeDimensions(toDistinctList(root.path("dimensions"))),
                    toDistinctList(root.path("filters")),
                    root.path("needTime").asBoolean(false),
                    root.path("needRelation").asBoolean(false),
                    root.path("needAggregation").asBoolean(false),
                    normalizeQueryMode(root.path("queryMode").asText("detail"))
            );
        } catch (Exception ex) {
            log.warn("Failed to parse rewrite json, fallback to heuristic rewrite. content={}", content, ex);
            return fallback(userInput);
        }
    }

    private QueryIntent fallback(String userInput) {
        String normalizedInput = normalize(userInput);
        boolean needTime = containsAny(normalizedInput,
                "\u6700\u8fd1", "\u8fc7\u53bb", "\u8fd1", "\u6309\u5e74", "\u6309\u6708", "\u6309\u65e5",
                "year", "month", "date", "quarter", "trend");
        boolean aggregate = containsAny(normalizedInput,
                "\u7edf\u8ba1", "\u6c47\u603b", "\u603b\u6570", "\u6392\u540d", "\u5360\u6bd4",
                "sum", "count", "avg", "average", "top");
        boolean trend = containsAny(normalizedInput,
                "\u8d8b\u52bf", "\u53d8\u5316", "\u6ce2\u52a8", "trend");

        Set<String> topics = new LinkedHashSet<>();
        Set<String> metrics = new LinkedHashSet<>();
        Set<String> dimensions = new LinkedHashSet<>();
        Set<String> filters = new LinkedHashSet<>();

        if (containsAny(normalizedInput, "\u9500\u552e", "gmv", "sale", "sales")) {
            topics.add("sales");
            metrics.add("sales_amount");
        }
        if (containsAny(normalizedInput, "\u5e93\u5b58", "inventory", "stock")) {
            topics.add("inventory");
            metrics.add("quantity");
        }
        if (containsAny(normalizedInput, "\u5ba2\u6237", "customer")) {
            topics.add("customer");
            dimensions.add("customer_name");
        }
        if (containsAny(normalizedInput, "\u5546\u54c1", "\u54c1\u724c", "\u54c1\u7c7b", "product", "brand", "category")) {
            topics.add("product");
        }
        if (containsAny(normalizedInput, "\u95e8\u5e97", "\u533a\u57df", "\u57ce\u5e02", "\u7701", "store", "city", "province")) {
            topics.add("store");
        }
        if (needTime) {
            topics.add("time");
            if (containsAny(normalizedInput, "\u5e74", "year")) {
                dimensions.add("year");
            }
            if (containsAny(normalizedInput, "\u6708", "month")) {
                dimensions.add("month");
            }
            if (containsAny(normalizedInput, "\u65e5", "day", "date")) {
                dimensions.add("day");
            }
        }
        if (containsAny(normalizedInput, "\u9500\u91cf", "\u6570\u91cf", "quantity", "count")) {
            metrics.add("quantity");
        }
        if (containsAny(normalizedInput, "\u6298\u6263", "discount")) {
            metrics.add("discount");
        }

        dimensions.addAll(normalizeDimensions(extractAliasMatches(normalizedInput, DIMENSION_ALIAS)));
        metrics.addAll(normalizeMetrics(extractAliasMatches(normalizedInput, METRIC_ALIAS)));
        filters.addAll(extractFilters(normalizedInput));

        boolean needRelation = !dimensions.isEmpty()
                || topics.contains("customer")
                || topics.contains("product")
                || topics.contains("store")
                || (needTime && !topics.isEmpty());
        String queryMode = trend ? "trend" : (aggregate ? "aggregate" : "detail");

        return new QueryIntent(
                buildFallbackRewrittenQuery(topics, metrics, dimensions, filters, queryMode),
                new ArrayList<>(topics),
                new ArrayList<>(metrics),
                new ArrayList<>(dimensions),
                new ArrayList<>(filters),
                needTime,
                needRelation,
                aggregate || trend,
                queryMode
        );
    }

    private List<String> normalizeTopics(List<String> topics) {
        if (topics == null || topics.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String topic : topics) {
            String value = normalize(topic).replace(' ', '_');
            if (!value.isBlank()) {
                normalized.add(TOPIC_ALIAS.getOrDefault(value, value));
            }
        }
        return new ArrayList<>(normalized);
    }

    private List<String> normalizeMetrics(List<String> metrics) {
        return normalizeAliases(metrics, METRIC_ALIAS);
    }

    private List<String> normalizeDimensions(List<String> dimensions) {
        return normalizeAliases(dimensions, DIMENSION_ALIAS);
    }

    private List<String> normalizeAliases(List<String> values, Map<String, String> aliasMap) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return List.of();
        }
        for (String value : values) {
            String token = normalize(value).replace(' ', '_');
            if (token.isBlank()) {
                continue;
            }
            normalized.add(aliasMap.getOrDefault(token, token));
        }
        return new ArrayList<>(normalized);
    }

    private List<String> extractAliasMatches(String normalizedInput, Map<String, String> aliasMap) {
        LinkedHashSet<String> results = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : aliasMap.entrySet()) {
            if (containsAny(normalizedInput, entry.getKey())) {
                results.add(entry.getValue());
            }
        }
        return new ArrayList<>(results);
    }

    private List<String> extractFilters(String normalizedInput) {
        LinkedHashSet<String> filters = new LinkedHashSet<>();
        if (containsAny(normalizedInput, "\u6700\u8fd1\u4e09\u4e2a\u6708", "last_3_month", "last 3 month")) {
            filters.add("last_3_months");
        }
        if (containsAny(normalizedInput, "\u6700\u8fd1\u4e00\u5e74", "last_year", "last year")) {
            filters.add("last_1_year");
        }
        if (containsAny(normalizedInput, "\u6309\u7701", "\u7701\u4efd", "province")) {
            filters.add("province");
        }
        if (containsAny(normalizedInput, "\u6309\u57ce\u5e02", "\u57ce\u5e02", "city")) {
            filters.add("city");
        }
        if (containsAny(normalizedInput, "\u6309\u95e8\u5e97", "store")) {
            filters.add("store");
        }
        return new ArrayList<>(filters);
    }

    private String buildFallbackRewrittenQuery(Set<String> topics,
                                               Set<String> metrics,
                                               Set<String> dimensions,
                                               Set<String> filters,
                                               String queryMode) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        parts.addAll(topics);
        parts.addAll(metrics);
        parts.addAll(dimensions);
        parts.addAll(filters);
        parts.add(queryMode);
        return String.join(" ", parts);
    }

    private String fallbackRewrittenQuery(String userInput) {
        return textOrDefault(userInput, "");
    }

    private List<String> toDistinctList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        Set<String> values = new LinkedHashSet<>();
        node.forEach(item -> {
            String value = item.asText("").trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        });
        return new ArrayList<>(values);
    }

    private String normalizeQueryMode(String mode) {
        return switch (normalize(mode)) {
            case "aggregate", "trend" -> normalize(mode);
            default -> "detail";
        };
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(normalize(candidate))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return textOrDefault(value, "").trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Map<String, String> createMetricAlias() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("sales_amount", "sales_amount");
        aliases.put("salesamount", "sales_amount");
        aliases.put("gmv", "sales_amount");
        aliases.put("sale", "sales_amount");
        aliases.put("sales", "sales_amount");
        aliases.put("\u9500\u552e\u989d", "sales_amount");
        aliases.put("\u9500\u552e\u989d\u5ea6", "sales_amount");
        aliases.put("\u9500\u552e", "sales_amount");
        aliases.put("quantity", "quantity");
        aliases.put("qty", "quantity");
        aliases.put("\u9500\u91cf", "quantity");
        aliases.put("\u6570\u91cf", "quantity");
        aliases.put("\u5e93\u5b58\u91cf", "quantity");
        aliases.put("discount", "discount");
        aliases.put("\u6298\u6263", "discount");
        aliases.put("count", "count");
        aliases.put("\u603b\u6570", "count");
        aliases.put("\u7edf\u8ba1", "count");
        return Map.copyOf(aliases);
    }

    private static Map<String, String> createTopicAlias() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("sales", "sales");
        aliases.put("sale", "sales");
        aliases.put("\u9500\u552e", "sales");
        aliases.put("inventory", "inventory");
        aliases.put("stock", "inventory");
        aliases.put("\u5e93\u5b58", "inventory");
        aliases.put("customer", "customer");
        aliases.put("\u5ba2\u6237", "customer");
        aliases.put("product", "product");
        aliases.put("brand", "product");
        aliases.put("category", "product");
        aliases.put("\u5546\u54c1", "product");
        aliases.put("\u54c1\u724c", "product");
        aliases.put("\u54c1\u7c7b", "product");
        aliases.put("store", "store");
        aliases.put("region", "store");
        aliases.put("province", "store");
        aliases.put("city", "store");
        aliases.put("\u95e8\u5e97", "store");
        aliases.put("\u533a\u57df", "store");
        aliases.put("\u7701", "store");
        aliases.put("\u57ce\u5e02", "store");
        aliases.put("time", "time");
        aliases.put("date", "time");
        aliases.put("\u65f6\u95f4", "time");
        aliases.put("\u65e5\u671f", "time");
        return Map.copyOf(aliases);
    }

    private static Map<String, String> createDimensionAlias() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("province", "province");
        aliases.put("\u7701", "province");
        aliases.put("\u7701\u4efd", "province");
        aliases.put("city", "city");
        aliases.put("\u57ce\u5e02", "city");
        aliases.put("brand", "brand");
        aliases.put("\u54c1\u724c", "brand");
        aliases.put("category", "category_name");
        aliases.put("category_name", "category_name");
        aliases.put("\u54c1\u7c7b", "category_name");
        aliases.put("\u5206\u7c7b", "category_name");
        aliases.put("product", "product_name");
        aliases.put("product_name", "product_name");
        aliases.put("\u5546\u54c1", "product_name");
        aliases.put("customer", "customer_name");
        aliases.put("customer_name", "customer_name");
        aliases.put("\u5ba2\u6237", "customer_name");
        aliases.put("store", "store_name");
        aliases.put("store_name", "store_name");
        aliases.put("\u95e8\u5e97", "store_name");
        aliases.put("gender", "gender");
        aliases.put("\u6027\u522b", "gender");
        aliases.put("age", "age");
        aliases.put("\u5e74\u9f84", "age");
        aliases.put("year", "year");
        aliases.put("\u5e74", "year");
        aliases.put("quarter", "quarter");
        aliases.put("\u5b63\u5ea6", "quarter");
        aliases.put("month", "month");
        aliases.put("\u6708", "month");
        aliases.put("day", "day");
        aliases.put("\u65e5", "day");
        aliases.put("weekday", "weekday");
        aliases.put("\u661f\u671f", "weekday");
        return Map.copyOf(aliases);
    }
}
