package io.github.deltatango.pgjson.model.enums;

/**
 * The four PostgreSQL GIN indexing strategies supported by PgJson.
 *
 * <p>Each strategy maps to a different PostgreSQL index type and query operator,
 * optimized for a specific access pattern on the {@code json_data} column.
 * Index types are declared in JSON Schema via the {@code x-pgjson-index} vocabulary extension.</p>
 *
 * @see io.github.deltatango.pgjson.model.IndexInfo
 * @see io.github.deltatango.pgjson.util.IndexUtil
 */
public enum IndexType {
    /**
     * JSONB containment index ({@code @>}) for top-level object/array matching.
     *
     * <p>Uses {@code GIN ... jsonb_path_ops}. Best for queries that check whether a JSON
     * key's value contains a given sub-document or array element.</p>
     */
    object("object"),

    /**
     * JSONB containment index for nested object paths (e.g., {@code address.city}).
     *
     * <p>Similar to {@link #object} but traverses multiple levels of JSON nesting
     * using chained {@code ->} operators.</p>
     */
    nestedobject("nestedobject"),

    /**
     * Exact-match index on a scalar JSON value.
     *
     * <p>Uses a B-tree index on {@code json_data->>'key'}. Best for equality lookups
     * on primitive string or numeric fields.</p>
     */
    exact("exact"),

    /**
     * Full-text search index using PostgreSQL {@code tsvector}/{@code tsquery}.
     *
     * <p>Uses {@code GIN to_tsvector('simple', json_data->'key')}. Best for
     * multi-word text search on string fields.</p>
     */
    fts("fts");

    private final String propertyName;

    IndexType(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * Returns the lowercase property name used in index naming conventions and schema extensions.
     *
     * @return the property name (e.g., {@code "exact"}, {@code "fts"})
     */
    public String getPropertyName() {
        return this.propertyName;
    }
}
