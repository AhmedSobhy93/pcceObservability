package com.cisco.cx.observability.model;

public record SchemaColumn(
        String tableName,
        String columnName,
        String typeName,
        int columnSize,
        boolean nullable
) {
}
