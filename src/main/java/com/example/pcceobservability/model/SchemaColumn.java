package com.example.pcceobservability.model;

public record SchemaColumn(
        String tableName,
        String columnName,
        String typeName,
        int columnSize,
        boolean nullable
) {
}
