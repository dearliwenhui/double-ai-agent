package com.dave.ai.bi.helper.application.sqlchat.dto;

public record SqlChatSessionStartResponse(String sessionId,
                                          String userId,
                                          String sessionTitle,
                                          String status) {
}
