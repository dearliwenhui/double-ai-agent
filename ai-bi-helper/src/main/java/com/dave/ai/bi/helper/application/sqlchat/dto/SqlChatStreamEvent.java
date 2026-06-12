package com.dave.ai.bi.helper.application.sqlchat.dto;

import java.util.Map;

public record SqlChatStreamEvent(String type,
                                 String message,
                                 Map<String, Object> payload) {
}
