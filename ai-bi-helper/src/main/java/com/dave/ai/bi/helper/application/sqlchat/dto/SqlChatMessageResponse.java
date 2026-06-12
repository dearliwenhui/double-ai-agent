package com.dave.ai.bi.helper.application.sqlchat.dto;

import java.util.Map;

public record SqlChatMessageResponse(String sessionId,
                                     String userId,
                                     String assistantReply,
                                     String generatedSql,
                                     String retrievalContext,
                                     String rewrittenQuery,
                                     Map<String, Object> evaluateResult,
                                     String status,
                                     boolean previewAllowed) {
}
