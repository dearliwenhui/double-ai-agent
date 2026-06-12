package com.dave.ai.bi.helper.application.sqlchat.service;

import com.dave.ai.bi.helper.application.sqlchat.dto.SqlExportResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CsvExportService {

    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final JdbcTemplate jdbcTemplate;
    private final SqlSafetyGuardService safetyGuardService;

    public CsvExportService(JdbcTemplate jdbcTemplate,
                            SqlSafetyGuardService safetyGuardService) {
        this.jdbcTemplate = jdbcTemplate;
        this.safetyGuardService = safetyGuardService;
    }

    public SqlExportResponse export(String sessionId, String userId, String sql) throws Exception {
        safetyGuardService.validateReadOnlySelect(sql);
        String normalizedSql = safetyGuardService.normalizeSql(sql);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(normalizedSql);
        Path csvFilePath = exportToCsv(rows);
        return new SqlExportResponse(
                sessionId,
                userId,
                normalizedSql,
                csvFilePath.toAbsolutePath().toString(),
                LocalDateTime.now(),
                true,
                "CSV 导出成功"
        );
    }

    private Path exportToCsv(List<Map<String, Object>> rows) throws Exception {
        Path outputDir = Paths.get("ai-bi-helper", "output");
        Files.createDirectories(outputDir);

        Path csvFilePath = outputDir.resolve("query-result-" + FILE_NAME_FORMATTER.format(LocalDateTime.now()) + ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(csvFilePath);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            if (rows.isEmpty()) {
                return csvFilePath;
            }
            List<String> headers = new ArrayList<>(rows.get(0).keySet());
            csvPrinter.printRecord(headers);
            for (Map<String, Object> row : rows) {
                List<Object> record = new ArrayList<>(headers.size());
                for (String header : headers) {
                    record.add(row.get(header));
                }
                csvPrinter.printRecord(record);
            }
            csvPrinter.flush();
        }
        return csvFilePath;
    }
}
