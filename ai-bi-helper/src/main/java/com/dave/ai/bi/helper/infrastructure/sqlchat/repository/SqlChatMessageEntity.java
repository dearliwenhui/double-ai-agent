package com.dave.ai.bi.helper.infrastructure.sqlchat.repository;

import java.time.LocalDateTime;

public record SqlChatMessageEntity(Long id,
                                   String sessionId,
                                   String userId,
                                   String role,
                                   String messageType,
                                   String content,
                                   String generatedSql,
                                   String editedSql,
                                   String evaluateResultJson,
                                   String retrievalContext,
                                   String previewResultJson,
                                   LocalDateTime createdTime) {
}
