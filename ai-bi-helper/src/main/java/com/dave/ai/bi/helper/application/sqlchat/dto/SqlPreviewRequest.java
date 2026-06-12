package com.dave.ai.bi.helper.application.sqlchat.dto;

public record SqlPreviewRequest(String sessionId,
                                String userId,
                                String sql) {
}
