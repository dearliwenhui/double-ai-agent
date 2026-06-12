package com.dave.ai.bi.helper.application.sqlchat.service;

import com.dave.ai.bi.helper.application.sqlchat.dto.SqlChatMessageResponse;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlChatSessionDetail;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlChatSessionStartResponse;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlChatSessionSummary;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlExportResponse;
import com.dave.ai.bi.helper.application.sqlchat.dto.SqlPreviewResponse;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SqlChatApplicationService {

    private final SqlChatSessionService sessionService;
    private final SqlGenerationService generationService;
    private final SqlEvaluationService evaluationService;
    private final SqlPreviewService previewService;
    private final CsvExportService exportService;

    public SqlChatApplicationService(SqlChatSessionService sessionService,
                                     SqlGenerationService generationService,
                                     SqlEvaluationService evaluationService,
                                     SqlPreviewService previewService,
                                     CsvExportService exportService) {
        this.sessionService = sessionService;
        this.generationService = generationService;
        this.evaluationService = evaluationService;
        this.previewService = previewService;
        this.exportService = exportService;
    }

    public SqlChatSessionStartResponse startSession(String userId) {
        return sessionService.startSession(userId);
    }

    public List<SqlChatSessionSummary> listSessions(String userId) {
        return sessionService.listSessions(userId);
    }

    public SqlChatSessionDetail sessionDetail(String sessionId, String userId) {
        return sessionService.getSessionDetail(sessionId, userId);
    }

    public SqlChatMessageResponse sendMessage(String sessionId, String userId, String userInput) {
        sessionService.requireSession(sessionId, userId);
        sessionService.saveUserMessage(sessionId, userId, userInput);

        SqlGenerationService.SqlGenerationResult generated = generationService.generate(userInput);
        SqlEvaluationService.SqlEvaluationResult evaluated = evaluationService.evaluate(
                userInput,
                generated.generatedSql(),
                generated.retrievalContext()
        );

        String assistantReply = buildAssistantReply(generated.generatedSql(), evaluated.evaluateResult());
        sessionService.saveAssistantMessage(
                sessionId,
                userId,
                assistantReply,
                generated.generatedSql(),
                evaluated.rawEvaluateJson(),
                generated.retrievalContext()
        );
        sessionService.updateSessionAfterMessage(
                sessionId,
                userId,
                userInput,
                generated.generatedSql(),
                evaluated.rawEvaluateJson()
        );

        boolean previewAllowed = Boolean.TRUE.equals(evaluated.evaluateResult().get("pass"))
                || !"REJECT".equalsIgnoreCase(String.valueOf(evaluated.evaluateResult().get("finalDecision")));
        return new SqlChatMessageResponse(
                sessionId,
                userId,
                assistantReply,
                generated.generatedSql(),
                generated.retrievalContext(),
                generated.rewrittenQuery(),
                evaluated.evaluateResult(),
                "EVALUATED",
                previewAllowed
        );
    }

    public SqlPreviewResponse preview(String sessionId, String userId, String sql) {
        sessionService.requireSession(sessionId, userId);
        SqlPreviewResponse previewResponse = previewService.preview(sessionId, userId, sql);
        Map<String, Object> previewSummary = new LinkedHashMap<>();
        previewSummary.put("limit", previewResponse.limit());
        previewSummary.put("columns", previewResponse.columns());
        previewSummary.put("rowCount", previewResponse.rows().size());
        previewSummary.put("success", previewResponse.success());
        sessionService.savePreviewMessage(
                sessionId,
                userId,
                previewResponse.sql(),
                sessionService.toJson(previewSummary)
        );
        sessionService.updateSessionAfterPreview(sessionId, userId, previewResponse.sql());
        return previewResponse;
    }

    public SqlExportResponse export(String sessionId, String userId, String sql) throws Exception {
        sessionService.requireSession(sessionId, userId);
        if (!sessionService.hasSuccessfulPreview(sessionId, userId)) {
            throw new IllegalStateException("Please preview the first 20 rows successfully before exporting CSV");
        }
        SqlExportResponse exportResponse = exportService.export(sessionId, userId, sql);
        sessionService.saveExportMessage(sessionId, userId, exportResponse.sql(), exportResponse.csvFilePath());
        sessionService.updateSessionAfterExport(sessionId, userId, exportResponse.sql(), exportResponse.csvFilePath());
        return exportResponse;
    }

    private String buildAssistantReply(String generatedSql, Map<String, Object> evaluateResult) {
        Object score = evaluateResult.getOrDefault("score", 0);
        Object decision = evaluateResult.getOrDefault("finalDecision", "REVIEW");
        Object riskLevel = evaluateResult.getOrDefault("riskLevel", "UNKNOWN");
        if (generatedSql == null || generatedSql.isBlank()) {
            return "No valid SQL was generated. Please refine the business request and try again.";
        }
        return "SQL has been generated and evaluated."
                + " score=" + score
                + ", riskLevel=" + riskLevel
                + ", decision=" + decision
                + ". Review the SQL and evaluation result before previewing the first 20 rows.";
    }
}
