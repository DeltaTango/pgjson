package io.github.deltatango.pgjson.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a JSON document stored in a user-defined data table.
 *
 * <p>Each user-defined table (created via schema insertion) stores rows of this shape.
 * The JSON payload is stored in a PostgreSQL {@code json} column and is validated against
 * the associated {@link io.github.deltatango.pgjson.model.TableDef} schema on insert and update.</p>
 *
 * @see io.github.deltatango.pgjson.model.repo.DatabaseEntryRepo
 */
@Data
public class DatabaseEntry implements Serializable {

    /** Database column name for the auto-incremented primary key. */
    public static final String ENTRY_DB_ID_COLUMN = "id";
    /** Database column name for the foreign key referencing {@code tabledef.id}. */
    public static final String SCHEMA_DB_ID_COLUMN = "tabledef_id";
    /** Database column name for the JSON document payload. */
    public static final String JSON_DATA_COLUMN = "json_data";
    /** Database column name for the UUID identifier of this entry. */
    public static final String ENTRY_ID_UUID_COLUMN = "id_uuid";
    /** Database column name for the creation timestamp. */
    public static final String DATA_CREATED_DATE_TIME_COLUMN = "data_created";
    /** Database column name for the last-modified timestamp. */
    public static final String DATA_CHANGED_DATE_TIME_COLUMN = "data_changed";

    /** Auto-incremented primary key. */
    Integer entryDbId;
    /** Foreign key referencing the {@code tabledef.id} of the schema used for validation. */
    Integer schemaDbId;
    /** The JSON document payload as a string. */
    String jsonData;
    /** UUID identifier for this entry, used for lookups and updates. */
    String entryIdUuid;
    /** Timestamp when this entry was first inserted. */
    LocalDateTime dataCreatedDateTime;
    /** Timestamp when this entry was last modified ({@code null} if never updated). */
    LocalDateTime dataChangedDateTime;
}
