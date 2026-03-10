package io.github.deltatango.pgjson;

import com.google.gson.Gson;
import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import io.github.deltatango.pgjson.exceptions.RequestException;
import io.github.deltatango.pgjson.model.DatabaseEntry;
import io.github.deltatango.pgjson.model.TableDef;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import io.github.deltatango.pgjson.model.operations.PagedResult;
import io.github.deltatango.pgjson.model.operations.Result;
import io.github.deltatango.pgjson.model.repo.DatabaseEntryRepo;
import io.github.deltatango.pgjson.model.repo.TableDefRepo;
import io.github.deltatango.pgjson.util.DbUtil;
import io.github.deltatango.pgjson.util.JsonUtil;
import io.github.deltatango.pgjson.util.ManagedConnection;
import io.github.deltatango.pgjson.util.ValidationUtil;
import io.github.deltatango.pgjson.util.DatabaseOperationExecutor;
import io.github.deltatango.pgjson.util.RetryConfig;
import io.github.deltatango.pgjson.util.CircuitBreaker;
import io.github.deltatango.pgjson.constants.ApplicationConstants;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 * Main entry point for the PgJson library.
 *
 * <p>This class acts as a <strong>facade</strong> that delegates to focused internal services
 * ({@code SchemaService}, {@code DataService}, {@code SearchService}, {@code UiLabelService}).
 * It owns the connection lifecycle and transaction boundaries.</p>
 *
 * <p>Multi-step write operations (insert, update, merge, deleteArrayElement) are automatically
 * wrapped in database transactions. Read-only and single-statement operations use auto-commit.</p>
 *
 * <p>Each public method opens exactly <strong>one</strong> connection from the pool, which is
 * shared across all internal steps of the operation.</p>
 *
 * @author PostgreSQL JSON Client Team
 * @version 26.2.1
 * @since 1.0.0
 */
@Slf4j
public class PostgreSqlJsonClient implements AutoCloseable {

    private static final String EXCEPTION_HAPPENED_MESSAGE = ApplicationConstants.EXCEPTION_HAPPENED_MESSAGE;

    private volatile boolean closed = false;

    private DbUtil dbUtil;
    private DatabaseOperationExecutor operationExecutor;

    // Internal services (package-private, not part of public API)
    private SchemaService schemaService;
    private DataService dataService;
    private SearchService searchService;
    private UiLabelService uiLabelService;

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("PostgreSqlJsonClient has been closed");
        }
    }

    /**
     * Creates a new PgJson client and initializes the connection pool and internal services.
     *
     * <p>On construction, all effective schema definitions are loaded into memory from the
     * {@code tabledef} table. The connection pool is configured via HikariCP properties.</p>
     *
     * @param props JDBC and HikariCP configuration properties (e.g., {@code jdbcUrl},
     *              {@code dataSource.user}, {@code dataSource.password})
     * @throws PostgreJsonException if initialization fails (connection, schema loading, etc.)
     */
    public PostgreSqlJsonClient(Properties props) throws PostgreJsonException {
        try {
            Gson gson = new Gson();
            dbUtil = new DbUtil(props);
            JsonUtil jsonUtil = new JsonUtil();
            ValidationUtil validationUtil = new ValidationUtil();

            RetryConfig retryConfig = RetryConfig.defaultConfig();
            CircuitBreaker circuitBreaker = CircuitBreaker.defaultBreaker();
            operationExecutor = new DatabaseOperationExecutor(retryConfig, circuitBreaker);

            TableDefRepo tableDefRepo = new TableDefRepo();
            DatabaseEntryRepo databaseEntryRepo = new DatabaseEntryRepo();

            schemaService = new SchemaService(tableDefRepo, validationUtil);
            dataService = new DataService(databaseEntryRepo, schemaService, jsonUtil, gson);
            searchService = new SearchService(databaseEntryRepo, schemaService, gson);
            uiLabelService = new UiLabelService(schemaService, gson);

            // Load effective table definitions into memory
            try (ManagedConnection mc = dbUtil.getManagedConnection()) {
                schemaService.loadAllEffectiveTableDef(mc.getConnection());
            }
        } catch (Exception exc) {
            log.error("Initialization of PostgreSqlJsonClient failed", exc);
            throw new PostgreJsonException("Failed to initialize PostgreSqlJsonClient", exc);
        }
    }

    /**
     * Returns the database utility managing the HikariCP connection pool.
     *
     * @return the {@link DbUtil} instance
     * @throws IllegalStateException if this client has been closed
     */
    public DbUtil getDbUtil() {
        ensureOpen();
        return dbUtil;
    }

    /**
     * Returns the operation executor with retry and circuit breaker support.
     *
     * @return the {@link DatabaseOperationExecutor} instance
     * @throws IllegalStateException if this client has been closed
     */
    public DatabaseOperationExecutor getOperationExecutor() {
        ensureOpen();
        return operationExecutor;
    }

    // ---- Schema operations ----

    /**
     * Retrieves a schema definition by UUID, resolving from the in-memory cache first.
     *
     * @param idUuid the UUID of the schema definition
     * @return {@link OperationResult.Success} with the {@link TableDef},
     *         {@link OperationResult.NotFound} if not found,
     *         or {@link OperationResult.Error} on failure
     */
    public OperationResult<TableDef> getTableDefByIdUuidFromMemory(String idUuid) {
        ensureOpen();
        try (ManagedConnection mc = dbUtil.getManagedConnection()) {
            return schemaService.resolveTableDefByIdUuid(mc.getConnection(), idUuid);
        } catch (SQLException e) {
            log.error("Database error in getTableDefByIdUuidFromMemory", e);
            return OperationResult.error("Failed to retrieve table definition", e);
        } catch (Exception exc) {
            log.error("Unexpected error in method getTableDefByIdUuidFromMemory", exc);
            return OperationResult.error("Failed to retrieve table definition", exc);
        }
    }

    /**
     * Retrieves a schema definition by table name and schema name, resolving from memory first.
     *
     * @param tableName  the data table name
     * @param schemaName the human-readable schema name
     * @return {@link OperationResult.Success} with the {@link TableDef},
     *         {@link OperationResult.NotFound} if not found,
     *         or {@link OperationResult.Error} on failure
     */
    public OperationResult<TableDef> getTableDefByTableNameAndNameFromMemory(String tableName, String schemaName) {
        ensureOpen();
        try (ManagedConnection mc = dbUtil.getManagedConnection()) {
            return schemaService.resolveTableDefByTableNameAndName(mc.getConnection(), tableName, schemaName);
        } catch (SQLException e) {
            log.error("Database error in getTableDefByTableNameAndNameFromMemory", e);
            return OperationResult.error("Failed to retrieve table definition", e);
        } catch (Exception exc) {
            log.error("Unexpected error in method getTableDefByTableNameAndNameFromMemory", exc);
            return OperationResult.error("Failed to retrieve table definition", exc);
        }
    }

    /**
     * Retrieves a schema definition by its primary key ID, resolving from memory first.
     *
     * @param tableDefId the auto-incremented primary key
     * @return {@link OperationResult.Success} with the {@link TableDef},
     *         {@link OperationResult.NotFound} if not found,
     *         or {@link OperationResult.Error} on failure
     */
    public OperationResult<TableDef> getTableDefByIdFromMemory(Integer tableDefId) {
        ensureOpen();
        try (ManagedConnection mc = dbUtil.getManagedConnection()) {
            return schemaService.resolveTableDefById(mc.getConnection(), tableDefId);
        } catch (SQLException e) {
            log.error("Database error in getTableDefByIdFromMemory", e);
            return OperationResult.error("Failed to retrieve table definition", e);
        } catch (Exception exc) {
            log.error("Unexpected error in method getTableDefByIdFromMemory", exc);
            return OperationResult.error("Failed to retrieve table definition", exc);
        }
    }

    /**
     * Retrieves a schema definition by UUID directly from the database.
     *
     * @param idUuid the UUID of the schema definition (must not be null or empty)
     * @return {@link OperationResult.Success} with the {@link TableDef},
     *         {@link OperationResult.NotFound} if not found,
     *         or {@link OperationResult.Error} on failure or invalid input
     */
    public OperationResult<TableDef> getTableDefByIdUuid(String idUuid) {
        ensureOpen();
        if (idUuid == null || idUuid.trim().isEmpty()) {
            return OperationResult.error("ID UUID cannot be null or empty");
        }

        try (ManagedConnection mc = dbUtil.getManagedConnection()) {
            return schemaService.resolveTableDefByIdUuid(mc.getConnection(), idUuid);
        } catch (Exception exc) {
            log.error("Exception in method getTableDefByIdUuid", exc);
            return OperationResult.error("Failed to retrieve table definition", exc);
        }
    }

    /**
     * Inserts a new JSON Schema definition and creates the associated data table and indexes.
     *
     * @param tableName  the name of the data table to create or associate with
     * @param schemaName a human-readable schema name (e.g., "user-profile-v1")
     * @param schemaData the raw JSON Schema 2020-12 string
     * @return {@link OperationResult.Success} with the generated UUID of the new schema,
     *         or {@link OperationResult.Error} on failure or invalid input
     */
    public OperationResult<String> insertSchema(String tableName, String schemaName, String schemaData) {
        ensureOpen();
        if (tableName == null || tableName.trim().isEmpty()) {
            return OperationResult.error("Table name cannot be null or empty");
        }
        if (schemaName == null || schemaName.trim().isEmpty()) {
            return OperationResult.error("Schema name cannot be null or empty");
        }
        if (schemaData == null || schemaData.trim().isEmpty()) {
            return OperationResult.error("Schema data cannot be null or empty");
        }

        try (ManagedConnection mc = dbUtil.getManagedConnection()) {
            return schemaService.insertSchema(mc.getConnection(), tableName, schemaName, schemaData);
        } catch (SQLException e) {
            log.error("Database error in insertSchema", e);
            return OperationResult.error("Failed to insert schema into database", e);
        } catch (Exception exc) {
            log.error("Unexpected error in method insertSchema", exc);
            return OperationResult.error("Failed to insert schema", exc);
        }
    }

    // ---- Data operations (transactional) ----

    /**
     * Inserts a JSON document into the specified table, using the latest schema for validation.
     *
     * <p>This operation is transactional: the insert is committed on success and rolled back on failure.</p>
     *
     * @param tableName the data table name
     * @param jsonData  the JSON document to insert
     * @return {@link OperationResult.Success} with the {@link Result} containing the new entry's UUID,
     *         or {@link OperationResult.Error} on validation failure or database error
     */
    public OperationResult<Result> insertData(String tableName, String jsonData) {
        ensureOpen();
        return insertDataImpl(tableName, null, jsonData);
    }

    /**
     * Inserts a JSON document into the specified table, validating against a specific schema version.
     *
     * <p>This operation is transactional: the insert is committed on success and rolled back on failure.</p>
     *
     * @param tableName  the data table name
     * @param schemaName the schema name to validate against (e.g., "user-profile-v2")
     * @param jsonData   the JSON document to insert
     * @return {@link OperationResult.Success} with the {@link Result} containing the new entry's UUID,
     *         or {@link OperationResult.Error} on validation failure or database error
     */
    public OperationResult<Result> insertData(String tableName, String schemaName, String jsonData) {
        ensureOpen();
        if (schemaName == null || schemaName.trim().isEmpty()) {
            return OperationResult.error("Schema name cannot be null or empty");
        }
        return insertDataImpl(tableName, schemaName, jsonData);
    }

    private OperationResult<Result> insertDataImpl(String tableName, String schemaName, String jsonData) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return OperationResult.error("Table name cannot be null or empty");
        }
        if (jsonData == null || jsonData.trim().isEmpty()) {
            return OperationResult.error("JSON data cannot be null or empty");
        }

        try (ManagedConnection mc = dbUtil.getManagedConnection()) {
            Connection connection = mc.getConnection();
            connection.setAutoCommit(false);
            try {
                OperationResult<Result> result = dataService.insertData(connection, tableName, schemaName, jsonData);
                if (result.isSuccess()) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
                return result;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument in insertDataImpl", e);
            throw e;
        } catch (SQLException e) {
            log.error("Database error in insertDataImpl", e);
            return OperationResult.error("Failed to insert data into database", e);
        } catch (Exception exc) {
            log.error("Unexpected error in method insertDataImpl", exc);
            return OperationResult.error("Failed to insert data", exc);
        }
    }

    /**
     * Replaces the JSON data of an existing entry (full replacement, no merge).
     *
     * <p>This operation is transactional: the update is committed on success and rolled back on failure.
     * The document is validated against the schema before updating.</p>
     *
     * @param tableName   the data table name
     * @param jsonData    the new JSON document (replaces the existing one entirely)
     * @param entryIdUuid the UUID of the entry to update
     * @return {@link OperationResult.Success} with the {@link Result},
     *         or {@link OperationResult.Error} on validation failure or database error
     */
    public OperationResult<Result> updateData(String tableName, String jsonData, String entryIdUuid) {
        ensureOpen();
        if (tableName == null || tableName.trim().isEmpty()) {
            return OperationResult.error("Table name cannot be null or empty");
        }
        if (jsonData == null || jsonData.trim().isEmpty()) {
            return OperationResult.error("JSON data cannot be null or empty");
        }
        if (entryIdUuid == null || entryIdUuid.trim().isEmpty()) {
            return OperationResult.error("Entry ID UUID cannot be null or empty");
        }

        try (ManagedConnection mc = dbUtil.getManagedConnection()) {
            Connection connection = mc.getConnection();
            connection.setAutoCommit(false);
            try {
                OperationResult<Result> result = dataService.updateData(connection, tableName, jsonData, entryIdUuid);
                if (result.isSuccess()) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
                return result;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (Exception exc) {
            log.error("Exception in method updateData", exc);
            return OperationResult.error(EXCEPTION_HAPPENED_MESSAGE, exc);
        }
    }

    /**
     * Merges partial JSON data into an existing entry (deep merge, not full replacement).
     *
     * <p>This operation is transactional: the existing document is fetched, merged with
     * the provided data, validated against the schema, and then persisted.</p>
     *
     * @param tableName   the data table name
     * @param data        the partial JSON data to merge into the existing document
     * @param entryIdUuid the UUID of the entry to update
     * @return {@link OperationResult.Success} with the {@link Result} containing the merged document,
     *         or {@link OperationResult.Error} on validation failure or database error
     */
    public OperationResult<Result> updateDataWithMerge(String tableName, String data, String entryIdUuid) {
        ensureOpen();
        try (ManagedConnection mc = dbUtil.getManagedConnection()) {
            Connection connection = mc.getConnection();
            connection.setAutoCommit(false);
            try {
                OperationResult<Result> result = dataService.updateDataWithMerge(connection, tableName, data, entryIdUuid);
                if (result.isSuccess()) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
                return result;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (Exception exc) {
            log.error("Exception in method updateDataWithMerge", exc);
            return OperationResult.error(EXCEPTION_HAPPENED_MESSAGE, exc);
        }
    }

    /**
     * Deletes a database entry by its UUID.
     *
     * @param tableName the data table name
     * @param idUuid    the UUID of the entry to delete
     * @return {@link OperationResult.Success} with {@code true} if the entry was deleted,
     *         or {@link OperationResult.Error} on failure or invalid input
     */
    public OperationResult<Boolean> deleteData(String tableName, String idUuid) {
        ensureOpen();
        if (tableName == null || tableName.trim().isEmpty()) {
            return OperationResult.error("Table name cannot be null or empty");
        }
        if (idUuid == null || idUuid.trim().isEmpty()) {
            return OperationResult.error("ID UUID cannot be null or empty");
        }

        try (ManagedConnection mc = dbUtil.getManagedConnection()) {
            return dataService.deleteData(mc.getConnection(), tableName, idUuid);
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument in deleteData", e);
            throw e;
        } catch (SQLException e) {
            log.error("Database error in deleteData", e);
            return OperationResult.error("Failed to delete data from database", e);
        } catch (Exception exc) {
            log.error("Unexpected error in method deleteData", exc);
            return OperationResult.error("Failed to delete data", exc);
        }
    }

    /**
     * Removes an element from a JSON array within an existing entry.
     *
     * <p>This operation is transactional: the array element is removed, the document is
     * re-validated against the schema, and the result is committed or rolled back.</p>
     *
     * @param tableName   the data table name
     * @param data        JSON specifying the array key and element index to remove
     * @param entryIdUuid the UUID of the entry containing the array
     * @return {@link OperationResult.Success} with the {@link Result} containing the updated document,
     *         or {@link OperationResult.Error} on failure
     */
    public OperationResult<Result> deleteArrayElement(String tableName, String data, String entryIdUuid) {
        ensureOpen();
        try (ManagedConnection mc = dbUtil.getManagedConnection()) {
            Connection connection = mc.getConnection();
            connection.setAutoCommit(false);
            try {
                OperationResult<Result> result = dataService.deleteArrayElement(connection, tableName, data, entryIdUuid);
                if (result.isSuccess()) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
                return result;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (Exception exc) {
            log.error("Exception in method deleteArrayElement", exc);
            return OperationResult.error(EXCEPTION_HAPPENED_MESSAGE, exc);
        }
    }

    // ---- Select / Search operations (read-only, auto-commit) ----

    /**
     * Retrieves a single database entry by its UUID.
     *
     * <p>This is a read-only operation using auto-commit.</p>
     *
     * @param idUuid    the UUID of the entry to retrieve
     * @param tableName the data table name
     * @return {@link OperationResult.Success} with the {@link DatabaseEntry},
     *         {@link OperationResult.NotFound} if no match exists,
     *         or {@link OperationResult.Error} on failure
     */
    public OperationResult<DatabaseEntry> selectDataByIdUuid(String idUuid, String tableName) {
        ensureOpen();
        if (idUuid == null || idUuid.trim().isEmpty()) {
            return OperationResult.error("ID UUID cannot be null or empty");
        }
        if (tableName == null || tableName.trim().isEmpty()) {
            return OperationResult.error("Table name cannot be null or empty");
        }

        try (ManagedConnection mc = dbUtil.getManagedConnection()) {
            return dataService.selectDataByIdUuid(mc.getConnection(), idUuid, tableName);
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument in selectDataByIdUuid", e);
            throw e;
        } catch (SQLException e) {
            log.error("Database error in selectDataByIdUuid", e);
            return OperationResult.error("Failed to retrieve data from database", e);
        } catch (Exception exc) {
            log.error("Unexpected error in method selectDataByIdUuid", exc);
            return OperationResult.error("Failed to retrieve data", exc);
        }
    }

    /**
     * Searches for JSON documents matching the given search criteria, using the latest schema.
     *
     * <p>The search JSON must contain {@code limit} and {@code offset} fields, and optionally
     * {@code searchTerm}, {@code orderType}, and {@code logicalOperator}.</p>
     *
     * @param tableName  the data table name
     * @param searchJson the search criteria as a JSON string
     * @return {@link OperationResult.Success} with the list of matching {@link DatabaseEntry} objects,
     *         or {@link OperationResult.Error} on failure
     * @throws RequestException if the search JSON is malformed or missing required fields
     */
    public OperationResult<List<DatabaseEntry>> selectData(String tableName, String searchJson) throws RequestException {
        ensureOpen();
        return selectDataImpl(tableName, null, searchJson);
    }

    /**
     * Searches for JSON documents matching the given search criteria, using a specific schema version.
     *
     * @param tableName  the data table name
     * @param schemaName the schema name to use for index resolution
     * @param searchJson the search criteria as a JSON string
     * @return {@link OperationResult.Success} with the list of matching {@link DatabaseEntry} objects,
     *         or {@link OperationResult.Error} on failure
     * @throws RequestException if the search JSON is malformed or missing required fields
     */
    public OperationResult<List<DatabaseEntry>> selectData(String tableName, String schemaName, String searchJson) throws RequestException {
        ensureOpen();
        return selectDataImpl(tableName, schemaName, searchJson);
    }

    private OperationResult<List<DatabaseEntry>> selectDataImpl(String tableName, String schemaName, String searchJson)
            throws RequestException {
        if (tableName == null || tableName.trim().isEmpty()) {
            return OperationResult.error("Table name cannot be null or empty");
        }

        try (ManagedConnection mc = dbUtil.getManagedConnection()) {
            return searchService.selectData(mc.getConnection(), tableName, schemaName, searchJson);
        } catch (RequestException | com.google.gson.JsonSyntaxException e) {
            throw e;
        } catch (Exception exc) {
            log.error("Exception in method selectDataImpl", exc);
            return OperationResult.error("Search operation failed", exc);
        }
    }

    /**
     * Searches for JSON documents and returns a paginated result with total count,
     * using the latest schema for index resolution.
     *
     * <p>This method executes both a {@code COUNT(*)} query and a data query using the same
     * filters, returning the results in a {@link PagedResult} that includes total count metadata
     * suitable for building pagination UI.</p>
     *
     * @param tableName  the data table name
     * @param searchJson the search criteria as a JSON string
     * @return {@link OperationResult.Success} with the {@link PagedResult},
     *         or {@link OperationResult.Error} on failure
     * @throws RequestException if the search JSON is malformed or missing required fields
     */
    public OperationResult<PagedResult<DatabaseEntry>> selectDataWithCount(String tableName, String searchJson) throws RequestException {
        ensureOpen();
        return selectDataWithCountImpl(tableName, null, searchJson);
    }

    /**
     * Searches for JSON documents and returns a paginated result with total count,
     * using a specific schema version for index resolution.
     *
     * @param tableName  the data table name
     * @param schemaName the schema name to use for index resolution
     * @param searchJson the search criteria as a JSON string
     * @return {@link OperationResult.Success} with the {@link PagedResult},
     *         or {@link OperationResult.Error} on failure
     * @throws RequestException if the search JSON is malformed or missing required fields
     */
    public OperationResult<PagedResult<DatabaseEntry>> selectDataWithCount(String tableName, String schemaName, String searchJson) throws RequestException {
        ensureOpen();
        return selectDataWithCountImpl(tableName, schemaName, searchJson);
    }

    private OperationResult<PagedResult<DatabaseEntry>> selectDataWithCountImpl(String tableName, String schemaName, String searchJson)
            throws RequestException {
        if (tableName == null || tableName.trim().isEmpty()) {
            return OperationResult.error("Table name cannot be null or empty");
        }

        try (ManagedConnection mc = dbUtil.getManagedConnection()) {
            return searchService.selectDataWithCount(mc.getConnection(), tableName, schemaName, searchJson);
        } catch (RequestException | com.google.gson.JsonSyntaxException e) {
            throw e;
        } catch (Exception exc) {
            log.error("Exception in method selectDataWithCountImpl", exc);
            return OperationResult.error("Search with count operation failed", exc);
        }
    }

    // ---- UI Label operations ----

    /**
     * Extracts UI labels from the latest schema for the given table.
     *
     * <p>UI labels are defined via the {@code x-pgjson-uiLabel} vocabulary extension
     * in the JSON Schema.</p>
     *
     * @param tableName the data table name
     * @return {@link OperationResult.Success} with the UI labels as a JSON string,
     *         or {@link OperationResult.Error} on failure
     */
    public OperationResult<String> getUiLabels(String tableName) {
        ensureOpen();
        return getUiLabelsImpl(tableName, null);
    }

    /**
     * Extracts UI labels from a specific schema version for the given table.
     *
     * @param tableName  the data table name
     * @param schemaName the schema name to use
     * @return {@link OperationResult.Success} with the UI labels as a JSON string,
     *         or {@link OperationResult.Error} on failure
     */
    public OperationResult<String> getUiLabels(String tableName, String schemaName) {
        ensureOpen();
        return getUiLabelsImpl(tableName, schemaName);
    }

    private OperationResult<String> getUiLabelsImpl(String tableName, String schemaName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return OperationResult.error("Table name cannot be null or empty");
        }

        try (ManagedConnection mc = dbUtil.getManagedConnection()) {
            return uiLabelService.getUiLabels(mc.getConnection(), tableName, schemaName);
        } catch (SQLException e) {
            log.error("Database error in getUiLabelsImpl", e);
            return OperationResult.error("Failed to retrieve UI labels", e);
        } catch (Exception exc) {
            log.error("Exception in method getUiLabels", exc);
            return OperationResult.error("Failed to retrieve UI labels", exc);
        }
    }

    /**
     * Extracts UI labels directly from a schema JSON string (no database lookup).
     *
     * @param schema the raw JSON Schema string containing {@code x-pgjson-uiLabel} extensions
     * @return {@link OperationResult.Success} with the UI labels as a JSON string,
     *         or {@link OperationResult.Error} if the schema is null/empty or parsing fails
     */
    public OperationResult<String> getUiLabelsFromSchemaFile(String schema) {
        ensureOpen();
        if (schema == null || schema.trim().isEmpty()) {
            return OperationResult.error("Schema cannot be null or empty");
        }

        return uiLabelService.getUiLabelsFromSchemaFile(schema);
    }

    // ---- Health check ----

    /**
     * Checks if the database connection is active and responsive.
     */
    public boolean isDatabaseRunning() {
        ensureOpen();
        try (ManagedConnection mc = dbUtil.getManagedConnection()) {
            Connection connection = mc.getConnection();
            OperationResult<Boolean> result = new TableDefRepo().selectTestFromTableDef(connection);
            return result.isSuccess() && Boolean.TRUE.equals(((OperationResult.Success<Boolean>) result).value());
        } catch (Exception exc) {
            log.error("Exception in method isDatabaseRunning", exc);
            return false;
        }
    }

    // ---- Lifecycle ----

    /**
     * Closes this client and releases the underlying connection pool.
     *
     * <p>After calling this method, all subsequent operations will throw
     * {@link IllegalStateException}. This method is idempotent.</p>
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        try {
            log.debug("Closing postgresql-json database data source.");
            closed = true;
            if (dbUtil != null) {
                dbUtil.stop();
            }
        } catch (Exception exc) {
            log.error("Exception in method close", exc);
        }
    }
}
