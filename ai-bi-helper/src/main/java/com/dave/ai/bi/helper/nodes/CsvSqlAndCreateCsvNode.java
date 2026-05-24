package com.dave.ai.bi.helper.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvSqlAndCreateCsvNode implements NodeAction {

    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final JdbcTemplate jdbcTemplate;

    public CsvSqlAndCreateCsvNode(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String genSQL = state.value("genSQL", "");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(genSQL);
        Path csvFilePath = exportToCsv(rows);

        return Map.of(
                "csvFilePath", csvFilePath.toAbsolutePath().toString()
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
