package com.dave.ai.bi.helper.application.sqlchat.service;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class SqlSafetyGuardService {

    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            " insert ", " update ", " delete ", " drop ", " alter ", " truncate ", " create ",
            " replace ", " grant ", " revoke ", " rename ", " merge ", " call "
    );

    public String normalizeSql(String sql) {
        if (sql == null) {
            return "";
        }
        String normalized = sql.trim();
        while (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    public void validateReadOnlySelect(String sql) {
        String normalized = normalizeSql(sql);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("SQL 不能为空");
        }
        String lowered = " " + normalized.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ") + " ";
        if (!(lowered.startsWith(" select ") || lowered.startsWith(" with "))) {
            throw new IllegalArgumentException("仅允许执行 SELECT/CTE 查询");
        }
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (lowered.contains(keyword)) {
                throw new IllegalArgumentException("检测到危险 SQL 关键字: " + keyword.trim().toUpperCase(Locale.ROOT));
            }
        }
    }

    public String buildPreviewSql(String sql, int limit) {
        validateReadOnlySelect(sql);
        String normalized = normalizeSql(sql);
        return "SELECT * FROM (" + normalized + ") sql_chat_preview LIMIT " + Math.max(1, limit);
    }
}
