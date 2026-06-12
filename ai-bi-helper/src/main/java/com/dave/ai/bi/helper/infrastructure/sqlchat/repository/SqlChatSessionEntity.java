package com.dave.ai.bi.helper.infrastructure.sqlchat.repository;

import java.time.LocalDateTime;

public record SqlChatSessionEntity(String sessionId,
                                   String userId,
                                   String sessionTitle,
                                   String status,
                                   String lastQuestion,
                                   String lastGeneratedSql,
                                   String lastEditedSql,
                                   String lastEvaluateResultJson,
                                   String lastPreviewStatus,
                                   String lastExportPath,
                                   LocalDateTime createdTime,
                                   LocalDateTime updatedTime) {
}
