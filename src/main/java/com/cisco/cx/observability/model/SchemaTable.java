package com.cisco.cx.observability.model;

public record SchemaTable(
        String tableCatalog,
        String tableSchema,
        String tableName,
        String tableType
) {
}
