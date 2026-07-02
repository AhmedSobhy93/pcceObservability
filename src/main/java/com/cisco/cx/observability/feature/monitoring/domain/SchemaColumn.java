package com.cisco.cx.observability.feature.monitoring.domain;

public record SchemaColumn(
        String tableName,
        String columnName,
        String typeName,
        int columnSize,
        boolean nullable
) {
}
