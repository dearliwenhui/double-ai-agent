package com.dave.ai.bi.helper.application.sqlchat.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record SqlChatSessionDetail(String sessionId,
                                   String userId,
                                   String sessionTitle,
                                   String status,
                                   String lastQuestion,
                                   String lastGeneratedSql,
                                   String lastEditedSql,
                                   Map<String, Object> lastEvaluateResult,
                                   String lastPreviewStatus,
                                   String lastExportPath,
                                   LocalDateTime createdTime,
                                   LocalDateTime updatedTime,
                                   List<SqlChatMessageView> messages) {
}
