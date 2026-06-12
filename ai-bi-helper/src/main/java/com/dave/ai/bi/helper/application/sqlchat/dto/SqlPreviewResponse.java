package com.dave.ai.bi.helper.application.sqlchat.dto;

import java.util.List;
import java.util.Map;

public record SqlPreviewResponse(String sessionId,
                                 String userId,
                                 String sql,
                                 int limit,
                                 List<String> columns,
                                 List<Map<String, Object>> rows,
                                 boolean success,
                                 String message) {
}
