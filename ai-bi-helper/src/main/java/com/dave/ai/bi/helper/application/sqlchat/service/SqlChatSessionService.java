package com.dave.ai.bi.helper.application.sqlchat.service;

import com.dave.ai.bi.helper.application.sqlchat.dto.SqlChatMessageView;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlChatSessionDetail;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlChatSessionStartResponse;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlChatSessionSummary;
import com.dave.ai.bi.helper.infrastructure.sqlchat.repository.SqlChatMessageEntity;
import com.dave.ai.bi.helper.infrastructure.sqlchat.repository.SqlChatMessageRepository;
import com.dave.ai.bi.helper.infrastructure.sqlchat.repository.SqlChatSessionEntity;
import com.dave.ai.bi.helper.infrastructure.sqlchat.repository.SqlChatSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class SqlChatSessionService {

    private final SqlChatSessionRepository sessionRepository;
    private final SqlChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    public SqlChatSessionService(SqlChatSessionRepository sessionRepository,
                                 SqlChatMessageRepository messageRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.objectMapper = new ObjectMapper();
    }

    public SqlChatSessionStartResponse startSession(String userId) {
        validateUserId(userId);
        SqlChatSessionEntity session = sessionRepository.createSession(userId.trim(), "新建 SQL 会话");
        return new SqlChatSessionStartResponse(
                session.sessionId(),
                session.userId(),
                session.sessionTitle(),
                session.status()
        );
    }

    public List<SqlChatSessionSummary> listSessions(String userId) {
        validateUserId(userId);
        return sessionRepository.listByUserId(userId.trim()).stream()
                .map(session -> new SqlChatSessionSummary(
                        session.sessionId(),
                        session.userId(),
                        session.sessionTitle(),
                        session.status(),
                        session.lastQuestion(),
                        session.updatedTime()
                ))
                .toList();
    }

    public SqlChatSessionDetail getSessionDetail(String sessionId, String userId) {
        SqlChatSessionEntity session = requireSession(sessionId, userId);
        List<SqlChatMessageView> messages = messageRepository.listBySessionId(sessionId, userId).stream()
                .map(this::toMessageView)
                .toList();
        return new SqlChatSessionDetail(
                session.sessionId(),
                session.userId(),
                session.sessionTitle(),
                session.status(),
                session.lastQuestion(),
                session.lastGeneratedSql(),
                session.lastEditedSql(),
                parseJson(session.lastEvaluateResultJson()),
                session.lastPreviewStatus(),
                session.lastExportPath(),
                session.createdTime(),
                session.updatedTime(),
                messages
        );
    }

    public SqlChatSessionEntity requireSession(String sessionId, String userId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
        validateUserId(userId);
        return sessionRepository.findBySessionIdAndUserId(sessionId.trim(), userId.trim())
                .orElseThrow(() -> new IllegalArgumentException("会话不存在或不属于当前 userId"));
    }

    public void saveUserMessage(String sessionId, String userId, String userInput) {
        messageRepository.save(new SqlChatMessageEntity(
                null,
                sessionId,
                userId,
                "user",
                "USER_PROMPT",
                userInput,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.now()
        ));
    }

    public void saveAssistantMessage(String sessionId,
                                     String userId,
                                     String assistantReply,
                                     String generatedSql,
                                     String evaluateResultJson,
                                     String retrievalContext) {
        messageRepository.save(new SqlChatMessageEntity(
                null,
                sessionId,
                userId,
                "assistant",
                "AI_RESULT",
                assistantReply,
                generatedSql,
                generatedSql,
                evaluateResultJson,
                retrievalContext,
                null,
                LocalDateTime.now()
        ));
    }

    public void savePreviewMessage(String sessionId,
                                   String userId,
                                   String sql,
                                   String previewSummaryJson) {
        messageRepository.save(new SqlChatMessageEntity(
                null,
                sessionId,
                userId,
                "system",
                "PREVIEW_SUCCESS",
                "SQL 预览成功",
                null,
                sql,
                null,
                null,
                previewSummaryJson,
                LocalDateTime.now()
        ));
    }

    public void saveExportMessage(String sessionId,
                                  String userId,
                                  String sql,
                                  String exportPath) {
        messageRepository.save(new SqlChatMessageEntity(
                null,
                sessionId,
                userId,
                "system",
                "EXPORT_SUCCESS",
                "CSV 导出成功",
                null,
                sql,
                null,
                null,
                "{\"csvFilePath\":\"" + exportPath.replace("\\", "\\\\") + "\"}",
                LocalDateTime.now()
        ));
    }

    public void updateSessionAfterMessage(String sessionId,
                                          String userId,
                                          String lastQuestion,
                                          String generatedSql,
                                          String evaluateResultJson) {
        String title = buildTitle(lastQuestion);
        sessionRepository.updateAfterMessage(
                sessionId,
                userId,
                title,
                "EVALUATED",
                lastQuestion,
                generatedSql,
                generatedSql,
                evaluateResultJson
        );
    }

    public void updateSessionAfterPreview(String sessionId, String userId, String currentSql) {
        sessionRepository.updateAfterPreview(sessionId, userId, currentSql, "SUCCESS");
    }

    public void updateSessionAfterExport(String sessionId, String userId, String currentSql, String exportPath) {
        sessionRepository.updateAfterExport(sessionId, userId, currentSql, exportPath);
    }

    public boolean hasSuccessfulPreview(String sessionId, String userId) {
        return messageRepository.hasSuccessfulPreview(sessionId, userId);
    }

    public String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private SqlChatMessageView toMessageView(SqlChatMessageEntity entity) {
        return new SqlChatMessageView(
                entity.id(),
                entity.role(),
                entity.messageType(),
                entity.content(),
                entity.generatedSql(),
                entity.editedSql(),
                parseJson(entity.evaluateResultJson()),
                entity.retrievalContext(),
                parseJson(entity.previewResultJson()),
                entity.createdTime()
        );
    }

    private Map<String, Object> parseJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return Map.of("raw", rawJson);
        }
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId 不能为空");
        }
    }

    private String buildTitle(String question) {
        if (question == null || question.isBlank()) {
            return "SQL 会话";
        }
        String normalized = question.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 24 ? normalized : normalized.substring(0, 24);
    }
}
