package com.dave.ai.bi.helper.application.sqlchat.dto;

public record SqlChatSendMessageRequest(String sessionId,
                                        String userId,
                                        String userInput) {
}
