package com.dave.ai.bi.helper.application.sqlchat.dto;

public record SqlExportRequest(String sessionId,
                               String userId,
                               String sql) {
}
