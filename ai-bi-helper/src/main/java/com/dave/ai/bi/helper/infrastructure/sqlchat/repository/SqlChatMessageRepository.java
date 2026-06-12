package com.dave.ai.bi.helper.infrastructure.sqlchat.repository;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class SqlChatMessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public SqlChatMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initializeTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sql_chat_message (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    session_id VARCHAR(64) NOT NULL,
                    user_id VARCHAR(64) NOT NULL,
                    role VARCHAR(32) NOT NULL,
                    message_type VARCHAR(64) NOT NULL,
                    content LONGTEXT NULL,
                    generated_sql LONGTEXT NULL,
                    edited_sql LONGTEXT NULL,
                    evaluate_result_json LONGTEXT NULL,
                    retrieval_context LONGTEXT NULL,
                    preview_result_json LONGTEXT NULL,
                    created_time DATETIME NOT NULL
                )
                """);
    }

    public void save(SqlChatMessageEntity entity) {
        jdbcTemplate.update("""
                        INSERT INTO sql_chat_message (
                            session_id, user_id, role, message_type, content, generated_sql,
                            edited_sql, evaluate_result_json, retrieval_context, preview_result_json, created_time
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                entity.sessionId(),
                entity.userId(),
                entity.role(),
                entity.messageType(),
                entity.content(),
                entity.generatedSql(),
                entity.editedSql(),
                entity.evaluateResultJson(),
                entity.retrievalContext(),
                entity.previewResultJson(),
                entity.createdTime()
        );
    }

    public List<SqlChatMessageEntity> listBySessionId(String sessionId, String userId) {
        return jdbcTemplate.query("""
                        SELECT id, session_id, user_id, role, message_type, content, generated_sql,
                               edited_sql, evaluate_result_json, retrieval_context, preview_result_json, created_time
                        FROM sql_chat_message
                        WHERE session_id = ? AND user_id = ?
                        ORDER BY id ASC
                        """,
                rowMapper(),
                sessionId,
                userId
        );
    }

    public boolean hasSuccessfulPreview(String sessionId, String userId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM sql_chat_message
                        WHERE session_id = ? AND user_id = ? AND message_type = 'PREVIEW_SUCCESS'
                        """,
                Integer.class,
                sessionId,
                userId
        );
        return count != null && count > 0;
    }

    private RowMapper<SqlChatMessageEntity> rowMapper() {
        return (rs, rowNum) -> new SqlChatMessageEntity(
                rs.getLong("id"),
                rs.getString("session_id"),
                rs.getString("user_id"),
                rs.getString("role"),
                rs.getString("message_type"),
                rs.getString("content"),
                rs.getString("generated_sql"),
                rs.getString("edited_sql"),
                rs.getString("evaluate_result_json"),
                rs.getString("retrieval_context"),
                rs.getString("preview_result_json"),
                toLocalDateTime(rs.getTimestamp("created_time"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
