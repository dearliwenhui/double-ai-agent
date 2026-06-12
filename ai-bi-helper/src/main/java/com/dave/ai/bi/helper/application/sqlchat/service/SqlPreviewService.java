package com.dave.ai.bi.helper.application.sqlchat.service;

import com.dave.ai.bi.helper.application.sqlchat.dto.SqlPreviewResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SqlPreviewService {

    public static final int PREVIEW_LIMIT = 20;

    private final JdbcTemplate jdbcTemplate;
    private final SqlSafetyGuardService safetyGuardService;

    public SqlPreviewService(JdbcTemplate jdbcTemplate,
                             SqlSafetyGuardService safetyGuardService) {
        this.jdbcTemplate = jdbcTemplate;
        this.safetyGuardService = safetyGuardService;
    }

    public SqlPreviewResponse preview(String sessionId, String userId, String sql) {
        String previewSql = safetyGuardService.buildPreviewSql(sql, PREVIEW_LIMIT);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(previewSql);
        List<String> columns = rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());
        return new SqlPreviewResponse(
                sessionId,
                userId,
                safetyGuardService.normalizeSql(sql),
                PREVIEW_LIMIT,
                columns,
                rows,
                true,
                "预览成功"
        );
    }
}
