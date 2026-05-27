package com.dave.ai.bi.helper.domain.rag.gateway;

import com.dave.ai.bi.helper.domain.rag.model.SchemaRelationMeta;
import com.dave.ai.bi.helper.domain.rag.model.SchemaTableMeta;

import java.util.List;
import java.util.Optional;

public interface SchemaMetadataProvider {

    Optional<SchemaTableMeta> table(String tableName);

    boolean hasTable(String tableName);

    List<SchemaTableMeta> allTables();

    List<SchemaRelationMeta> allRelations();

    List<SchemaRelationMeta> relationsFor(String tableName);

    List<String> availableTopics();

    List<String> metricFieldNames();

    List<String> dimensionFieldNames();

    List<String> timeFieldNames();

    List<String> findTablesByField(String fieldName);

    List<String> findTablesByTopic(String topic);

    String preferredFactTable(List<String> topics);

    List<String> matchTableNames(String input);
}
