package com.dave.ai.bi.helper.controller;

import com.dave.ai.bi.helper.application.sqlchat.dto.SqlChatMessageResponse;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlChatSendMessageRequest;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlChatSessionDetail;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlChatSessionStartResponse;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlChatSessionSummary;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlChatStreamEvent;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlExportRequest;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlExportResponse;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlPreviewRequest;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlPreviewResponse;
import com.dave.ai.bi.helper.application.sqlchat.service.SqlChatApplicationService;
import com.dave.common.domain.vo.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/sql-chat")
public class SqlChatController {

    @Resource
    private SqlChatApplicationService sqlChatApplicationService;

    @Resource
    private ObjectMapper objectMapper;

    @PostMapping("/session/start")
    public R<SqlChatSessionStartResponse> startSession(@RequestParam String userId) {
        return R.success(sqlChatApplicationService.startSession(userId));
    }

    @GetMapping("/session/list")
    public R<List<SqlChatSessionSummary>> listSessions(@RequestParam String userId) {
        return R.success(sqlChatApplicationService.listSessions(userId));
    }

    @GetMapping("/session/detail")
    public R<SqlChatSessionDetail> sessionDetail(@RequestParam String sessionId, @RequestParam String userId) {
        return R.success(sqlChatApplicationService.sessionDetail(sessionId, userId));
    }

    @PostMapping("/message")
    public R<SqlChatMessageResponse> sendMessage(@RequestBody SqlChatSendMessageRequest request) {
        return R.success(sqlChatApplicationService.sendMessage(
                request.sessionId(),
                request.userId(),
                request.userInput()
        ));
    }

    @GetMapping("/message/stream")
    public SseEmitter sendMessageStream(@RequestParam String sessionId,
                                        @RequestParam String userId,
                                        @RequestParam String userInput) {
        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> {
            try {
                emit(emitter, "status", "Question accepted", Map.of("stage", "start"));
                emit(emitter, "status", "Retrieving schema context", Map.of("stage", "retrieve"));
                emit(emitter, "status", "Generating SQL", Map.of("stage", "generate"));
                SqlChatMessageResponse response = sqlChatApplicationService.sendMessage(sessionId, userId, userInput);
                emit(emitter, "status", "Producing evaluation result", Map.of("stage", "evaluate"));
                emit(emitter, "completed", "Generation completed", Map.of(
                        "sessionId", response.sessionId(),
                        "userId", response.userId(),
                        "assistantReply", response.assistantReply(),
                        "generatedSql", response.generatedSql(),
                        "retrievalContext", response.retrievalContext(),
                        "rewrittenQuery", response.rewrittenQuery(),
                        "evaluateResult", response.evaluateResult(),
                        "status", response.status(),
                        "previewAllowed", response.previewAllowed()
                ));
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emit(emitter, "error", ex.getMessage(), Map.of("stage", "failed"));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    @PostMapping("/sql/preview")
    public R<SqlPreviewResponse> preview(@RequestBody SqlPreviewRequest request) {
        return R.success(sqlChatApplicationService.preview(
                request.sessionId(),
                request.userId(),
                request.sql()
        ));
    }

    @PostMapping("/sql/export")
    public R<SqlExportResponse> export(@RequestBody SqlExportRequest request) throws Exception {
        return R.success(sqlChatApplicationService.export(
                request.sessionId(),
                request.userId(),
                request.sql()
        ));
    }

    private void emit(SseEmitter emitter, String type, String message, Map<String, Object> payload) throws IOException {
        String json = objectMapper.writeValueAsString(new SqlChatStreamEvent(type, message, payload));
        emitter.send(SseEmitter.event().name(type).data(json));
    }
}
