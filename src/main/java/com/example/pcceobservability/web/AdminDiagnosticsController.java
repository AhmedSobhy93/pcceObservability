package com.example.pcceobservability.web;

import com.example.pcceobservability.model.SchemaColumn;
import com.example.pcceobservability.model.SchemaTable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/diagnostics")
@PreAuthorize("hasAuthority('PERM_SOLUTION_ADMIN')")
public class AdminDiagnosticsController {

    private final DataSource awDataSource;
    private final DataSource hdsDataSource;
    private final DataSource cvpReportingDataSource;

    public AdminDiagnosticsController(
            @Qualifier("awDataSource") DataSource awDataSource,
            @Qualifier("hdsDataSource") DataSource hdsDataSource,
            @Qualifier("cvpReportingDataSource") DataSource cvpReportingDataSource) {
        this.awDataSource = awDataSource;
        this.hdsDataSource = hdsDataSource;
        this.cvpReportingDataSource = cvpReportingDataSource;
    }

    @GetMapping("/{source}/tables")
    public List<SchemaTable> tables(
            @PathVariable String source,
            @RequestParam(defaultValue = "%") String namePattern) throws SQLException {
        try (Connection connection = dataSource(source).getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<SchemaTable> tables = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(connection.getCatalog(), null, namePattern, new String[]{"TABLE", "VIEW"})) {
                while (rs.next() && tables.size() < 500) {
                    tables.add(new SchemaTable(
                            rs.getString("TABLE_CAT"),
                            rs.getString("TABLE_SCHEM"),
                            rs.getString("TABLE_NAME"),
                            rs.getString("TABLE_TYPE")));
                }
            }
            return tables;
        }
    }

    @GetMapping("/{source}/tables/{tableName}/columns")
    public List<SchemaColumn> columns(
            @PathVariable String source,
            @PathVariable String tableName) throws SQLException {
        try (Connection connection = dataSource(source).getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<SchemaColumn> columns = new ArrayList<>();
            try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, tableName, "%")) {
                while (rs.next()) {
                    columns.add(new SchemaColumn(
                            rs.getString("TABLE_NAME"),
                            rs.getString("COLUMN_NAME"),
                            rs.getString("TYPE_NAME"),
                            rs.getInt("COLUMN_SIZE"),
                            rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable));
                }
            }
            return columns;
        }
    }

    private DataSource dataSource(String source) {
        return switch (source.toLowerCase()) {
            case "aw" -> awDataSource;
            case "hds" -> hdsDataSource;
            case "cvp", "cvp-reporting", "cvp_reporting" -> cvpReportingDataSource;
            default -> throw new IllegalArgumentException("source must be one of: aw, hds, cvp-reporting");
        };
    }
}
