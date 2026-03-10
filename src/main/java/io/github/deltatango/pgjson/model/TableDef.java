package io.github.deltatango.pgjson.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Represents a schema definition stored in the {@code tabledef} metadata table.
 *
 * <p>Each row in {@code tabledef} maps a JSON Schema (2020-12) to a user-defined data table.
 * The schema is used for validation on insert/update and for extracting index hints and
 * UI label vocabulary extensions.</p>
 *
 * <p>Multiple schema versions can coexist for the same table; the "effective" schema is the
 * most recent one per {@code schema_name}.</p>
 *
 * @see io.github.deltatango.pgjson.model.repo.TableDefRepo
 */
@Data
public class TableDef {

    /** Database column name for the auto-incremented primary key. */
    public static final String TABLE_DEF_ID_COLUMN = "id";
    /** Database column name for the UUID identifier of this schema definition. */
    public static final String ID_UUID_COLUMN = "id_uuid";
    /** Database column name for the raw JSON Schema string. */
    public static final String SCHEMA_COLUMN = "schema";
    /** Database column name for the human-readable schema name (e.g., "user-profile-v1"). */
    public static final String SCHEMA_NAME_COLUMN = "schema_name";
    /** Database column name for the target data table this schema applies to. */
    public static final String TABLE_NAME_COLUMN = "table_name";
    /** Database column name for the timestamp when this schema was inserted. */
    public static final String SCHEMA_DATE_COLUMN = "schema_timestamp";
    /** Database column name for the SHA-256 hash used for cache invalidation. */
    public static final String SCHEMA_HASH_COLUMN = "schema_hash";
    /** The name of the metadata table itself ({@code "tabledef"}). */
    public static final String TABLE_NAME = "tabledef";

    /** Auto-incremented primary key in the {@code tabledef} table. */
    private Integer tableDefId;
    /** UUID identifier for this schema definition. */
    private String idUuid;
    /** Raw JSON Schema 2020-12 string, including PgJson vocabulary extensions. */
    private String schemaData;
    /** Human-readable schema name (e.g., "user-profile-v1"). */
    private String schemaName;
    /** Name of the user-defined data table this schema applies to. */
    private String tableName;
    /** Timestamp when this schema definition was inserted into the database. */
    private LocalDateTime schemaDate;
    /** SHA-256 hash of {@link #schemaData}, used by {@link io.github.deltatango.pgjson.util.SchemaCache} for invalidation. */
    private String schemaHash;
    /** Parsed index metadata extracted from the schema's {@code x-pgjson-index} vocabulary extensions. */
    private ArrayList<IndexInfo> indexInfo;
}
