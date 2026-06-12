package com.dave.ai.bi.helper.infrastructure.sqlchat.repository;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SqlChatSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public SqlChatSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initializeTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sql_chat_session (
                    session_id VARCHAR(64) PRIMARY KEY,
                    user_id VARCHAR(64) NOT NULL,
                    session_title VARCHAR(255) NOT NULL,
                    status VARCHAR(64) NOT NULL,
                    last_question TEXT NULL,
                    last_generated_sql LONGTEXT NULL,
                    last_edited_sql LONGTEXT NULL,
                    last_evaluate_result_json LONGTEXT NULL,
                    last_preview_status VARCHAR(64) NULL,
                    last_export_path VARCHAR(512) NULL,
                    created_time DATETIME NOT NULL,
                    updated_time DATETIME NOT NULL
                )
                """);
    }

    public SqlChatSessionEntity createSession(String userId, String sessionTitle) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                        INSERT INTO sql_chat_session (
                            session_id, user_id, session_title, status, created_time, updated_time
                        ) VALUES (?, ?, ?, ?, ?, ?)
                        """,
                sessionId, userId, sessionTitle, "NEW", now, now
        );
        return findBySessionIdAndUserId(sessionId, userId).orElseThrow();
    }

    public Optional<SqlChatSessionEntity> findBySessionIdAndUserId(String sessionId, String userId) {
        List<SqlChatSessionEntity> results = jdbcTemplate.query("""
                        SELECT session_id, user_id, session_title, status, last_question, last_generated_sql,
                               last_edited_sql, last_evaluate_result_json, last_preview_status, last_export_path,
                               created_time, updated_time
                        FROM sql_chat_session
                        WHERE session_id = ? AND user_id = ?
                        """,
                sessionRowMapper(),
                sessionId, userId
        );
        return results.stream().findFirst();
    }

    public List<SqlChatSessionEntity> listByUserId(String userId) {
        return jdbcTemplate.query("""
                        SELECT session_id, user_id, session_title, status, last_question, last_generated_sql,
                               last_edited_sql, last_evaluate_result_json, last_preview_status, last_export_path,
                               created_time, updated_time
                        FROM sql_chat_session
                        WHERE user_id = ?
                        ORDER BY updated_time DESC
                        """,
                sessionRowMapper(),
                userId
        );
    }

    public void updateAfterMessage(String sessionId,
                                   String userId,
                                   String sessionTitle,
                                   String status,
                                   String lastQuestion,
                                   String lastGeneratedSql,
                                   String lastEditedSql,
                                   String lastEvaluateResultJson) {
        jdbcTemplate.update("""
                        UPDATE sql_chat_session
                        SET session_title = ?, status = ?, last_question = ?, last_generated_sql = ?,
                            last_edited_sql = ?, last_evaluate_result_json = ?, updated_time = ?
                        WHERE session_id = ? AND user_id = ?
                        """,
                sessionTitle,
                status,
                lastQuestion,
                lastGeneratedSql,
                lastEditedSql,
                lastEvaluateResultJson,
                LocalDateTime.now(),
                sessionId,
                userId
        );
    }

    public void updateAfterPreview(String sessionId, String userId, String lastEditedSql, String previewStatus) {
        jdbcTemplate.update("""
                        UPDATE sql_chat_session
                        SET status = ?, last_edited_sql = ?, last_preview_status = ?, updated_time = ?
                        WHERE session_id = ? AND user_id = ?
                        """,
                "PREVIEW_SUCCESS",
                lastEditedSql,
                previewStatus,
                LocalDateTime.now(),
                sessionId,
                userId
        );
    }

    public void updateAfterExport(String sessionId, String userId, String lastEditedSql, String exportPath) {
        jdbcTemplate.update("""
                        UPDATE sql_chat_session
                        SET status = ?, last_edited_sql = ?, last_export_path = ?, updated_time = ?
                        WHERE session_id = ? AND user_id = ?
                        """,
                "EXPORTED",
                lastEditedSql,
                exportPath,
                LocalDateTime.now(),
                sessionId,
                userId
        );
    }

    private RowMapper<SqlChatSessionEntity> sessionRowMapper() {
        return (rs, rowNum) -> new SqlChatSessionEntity(
                rs.getString("session_id"),
                rs.getString("user_id"),
                rs.getString("session_title"),
                rs.getString("status"),
                rs.getString("last_question"),
                rs.getString("last_generated_sql"),
                rs.getString("last_edited_sql"),
                rs.getString("last_evaluate_result_json"),
                rs.getString("last_preview_status"),
                rs.getString("last_export_path"),
                toLocalDateTime(rs.getTimestamp("created_time")),
                toLocalDateTime(rs.getTimestamp("updated_time"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
