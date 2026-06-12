package com.dave.ai.bi.helper.application.sqlchat.dto;

import java.time.LocalDateTime;

public record SqlExportResponse(String sessionId,
                                String userId,
                                String sql,
                                String csvFilePath,
                                LocalDateTime exportedAt,
                                boolean success,
                                String message) {
}
