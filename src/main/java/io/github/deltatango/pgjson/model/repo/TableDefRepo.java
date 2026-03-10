package io.github.deltatango.pgjson.model.repo;

import io.github.deltatango.pgjson.model.IndexInfo;
import io.github.deltatango.pgjson.model.TableDef;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import io.github.deltatango.pgjson.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access layer for the {@code tabledef} metadata table.
 *
 * <p>This repository handles all CRUD operations on schema definitions, including
 * inserting new schemas, querying by various keys (UUID, ID, table name, schema name),
 * loading effective (latest-per-schema-name) definitions, and managing GIN indexes
 * on user-defined data tables.</p>
 *
 * <p>All queries use {@link java.sql.PreparedStatement} with parameterized values.
 * The caller is responsible for providing an open {@link java.sql.Connection}.</p>
 *
 * @see io.github.deltatango.pgjson.model.TableDef
 */
@Slf4j
public class TableDefRepo {

    /** SQL template: insert a new schema definition into {@code tabledef}. */
    protected static final String INSERT_TABLEDEF;
    /** SQL template: select from a user table by JSON attribute containment. */
    protected static final String SELECT_BY_ATTRIBUTE;
    /** SQL template: select the latest {@code tabledef} row by UUID. */
    protected static final String SELECT_TABLE_DEF_BY_UUID;
    /** SQL template: simple connectivity test ({@code SELECT 1}). */
    protected static final String SELECT_FOR_TEST;
    /** SQL template: select the latest schema per {@code schema_name} (effective schemas). */
    protected static final String SELECT_ALL_EFFECTIVE_TABLE_DEF;
    /** SQL template: create a GIN index on a JSON key in a user table. */
    protected static final String CREATE_INDEX;
    /** SQL template: select all PgJson-managed indexes ({@code pgjson_*}) for a table. */
    protected static final String SELECT_INDEX;
    /** SQL template: select a {@code tabledef} row by primary key ID. */
    protected static final String SELECT_TABLE_DEF_BY_ID;
    /** SQL template: select the latest {@code tabledef} row by table name. */
    protected static final String SELECT_TABLEDEF_BY_TABLE_NAME;
    /** SQL template: select the latest {@code tabledef} row by table name and schema name. */
    protected static final String SELECT_TABLEDEF_BY_TABLE_NAME_AND_SCHEMA_NAME;

    private DateUtil dateUtil = new DateUtil();

    static {
        INSERT_TABLEDEF =
                "INSERT INTO public.tabledef (id_uuid, schema, schema_name, table_name, schema_timestamp, schema_hash) VALUES (?, ?, ?, ?, ?, ?);";

        SELECT_BY_ATTRIBUTE =
                "SELECT * FROM public.%s WHERE json_data @> '%s' ORDER BY id DESC LIMIT 1;";

        SELECT_TABLE_DEF_BY_UUID =
                "SELECT * FROM  public.tabledef WHERE id_uuid=? ORDER BY id DESC LIMIT 1;";

        SELECT_TABLE_DEF_BY_ID = "SELECT * FROM  public.tabledef WHERE id=?;";

        SELECT_TABLEDEF_BY_TABLE_NAME = "SELECT * FROM  public.tabledef WHERE table_name=? ORDER BY id DESC LIMIT 1;";

        SELECT_TABLEDEF_BY_TABLE_NAME_AND_SCHEMA_NAME = "SELECT * FROM  public.tabledef WHERE table_name=? AND schema_name=? ORDER BY id DESC LIMIT 1; ";

        SELECT_FOR_TEST = "SELECT 1;";

        SELECT_ALL_EFFECTIVE_TABLE_DEF =
                "SELECT DISTINCT ON (schema_name) * FROM public.tabledef ORDER BY schema_name, id DESC;";

        CREATE_INDEX = "CREATE INDEX IF NOT EXISTS %s ON %s USING GIN (((json_data -> '%s'::text)));";

        SELECT_INDEX =
                "SELECT indexname, indexdef FROM pg_indexes WHERE tablename = ?  AND indexname LIKE 'pgjson_%';;";
    }

    /**
     * Creates a GIN index on a JSON key in the specified user data table.
     *
     * @param connection open JDBC connection
     * @param indexName  the index name (e.g., {@code pgjson_exact_users_email})
     * @param tableName  the target data table
     * @param attribute  the JSON key to index
     * @return {@link OperationResult.Success} with the number of affected rows,
     *         or {@link OperationResult.Error} on failure
     */
    public OperationResult<Integer> createIndexOnJsonObject(
            Connection connection, String indexName, String tableName, String attribute) {
        String insertSql = String.format(CREATE_INDEX, indexName, tableName, attribute);
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {
            int rows = preparedStatement.executeUpdate();
            return OperationResult.success(rows);
        } catch (Exception exc) {
            log.error("Error in method createIndexOnJsonObject()", exc);
            return OperationResult.error("Failed to create index on JSON object", exc);
        }
    }

    /**
     * Retrieves all PgJson-managed indexes ({@code pgjson_*}) for the given table.
     *
     * @param connection open JDBC connection
     * @param tableName  the data table to query indexes for
     * @return {@link OperationResult.Success} with the list of {@link IndexInfo},
     *         or {@link OperationResult.Error} on failure
     */
    public OperationResult<List<IndexInfo>> selectAllIndexOfTable(Connection connection, String tableName) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_INDEX)) {
            preparedStatement.setString(1, tableName);
            return executeIndexListPreparedStatement(preparedStatement);
        } catch (Exception exc) {
            log.error("Error in method selectAllIndexOfTable()", exc);
            return OperationResult.error("Failed to select indexes for table: " + tableName, exc);
        }
    }

    private OperationResult<List<IndexInfo>> executeIndexListPreparedStatement(PreparedStatement preparedStatement) {
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            List<IndexInfo> indexInfoList = new ArrayList<>();
            while (resultSet.next()) {
                IndexInfo indexInfo = new IndexInfo();
                indexInfo.setIndexName(resultSet.getString(IndexInfo.COLUMN_INDEX_NAME));
                indexInfo.setIndexDef(resultSet.getString(IndexInfo.COLUMN_INDEX_DEF));
                indexInfoList.add(indexInfo);
            }
            return OperationResult.success(indexInfoList);
        } catch (Exception exc) {
            log.error("Error in method executeIndexListPreparedStatement()", exc);
            return OperationResult.error("Failed to execute index list query", exc);
        }
    }

    /**
     * Inserts a new schema definition into the {@code tabledef} table.
     *
     * @param connection open JDBC connection
     * @param uuid       unique UUID for this schema definition
     * @param tableName  the target data table name
     * @param schemaName human-readable schema name (e.g., "user-profile-v1")
     * @param schemaData the raw JSON Schema 2020-12 string
     * @param schemaHash SHA-256 hash of {@code schemaData} for cache invalidation
     * @return {@link OperationResult.Success} with the number of inserted rows,
     *         or {@link OperationResult.Error} on failure
     */
    public OperationResult<Integer> insertSchema(
            Connection connection,
            String uuid,
            String tableName,
            String schemaName,
            String schemaData,
            String schemaHash) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_TABLEDEF)) {
            LocalDateTime localDateTime = LocalDateTime.now();
            int index = 1;
            preparedStatement.setString(index++, uuid);
            preparedStatement.setString(index++, schemaData);
            preparedStatement.setString(index++, schemaName);
            preparedStatement.setString(index++, tableName);
            preparedStatement.setTimestamp(index++, dateUtil.localDateTimeToSqlTimestamp(localDateTime));
            preparedStatement.setString(index, schemaHash);
            int rows = preparedStatement.executeUpdate();
            return OperationResult.success(rows);
        } catch (Exception exc) {
            log.error("Error in method insertSchema()", exc);
            return OperationResult.error("Failed to insert schema", exc);
        }
    }

    /**
     * Retrieves the most recent schema definition matching the given UUID.
     *
     * @param connection   open JDBC connection
     * @param tableDefUuid the UUID to search for
     * @return {@link OperationResult.Success} with the {@link TableDef},
     *         {@link OperationResult.NotFound} if no match exists,
     *         or {@link OperationResult.Error} on failure
     */
    public OperationResult<TableDef> selectTableDefByUuid(Connection connection, String tableDefUuid) {
        if (tableDefUuid == null || tableDefUuid.trim().isEmpty()) {
            log.warn("TableDef UUID cannot be null or empty");
            return OperationResult.error("TableDef UUID cannot be null or empty");
        }

        try (PreparedStatement preparedStatement =
                     connection.prepareStatement(SELECT_TABLE_DEF_BY_UUID)) {
            preparedStatement.setString(1, tableDefUuid);
            return executeTableDefPrepStatement(preparedStatement);
        } catch (Exception exc) {
            log.error("Error in method selectTableDefByUuid()", exc);
            return OperationResult.error("Failed to select TableDef by UUID: " + tableDefUuid, exc);
        }
    }

    /**
     * Retrieves a schema definition by its auto-incremented primary key.
     *
     * @param connection open JDBC connection
     * @param tableDefId the primary key ID (must be positive)
     * @return {@link OperationResult.Success} with the {@link TableDef},
     *         {@link OperationResult.NotFound} if no match exists,
     *         or {@link OperationResult.Error} on failure
     */
    public OperationResult<TableDef> selectTableDefById(Connection connection, Integer tableDefId) {
        if (tableDefId == null || tableDefId <= 0) {
            log.warn("TableDef ID must be positive");
            return OperationResult.error("TableDef ID must be positive");
        }

        try (PreparedStatement preparedStatement =
                     connection.prepareStatement(SELECT_TABLE_DEF_BY_ID)) {
            preparedStatement.setInt(1, tableDefId);
            return executeTableDefPrepStatement(preparedStatement);
        } catch (Exception exc) {
            log.error("Error in method selectTableDefById()", exc);
            return OperationResult.error("Failed to select TableDef by ID: " + tableDefId, exc);
        }
    }

    /**
     * Retrieves the most recent schema definition for the given table name.
     *
     * @param connection open JDBC connection
     * @param tableName  the data table name
     * @return {@link OperationResult.Success} with the {@link TableDef},
     *         {@link OperationResult.NotFound} if no match exists,
     *         or {@link OperationResult.Error} on failure
     */
    public OperationResult<TableDef> selectTableDefByTableName(Connection connection, String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            log.warn("Table name cannot be null or empty");
            return OperationResult.error("Table name cannot be null or empty");
        }

        try (PreparedStatement preparedStatement =
                     connection.prepareStatement(SELECT_TABLEDEF_BY_TABLE_NAME)) {
            preparedStatement.setString(1, tableName);
            return executeTableDefPrepStatement(preparedStatement);
        } catch (Exception exc) {
            log.error("Error in method selectTableDefByTableName()", exc);
            return OperationResult.error("Failed to select TableDef by table name: " + tableName, exc);
        }
    }

    /**
     * Retrieves the most recent schema definition matching both table name and schema name.
     *
     * @param connection open JDBC connection
     * @param tableName  the data table name
     * @param schemaName the human-readable schema name
     * @return {@link OperationResult.Success} with the {@link TableDef},
     *         {@link OperationResult.NotFound} if no match exists,
     *         or {@link OperationResult.Error} on failure
     */
    public OperationResult<TableDef> selectTableDefByTableNameAndName(Connection connection, String tableName, String schemaName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            log.warn("Table name cannot be null or empty");
            return OperationResult.error("Table name cannot be null or empty");
        }
        if (schemaName == null || schemaName.trim().isEmpty()) {
            log.warn("Schema name cannot be null or empty");
            return OperationResult.error("Schema name cannot be null or empty");
        }

        try (PreparedStatement preparedStatement =
                     connection.prepareStatement(SELECT_TABLEDEF_BY_TABLE_NAME_AND_SCHEMA_NAME)) {
            preparedStatement.setString(1, tableName);
            preparedStatement.setString(2, schemaName);
            return executeTableDefPrepStatement(preparedStatement);
        } catch (Exception exc) {
            log.error("Error in method selectTableDefByTableNameAndName()", exc);
            return OperationResult.error("Failed to select TableDef by table name and schema name", exc);
        }
    }

    /**
     * Retrieves all effective (latest-per-schema-name) schema definitions.
     *
     * <p>Uses {@code DISTINCT ON (schema_name)} to return only the most recent
     * schema for each unique schema name.</p>
     *
     * @param connection open JDBC connection
     * @return {@link OperationResult.Success} with the list of effective {@link TableDef}s,
     *         or {@link OperationResult.Error} on failure
     */
    public OperationResult<List<TableDef>> selectAllEffectiveTableDef(Connection connection) {
        try (PreparedStatement preparedStatement =
                     connection.prepareStatement(SELECT_ALL_EFFECTIVE_TABLE_DEF)) {
            return executeTableDefListPrepStatement(preparedStatement);
        } catch (Exception exc) {
            log.error("Error in method selectAllEffectiveTableDef()", exc);
            return OperationResult.error("Failed to select all effective table definitions", exc);
        }
    }

    private TableDef extractTableDefResultSet(ResultSet resultSet) {
        try {
            TableDef tableDef = new TableDef();
            tableDef.setTableDefId(resultSet.getInt(TableDef.TABLE_DEF_ID_COLUMN));
            tableDef.setIdUuid(resultSet.getString(TableDef.ID_UUID_COLUMN));
            tableDef.setSchemaData(resultSet.getString(TableDef.SCHEMA_COLUMN));
            tableDef.setSchemaName(resultSet.getString(TableDef.SCHEMA_NAME_COLUMN));
            tableDef.setTableName(resultSet.getString(TableDef.TABLE_NAME_COLUMN));
            tableDef.setSchemaDate(
                    dateUtil.sqlTimestampToLocalDateTime(resultSet.getTimestamp(TableDef.SCHEMA_DATE_COLUMN)));
            tableDef.setSchemaHash(resultSet.getString(TableDef.SCHEMA_HASH_COLUMN));
            return tableDef;
        } catch (Exception exc) {
            log.error("Error in method extractTableDefResultSet()", exc);
            return null;
        }
    }

    private OperationResult<List<TableDef>> executeTableDefListPrepStatement(PreparedStatement preparedStatement) {
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            List<TableDef> tableDefList = new ArrayList<>();
            while (resultSet.next()) {
                tableDefList.add(extractTableDefResultSet(resultSet));
            }
            return OperationResult.success(tableDefList);
        } catch (Exception exc) {
            log.error("Error in method executeTableDefListPrepStatement()", exc);
            return OperationResult.error("Failed to execute TableDef list query", exc);
        }
    }

    private OperationResult<TableDef> executeTableDefPrepStatement(PreparedStatement preparedStatement) {
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            log.debug("TableDef resultSet size: {}", resultSet.getFetchSize());
            if (resultSet.next()) {
                TableDef tableDef = extractTableDefResultSet(resultSet);
                if (tableDef != null) {
                    return OperationResult.success(tableDef);
                }
                return OperationResult.error("Failed to extract TableDef from result set");
            } else {
                log.debug("TableDef result set was empty, returning NotFound");
                return OperationResult.notFound("TableDef not found");
            }
        } catch (Exception exc) {
            log.error("Error in method executeTableDefPrepStatement()", exc);
            return OperationResult.error("Failed to execute TableDef query", exc);
        }
    }

    /**
     * Executes a simple {@code SELECT 1} query to verify database connectivity.
     *
     * @param connection open JDBC connection
     * @return {@link OperationResult.Success} with {@code true} if the query succeeds,
     *         or {@link OperationResult.Error} on failure
     */
    public OperationResult<Boolean> selectTestFromTableDef(Connection connection) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_FOR_TEST)) {
            boolean result = preparedStatement.execute();
            return OperationResult.success(result);
        } catch (Exception exc) {
            log.error("Error in method selectTestFromTableDef()", exc);
            return OperationResult.error("Failed to execute test query", exc);
        }
    }
}
