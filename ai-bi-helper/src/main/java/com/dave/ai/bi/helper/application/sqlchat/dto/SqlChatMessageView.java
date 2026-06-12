package com.dave.ai.bi.helper.application.sqlchat.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record SqlChatMessageView(Long id,
                                 String role,
                                 String messageType,
                                 String content,
                                 String generatedSql,
                                 String editedSql,
                                 Map<String, Object> evaluateResult,
                                 String retrievalContext,
                                 Map<String, Object> previewResult,
                                 LocalDateTime createdTime) {
}
