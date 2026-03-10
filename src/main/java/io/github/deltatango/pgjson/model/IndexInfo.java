package io.github.deltatango.pgjson.model;

import io.github.deltatango.pgjson.model.enums.IndexType;
import lombok.Data;

/**
 * Metadata about a PostgreSQL GIN index on a JSON data column.
 *
 * <p>Instances are populated from the {@code pg_indexes} system catalog for indexes
 * whose names start with {@code pgjson_}. The index name encodes the index type and
 * key path using the convention {@code pgjson_<type>_<table>_<key>}.</p>
 *
 * @see io.github.deltatango.pgjson.model.enums.IndexType
 */
@Data
public class IndexInfo {

    /** Column name in {@code pg_indexes} for the index name. */
    public static final String COLUMN_INDEX_NAME = "indexname";
    /** Column name in {@code pg_indexes} for the index definition SQL. */
    public static final String COLUMN_INDEX_DEF = "indexdef";

    /** The PostgreSQL index name (e.g., {@code pgjson_exact_users_email}). */
    private String indexName;
    /** The full {@code CREATE INDEX} definition as reported by {@code pg_indexes}. */
    private String indexDef;
    /** The indexing strategy (exact, fts, object, nestedobject). */
    private IndexType indexType;
    /** The JSON key path this index covers (e.g., {@code "email"} or {@code "address.city"}). */
    private String keyPath;

    /**
     * Sets the index type by parsing the given string into an {@link IndexType} enum value.
     *
     * @param indexTypeString one of {@code "exact"}, {@code "fts"}, {@code "object"}, or {@code "nestedobject"}
     * @throws IllegalArgumentException if the string does not match any {@link IndexType} value
     */
    public void setIndexType(String indexTypeString) {
        this.indexType = IndexType.valueOf(indexTypeString);
    }
}
