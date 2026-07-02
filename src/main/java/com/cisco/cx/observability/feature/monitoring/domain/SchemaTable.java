package com.cisco.cx.observability.feature.monitoring.domain;

public record SchemaTable(
        String tableCatalog,
        String tableSchema,
        String tableName,
        String tableType
) {
}
