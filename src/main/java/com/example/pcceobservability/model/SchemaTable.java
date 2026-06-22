package com.example.pcceobservability.model;

public record SchemaTable(
        String tableCatalog,
        String tableSchema,
        String tableName,
        String tableType
) {
}
