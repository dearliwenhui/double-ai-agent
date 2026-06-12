package com.dave.ai.bi.helper.application.sqlchat.dto;

import java.time.LocalDateTime;

public record SqlChatSessionSummary(String sessionId,
                                    String userId,
                                    String sessionTitle,
                                    String status,
                                    String lastQuestion,
                                    LocalDateTime updatedTime) {
}
