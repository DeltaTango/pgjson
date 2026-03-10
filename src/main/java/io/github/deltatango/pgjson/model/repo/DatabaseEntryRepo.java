package io.github.deltatango.pgjson.model.repo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import io.github.deltatango.pgjson.model.DatabaseEntry;
import io.github.deltatango.pgjson.model.IndexInfo;
import io.github.deltatango.pgjson.model.enums.IndexType;
import io.github.deltatango.pgjson.model.enums.LogicalOperator;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import io.github.deltatango.pgjson.model.operations.PagedResult;
import io.github.deltatango.pgjson.util.DateUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Data access layer for user-defined JSON data tables.
 *
 * <p>This repository handles CRUD and search operations on the dynamically-created
 * data tables that store JSON documents. All SQL identifiers (table names, key names)
 * are validated against {@link #SAFE_SQL_IDENTIFIER} to prevent SQL injection, and
 * all query values are bound via {@link java.sql.PreparedStatement} parameters.</p>
 *
 * <p>Search queries are built dynamically from {@link SearchTerm} objects, which carry
 * parameterized SQL fragments with their bound values.</p>
 *
 * @see io.github.deltatango.pgjson.model.DatabaseEntry
 * @see SearchTerm
 */
@Slf4j
public class DatabaseEntryRepo {

    /** SQL template: insert a JSON document with {@code RETURNING *}. */
    protected static final String INSERT_DATA;
    /** SQL template: select all rows with ordering, limit, and offset. */
    protected static final String SELECT_ALL;
    /** SQL template: select rows matching a search term (no limit). */
    protected static final String SELECT_ALL_BY_TERM;
    /** SQL template: select rows matching a search term with limit and offset. */
    protected static final String SELECT_ALL_BY_TERM_WITH_LIMIT;
    /** SQL template: select a single row by its {@code id_uuid}. */
    protected static final String SELECT_BY_IDUUID;
    /** SQL template: update JSON data by {@code id_uuid} with {@code RETURNING *}. */
    protected static final String UPDATE_BY_UUID;
    /** SQL template: remove an element from a JSON array by index with {@code RETURNING *}. */
    protected static final String DELETE_ARRAY_ELEMENT;
    /** SQL template: delete a row by {@code id_uuid}. */
    protected static final String DELETE_RECORD;
    /** SQL template: count all rows in a table. */
    protected static final String COUNT_ALL;
    /** SQL template: count rows matching a search term. */
    protected static final String COUNT_BY_TERM;

    private DateUtil dateUtil = new DateUtil();

    /**
     * Pattern for validating SQL identifiers (table names, column/key names).
     * Only allows alphanumeric characters and underscores, starting with a letter or underscore.
     */
    private static final Pattern SAFE_SQL_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    static {
        INSERT_DATA =
                "INSERT INTO public.%s (tabledef_id, json_data, id_uuid, data_created, data_changed) VALUES (?, ?::JSON, ?, ?, ?) RETURNING *;";

        SELECT_ALL = "SELECT * FROM public.%s ORDER BY id %s LIMIT ? OFFSET ?;";

        SELECT_ALL_BY_TERM = "SELECT * FROM public.%s WHERE %s;";

        SELECT_ALL_BY_TERM_WITH_LIMIT = "SELECT * FROM public.%s WHERE %s ORDER BY id %s LIMIT ? OFFSET ?;";

        SELECT_BY_IDUUID = "SELECT * FROM public.%s WHERE id_uuid=?;";

        UPDATE_BY_UUID = "UPDATE public.%s SET json_data=?::JSON, data_changed=NOW()::timestamp WHERE id_uuid=? RETURNING *;";

        DELETE_ARRAY_ELEMENT =
                "UPDATE public.%s"
                        + " SET json_data = jsonb_set(json_data, '{%s}', (json_data->'%s') - ?), data_changed=NOW()::timestamp"
                        + " WHERE id_uuid=?"
                        + " RETURNING *;";

        DELETE_RECORD = "DELETE FROM public.%s WHERE id_uuid=?";

        COUNT_ALL = "SELECT COUNT(*) FROM public.%s;";

        COUNT_BY_TERM = "SELECT COUNT(*) FROM public.%s WHERE %s;";
    }

    /**
     * Validates that a string is a safe SQL identifier (table name, column name, key name).
     * Only allows alphanumeric characters and underscores, starting with a letter or underscore.
     *
     * @param identifier the identifier to validate
     * @throws IllegalArgumentException if the identifier is null or contains unsafe characters
     */
    static void validateSqlIdentifier(String identifier) {
        if (identifier == null || !SAFE_SQL_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException(
                    "Invalid SQL identifier: '" + identifier + "'. Must match pattern: " + SAFE_SQL_IDENTIFIER.pattern());
        }
    }

    /**
     * Inner class for carrying parameterized SQL fragments with their bound parameter values.
     * Used to build search queries safely without SQL injection.
     */
    private static class SqlFragment {
        final String sql;
        final List<Object> params;

        SqlFragment(String sql, Object... params) {
            this.sql = sql;
            this.params = new ArrayList<>(Arrays.asList(params));
        }

        SqlFragment(String sql, List<Object> params) {
            this.sql = sql;
            this.params = new ArrayList<>(params);
        }
    }

    /**
     * Deletes a database entry by its UUID.
     *
     * @param connection open JDBC connection
     * @param tableName  the data table (validated as a safe SQL identifier)
     * @param idUuid     the UUID of the entry to delete
     * @return {@link OperationResult.Success} with the number of deleted rows,
     *         or {@link OperationResult.Error} on failure
     * @throws IllegalArgumentException if {@code tableName} is not a safe SQL identifier
     */
    public OperationResult<Integer> deleteRecord(Connection connection, String tableName, String idUuid) {
        validateSqlIdentifier(tableName);
        String queryString = String.format(DELETE_RECORD, tableName);
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            preparedStatement.setString(1, idUuid);
            int rows = preparedStatement.executeUpdate();
            return OperationResult.success(rows);
        } catch (Exception exc) {
            log.error("Error in method deleteRecord()", exc);
            return OperationResult.error("Failed to delete record", exc);
        }
    }

    /**
     * Searches for JSON documents matching the given criteria.
     *
     * <p>If {@code searchObject} is {@code null}, returns all rows with the given limit/offset.
     * Otherwise, builds a parameterized search term from the search object and available indexes,
     * then executes the query.</p>
     *
     * @param connection      open JDBC connection
     * @param tableName       the data table (validated as a safe SQL identifier)
     * @param order           sort order ({@code "asc"} or {@code "desc"})
     * @param limit           maximum rows to return
     * @param offset          number of rows to skip
     * @param searchObject    parsed search term JSON, or {@code null} for unfiltered
     * @param indexes         available GIN indexes for the table
     * @param logicalOperator how to combine multiple search terms ({@code AND} or {@code OR})
     * @return {@link OperationResult.Success} with the matching entries,
     *         or {@link OperationResult.Error} on failure
     * @throws IllegalArgumentException if {@code tableName} is not a safe SQL identifier
     */
    public OperationResult<List<DatabaseEntry>> searchData(
            Connection connection,
            String tableName,
            String order,
            Integer limit,
            Integer offset,
            JsonElement searchObject,
            List<IndexInfo> indexes,
            LogicalOperator logicalOperator) {
        try {
            validateSqlIdentifier(tableName);
            log.trace("searchData(), searchObject: {}", searchObject);
            if (searchObject == null) {
                return OperationResult.success(select(connection, tableName, limit, offset, order));
            } else {
                OperationResult<SearchTerm> termResult = createTerm(searchObject.getAsJsonObject(), indexes, limit, logicalOperator, tableName);
                if (termResult instanceof OperationResult.Error<SearchTerm> err) {
                    return OperationResult.error(err.message(), err.cause());
                }
                SearchTerm term = ((OperationResult.Success<SearchTerm>) termResult).value();
                log.trace("finalTerm: {}", term.getSearchTerm());
                return OperationResult.success(select(connection, tableName, term.getLimit(), offset, term.getSearchTerm(), term.getParameters(), order));
            }
        } catch (IllegalArgumentException exc) {
            throw exc;
        } catch (Exception exc) {
            log.error("Error in method searchData()", exc);
            return OperationResult.error("Search operation failed", exc);
        }
    }

    /**
     * Searches for JSON documents and returns a paginated result with total count.
     *
     * <p>Executes both a {@code COUNT(*)} query and a data query using the same filters,
     * returning the results wrapped in a {@link PagedResult}.</p>
     *
     * @param connection      open JDBC connection
     * @param tableName       the data table (validated as a safe SQL identifier)
     * @param order           sort order ({@code "asc"} or {@code "desc"})
     * @param limit           maximum rows to return
     * @param offset          number of rows to skip
     * @param searchObject    parsed search term JSON, or {@code null} for unfiltered
     * @param indexes         available GIN indexes for the table
     * @param logicalOperator how to combine multiple search terms ({@code AND} or {@code OR})
     * @return {@link OperationResult.Success} with the {@link PagedResult},
     *         or {@link OperationResult.Error} on failure
     * @throws IllegalArgumentException if {@code tableName} is not a safe SQL identifier
     */
    public OperationResult<PagedResult<DatabaseEntry>> searchDataWithCount(
            Connection connection,
            String tableName,
            String order,
            Integer limit,
            Integer offset,
            JsonElement searchObject,
            List<IndexInfo> indexes,
            LogicalOperator logicalOperator) {
        try {
            validateSqlIdentifier(tableName);
            List<DatabaseEntry> items;
            long totalCount;

            if (searchObject == null) {
                items = select(connection, tableName, limit, offset, order);
                totalCount = countRows(connection, tableName, null, null);
            } else {
                OperationResult<SearchTerm> termResult = createTerm(searchObject.getAsJsonObject(), indexes, limit, logicalOperator, tableName);
                if (termResult instanceof OperationResult.Error<SearchTerm> err) {
                    return OperationResult.error(err.message(), err.cause());
                }
                SearchTerm term = ((OperationResult.Success<SearchTerm>) termResult).value();
                items = select(connection, tableName, term.getLimit(), offset, term.getSearchTerm(), term.getParameters(), order);
                totalCount = countRows(connection, tableName, term.getSearchTerm(), term.getParameters());
            }

            PagedResult<DatabaseEntry> pagedResult = new PagedResult<>(items, totalCount, limit, offset);
            return OperationResult.success(pagedResult);
        } catch (IllegalArgumentException exc) {
            throw exc;
        } catch (Exception exc) {
            log.error("Error in method searchDataWithCount()", exc);
            return OperationResult.error("Search with count operation failed", exc);
        }
    }

    /**
     * Counts the total number of rows matching the given search term, or all rows if no term is provided.
     *
     * @param connection open JDBC connection
     * @param tableName  the data table (already validated)
     * @param term       the SQL WHERE clause fragment, or {@code null} for unfiltered
     * @param parameters the bound parameters for the term, or {@code null}
     * @return the total row count
     */
    private long countRows(Connection connection, String tableName, String term, List<Object> parameters) {
        String queryString;
        if (term == null) {
            queryString = String.format(COUNT_ALL, tableName);
        } else {
            queryString = String.format(COUNT_BY_TERM, tableName, term);
        }

        try (PreparedStatement ps = connection.prepareStatement(queryString)) {
            if (parameters != null) {
                int paramIndex = 1;
                for (Object param : parameters) {
                    if (param instanceof String) {
                        ps.setString(paramIndex++, (String) param);
                    } else {
                        ps.setObject(paramIndex++, param);
                    }
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (Exception exc) {
            log.error("Error in method countRows()", exc);
        }
        return 0L;
    }

    /**
     * Builds a parameterized {@link SearchTerm} from a parsed search JSON object.
     *
     * <p>Iterates over the keys in {@code searchTerm}, matches each to an available index
     * (or infers the index type from the value structure), and produces SQL fragments with
     * bound parameters. Control fields ({@code logicalOperator}, {@code limit}, {@code offset},
     * {@code orderType}) are skipped.</p>
     *
     * @param searchTerm      the parsed search term JSON object
     * @param indexes         available GIN indexes for the table
     * @param limit           maximum rows to return
     * @param logicalOperator how to combine multiple terms
     * @return {@link OperationResult.Success} with the built {@link SearchTerm},
     *         or {@link OperationResult.Error} if no valid terms could be generated
     * @throws IllegalArgumentException if any search key is not a safe SQL identifier
     */
    public OperationResult<SearchTerm> createTerm(JsonObject searchTerm, List<IndexInfo> indexes, Integer limit, LogicalOperator logicalOperator, String tableName) {
        try {
            validateSqlIdentifier(tableName);
            log.debug("Logical operator: {}", logicalOperator);
            log.trace("Indexes: {}", indexes);

            List<SqlFragment> fragments = new ArrayList<>();
            Integer adjustedLimit = limit;

            for (String key : searchTerm.keySet()) {
                // Skip control fields that are not actual search terms
                if (isControlField(key)) {
                    continue;
                }

                // Validate key is a safe SQL identifier to prevent injection
                validateSqlIdentifier(key);

                IndexInfo indexInfo = findIndexTypeForKey(indexes, key);
                if (indexInfo == null) {
                    // Fallback: infer index type from value structure
                    indexInfo = inferIndexTypeFromValue(key, searchTerm.get(key));
                    if (indexInfo != null) {
                        logMissingIndexWarning(indexInfo, tableName);
                    } else {
                        continue; // Skip if inference fails
                    }
                }

                log.trace("Processing key: {} with indexInfo: {}", key, indexInfo);
                SqlFragment fragment = createTermForIndexType(key, searchTerm.get(key), indexInfo);

                if (fragment != null) {
                    fragments.add(fragment);
                    // Adjust limit for array queries
                    if (indexInfo.getIndexType() == IndexType.object && isArrayQuery(searchTerm.get(key))) {
                        adjustedLimit = null;
                        log.debug("Array query detected, setting limit to null");
                    }
                }
            }

            return OperationResult.success(buildFinalSearchTerm(fragments, adjustedLimit, logicalOperator));
        } catch (IllegalArgumentException exc) {
            throw exc;
        } catch (PostgreJsonException exc) {
            return OperationResult.error(exc.getMessage(), exc);
        } catch (Exception exc) {
            log.error("Error in method createTerm()", exc);
            return OperationResult.error("Failed to create search term", exc);
        }
    }

    private SqlFragment createTermForIndexType(String key, JsonElement value, IndexInfo indexInfo) {
        switch (indexInfo.getIndexType()) {
            case exact:
                return createExactTerm(key, value);
            case object:
                return createObjectTerm(key, value);
            case nestedobject:
                return createNestedObjectTerm(key, value);
            case fts:
                return createFtsTerm(key, value);
            default:
                log.warn("Unknown index type: {}", indexInfo.getIndexType());
                return null;
        }
    }

    private SqlFragment createExactTerm(String key, JsonElement value) {
        String sql = String.format("json_data->>'%s'=?", key);
        return new SqlFragment(sql, value.getAsString());
    }

    private SqlFragment createObjectTerm(String key, JsonElement value) {
        log.trace("Creating object term for key: {}, value: {}", key, value.toString());
        List<JsonElement> arrayElements = extractJsonArrayElements(value);

        if (arrayElements.size() > 1) {
            // Build parameterized array query with individual ?::jsonb placeholders
            List<Object> params = new ArrayList<>();
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < arrayElements.size(); i++) {
                if (i > 0) {
                    placeholders.append(", ");
                }
                placeholders.append("?::jsonb");
                params.add("[" + arrayElements.get(i).toString() + "]");
            }
            String sql = String.format("json_data->'%s' @> ANY (ARRAY[%s])", key, placeholders);
            return new SqlFragment(sql, params);
        } else {
            String sql = String.format("json_data->'%s' @> ?::jsonb", key);
            return new SqlFragment(sql, value.toString());
        }
    }

    private SqlFragment createNestedObjectTerm(String key, JsonElement value) {
        try {
            JsonObject nestedObject = value.getAsJsonObject();
            if (nestedObject == null || nestedObject.isJsonNull()) {
                return null;
            }

            log.debug("Key: {} has nested object: {}", key, nestedObject.keySet());
            String nestedKey = nestedObject.keySet().iterator().next();
            // Validate nested key to prevent injection
            validateSqlIdentifier(nestedKey);
            log.debug("First key from {} is {}", key, nestedKey);

            String sql = String.format("json_data->'%s'->'%s' @> ?::jsonb", key, nestedKey);
            return new SqlFragment(sql, nestedObject.get(nestedKey).toString());
        } catch (ClassCastException exc) {
            log.trace("Failed to cast to JsonObject for key: {}", key, exc);
            return null;
        }
    }

    private SqlFragment createFtsTerm(String key, JsonElement value) {
        if (value.isJsonPrimitive()) {
            log.debug("IndexType.fts key: '{}', term: '{}'", key, value.getAsString());
            String sql = String.format("to_tsvector('simple', json_data->'%s') @@ to_tsquery('simple', ?)", key);
            return new SqlFragment(sql, value.getAsString());
        }
        return null;
    }

    private boolean isArrayQuery(JsonElement value) {
        List<JsonElement> elements = extractJsonArrayElements(value);
        return elements.size() > 1;
    }

    private SearchTerm buildFinalSearchTerm(List<SqlFragment> fragments, Integer limit, LogicalOperator logicalOperator)
            throws PostgreJsonException {
        log.debug("Logical operator property name: {}", logicalOperator.getPropertyName());

        if (fragments.isEmpty()) {
            throw new PostgreJsonException("Could not generate search term");
        }

        // Combine all SQL fragments and their parameters
        List<Object> allParams = new ArrayList<>();
        List<String> sqlParts = new ArrayList<>();
        for (SqlFragment fragment : fragments) {
            sqlParts.add(fragment.sql);
            allParams.addAll(fragment.params);
        }

        if (sqlParts.size() == 1) {
            log.debug("Single term generated");
            return new SearchTerm(sqlParts.get(0), limit, allParams);
        }

        String separator = String.format(" %s ", logicalOperator.getPropertyName());
        String finalTerm = String.join(separator, sqlParts);
        log.debug("Final term: {}", finalTerm);
        return new SearchTerm(finalTerm, limit, allParams);
    }

    /**
     * Extracts elements from a JSON array value. Returns an empty list if the value is not an array.
     */
    private List<JsonElement> extractJsonArrayElements(JsonElement objectTerms) {
        try {
            JsonArray array = objectTerms.getAsJsonArray();
            List<JsonElement> elements = new ArrayList<>();
            for (JsonElement element : array) {
                log.trace("single-element: {}", element.toString());
                elements.add(element);
            }
            return elements;
        } catch (Exception exc) {
            return new ArrayList<>();
        }
    }

    private IndexInfo findIndexTypeForKey(List<IndexInfo> indexes, String key) {
        log.trace("searching for: {}", key);
        for (IndexInfo indexInfo : indexes) {
            String indexName = indexInfo.getIndexName();
            List<String> nameList = new ArrayList<>(Arrays.asList(indexName.split("_")));
            if (!nameList.contains(key.toLowerCase())) {
                continue;
            }
            log.trace("nameList: {}", nameList);
            nameList.remove(0);
            log.trace("nameList.get(0) : {}", nameList.get(0));
            indexInfo.setIndexType(nameList.get(0));
            nameList.remove(0);
            if (nameList.size() > 1) {
                indexInfo.setKeyPath(String.join(".", nameList));
            } else {
                indexInfo.setKeyPath(nameList.get(0));
            }
            return indexInfo;
        }
        return null;
    }

    private boolean isControlField(String key) {
        // Control fields that should not be treated as search terms
        return "logicalOperator".equals(key)
               || "limit".equals(key)
               || "offset".equals(key)
               || "orderType".equals(key);
    }

    private IndexInfo inferIndexTypeFromValue(String key, JsonElement value) {
        if (value.isJsonPrimitive()) {
            String stringValue = value.getAsString();
            // Multi-word strings: use FTS for better search experience
            if (stringValue != null && stringValue.trim().split("\\s+").length > 1) {
                log.debug("Inferring FTS query type for multi-word field: {}", key);
                return createFallbackIndexInfo(key, IndexType.fts);
            } else {
                // Single values: exact match
                log.debug("Inferring exact query type for field: {}", key);
                return createFallbackIndexInfo(key, IndexType.exact);
            }
        } else if (value.isJsonObject() || value.isJsonArray()) {
            // Complex structures: object containment
            log.debug("Inferring object query type for complex field: {}", key);
            return createFallbackIndexInfo(key, IndexType.object);
        }
        log.warn("Could not infer query type for field: {} with value type: {}",
            key, value.getClass().getSimpleName());
        return null;
    }

    private IndexInfo createFallbackIndexInfo(String key, IndexType indexType) {
        IndexInfo info = new IndexInfo();
        info.setIndexName(String.format("fallback_%s_%s", indexType.name(), key));
        info.setIndexType(indexType.name());
        info.setKeyPath(key);
        return info;
    }

    private void logMissingIndexWarning(IndexInfo indexInfo, String tableName) {
        String key = indexInfo.getKeyPath();
        IndexType indexType = indexInfo.getIndexType();

        String indexName = String.format("pgjson_%s_%s_%s",
            indexType.name().toLowerCase(),
            tableName.toLowerCase(),
            key.toLowerCase().replace(".", "_"));

        String createIndexSql = generateCreateIndexSql(indexName, tableName, key, indexType);

        log.warn("\n"
            + "================================================================================\n"
            + "PERFORMANCE WARNING: Missing Index\n"
            + "================================================================================\n"
            + "Table: {}\n"
            + "Field: {}\n"
            + "Query Type: {}\n"
            + "Impact: Using FULL TABLE SCAN (slow for large datasets)\n"
            + "\n"
            + "To improve performance, create this index:\n"
            + "{}\n"
            + "================================================================================",
            tableName, key, indexType, createIndexSql);
    }

    private String generateCreateIndexSql(String indexName, String tableName, String key, IndexType type) {
        switch (type) {
            case exact:
                return String.format("CREATE INDEX %s ON %s ((json_data ->>'%s'));",
                    indexName, tableName, key);

            case fts:
                return String.format("CREATE INDEX %s ON %s USING gin(to_tsvector('simple', json_data->'%s'));",
                    indexName, tableName, key);

            case object:
                return String.format("CREATE INDEX %s ON %s USING gin((json_data->'%s') jsonb_path_ops);",
                    indexName, tableName, key);

            case nestedobject:
                // For nested objects, handle dot notation in key path
                String[] parts = key.split("\\.");
                StringBuilder pathBuilder = new StringBuilder("json_data");
                for (String part : parts) {
                    pathBuilder.append("->'").append(part).append("'");
                }
                return String.format("CREATE INDEX %s ON %s USING gin((%s) jsonb_path_ops);",
                    indexName, tableName, pathBuilder.toString());

            default:
                return "-- Unknown index type: " + type;
        }
    }

    /**
     * Executes a parameterized search query with search term parameters bound safely.
     */
    private List<DatabaseEntry> select(
            Connection connection,
            String tableName,
            Integer limit,
            Integer offset,
            String term,
            List<Object> termParameters,
            String order) {

        validateSqlIdentifier(tableName);
        String queryString;
        if (limit != null) {
            queryString = String.format(SELECT_ALL_BY_TERM_WITH_LIMIT, tableName, term, order);
            log.debug("Limit is not null, selecting with limit ({}) and offset ({})", limit, offset);
        } else {
            queryString = String.format(SELECT_ALL_BY_TERM, tableName, term);
            log.debug("Limit is null, selecting without limit and offset");
        }
        log.debug("queryString: {}", queryString);
        long startTime = System.currentTimeMillis();
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            long timeOfStatementPrepare = System.currentTimeMillis();
            long durationOfStatementPrepare = timeOfStatementPrepare - startTime;
            log.debug("Duration of connection.prepareStatement: {} ms", durationOfStatementPrepare);

            int paramIndex = 1;
            // Bind search term parameters first
            for (Object param : termParameters) {
                if (param instanceof String) {
                    preparedStatement.setString(paramIndex++, (String) param);
                } else {
                    preparedStatement.setObject(paramIndex++, param);
                }
            }
            // Then bind limit/offset
            if (limit != null) {
                preparedStatement.setInt(paramIndex++, limit);
                preparedStatement.setInt(paramIndex, offset);
            }
            return executeDatabaseEntryListPrepStatement(preparedStatement, limit);
        } catch (Exception exc) {
            log.error("Error in method select(SELECT_ALL_BY_TERM)", exc);
            return new ArrayList<>();
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.debug("Duration of select(SELECT_ALL_BY_TERM): {} ms", duration);
        }
    }

    private List<DatabaseEntry> select(
            Connection connection, String tableName, Integer limit, Integer offset, String order) {
        validateSqlIdentifier(tableName);
        String queryString = String.format(SELECT_ALL, tableName, order);
        log.debug("queryString: {}", queryString);
        long startTime = System.currentTimeMillis();
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            int index = 1;
            preparedStatement.setInt(index++, limit);
            preparedStatement.setInt(index, offset);
            return executeDatabaseEntryListPrepStatement(preparedStatement, limit);
        } catch (Exception exc) {
            log.error("Error in method select(SELECT_ALL)", exc);
            return new ArrayList<>();
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.debug("Duration of select(SELECT_ALL): {} ms", duration);
        }
    }

    private List<DatabaseEntry> executeDatabaseEntryListPrepStatement(
            PreparedStatement preparedStatement, Integer fetchSize) throws SQLException {
        long startTime = System.currentTimeMillis();
        if (fetchSize != null) {
            preparedStatement.setFetchSize(fetchSize);
        }
        try (ResultSet resultSet = preparedStatement.executeQuery()) {

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.debug("Duration of executeDatabaseEntryListPrepStatement (query to DB only): {} ms", duration);

            List<DatabaseEntry> databaseEntryList = new ArrayList<>();

            while (resultSet.next()) {
                databaseEntryList.add(extractDatabaseEntryResultSet(resultSet));
            }
            return databaseEntryList;
        } catch (Exception exc) {
            log.error("Error in method executeDatabaseEntryListPrepStatement()", exc);
            return new ArrayList<>();
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.debug("Duration of executeDatabaseEntryListPrepStatement (including query to DB and fetch): {} ms", duration);
        }
    }

    private DatabaseEntry extractDatabaseEntryResultSet(ResultSet resultSet) throws SQLException {
        long startTime = System.currentTimeMillis();
        try {
            DatabaseEntry dbEntry = new DatabaseEntry();
            dbEntry.setEntryDbId(resultSet.getInt(DatabaseEntry.ENTRY_DB_ID_COLUMN));
            dbEntry.setSchemaDbId(resultSet.getInt(DatabaseEntry.SCHEMA_DB_ID_COLUMN));
            dbEntry.setDataCreatedDateTime(
                    dateUtil.sqlTimestampToLocalDateTime(
                            resultSet.getTimestamp(DatabaseEntry.DATA_CREATED_DATE_TIME_COLUMN)));
            dbEntry.setDataChangedDateTime(
                    dateUtil.sqlTimestampToLocalDateTime(
                            resultSet.getTimestamp(DatabaseEntry.DATA_CHANGED_DATE_TIME_COLUMN)));
            dbEntry.setEntryIdUuid(resultSet.getString(DatabaseEntry.ENTRY_ID_UUID_COLUMN));
            dbEntry.setJsonData(resultSet.getString(DatabaseEntry.JSON_DATA_COLUMN));
            return dbEntry;
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.trace("Duration of extractDatabaseEntryResultSet: {} ms", duration);
        }
    }

    private OperationResult<DatabaseEntry> extractDbEntryOperationResult(PreparedStatement preparedStatement) {
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            if (resultSet.next()) {
                DatabaseEntry entry = extractDatabaseEntryResultSet(resultSet);
                return OperationResult.success(entry);
            }
            log.debug("DatabaseEntry result set was empty, returning NotFound");
            return OperationResult.notFound("Database entry not found");
        } catch (Exception exc) {
            log.error("Error in method extractDbEntryOperationResult()", exc);
            return OperationResult.error("Failed to extract database entry", exc);
        }
    }

    /**
     * Retrieves a single database entry by its UUID.
     *
     * @param connection open JDBC connection
     * @param tableName  the data table (validated as a safe SQL identifier)
     * @param idUuid     the UUID of the entry to retrieve
     * @return {@link OperationResult.Success} with the {@link DatabaseEntry},
     *         {@link OperationResult.NotFound} if no match exists,
     *         or {@link OperationResult.Error} on failure
     * @throws IllegalArgumentException if {@code tableName} is not a safe SQL identifier
     */
    public OperationResult<DatabaseEntry> selectDataByIdUuid(Connection connection, String tableName, String idUuid) {
        if (tableName == null || tableName.trim().isEmpty()) {
            log.warn("Table name cannot be null or empty");
            return OperationResult.error("Table name cannot be null or empty");
        }
        if (idUuid == null || idUuid.trim().isEmpty()) {
            log.warn("ID UUID cannot be null or empty");
            return OperationResult.error("ID UUID cannot be null or empty");
        }

        validateSqlIdentifier(tableName);
        String queryString = String.format(SELECT_BY_IDUUID, tableName);
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            preparedStatement.setString(1, idUuid);
            return extractDbEntryOperationResult(preparedStatement);
        } catch (Exception exc) {
            log.error("Error in method selectDataByIdUuid()", exc);
            return OperationResult.error("Failed to select data by ID UUID", exc);
        }
    }

    /**
     * Inserts a new JSON document into the specified data table.
     *
     * <p>Uses {@code INSERT ... RETURNING *} to return the full inserted row.</p>
     *
     * @param connection  open JDBC connection (should have auto-commit disabled for transactional use)
     * @param entryIdUuid UUID for the new entry
     * @param tableName   the data table (validated as a safe SQL identifier)
     * @param jsonData    the JSON document to insert
     * @param tableDefId  the foreign key referencing the schema definition in {@code tabledef}
     * @return {@link OperationResult.Success} with the inserted {@link DatabaseEntry},
     *         or {@link OperationResult.Error} on failure
     * @throws IllegalArgumentException if {@code tableName} is not a safe SQL identifier
     */
    public OperationResult<DatabaseEntry> insertData(Connection connection, String entryIdUuid, String tableName, String jsonData, Integer tableDefId) {
        if (tableName == null || tableName.trim().isEmpty()) {
            log.warn("Table name cannot be null or empty");
            return OperationResult.error("Table name cannot be null or empty");
        }
        if (entryIdUuid == null || entryIdUuid.trim().isEmpty()) {
            log.warn("Entry ID UUID cannot be null or empty");
            return OperationResult.error("Entry ID UUID cannot be null or empty");
        }
        if (jsonData == null || jsonData.trim().isEmpty()) {
            log.warn("JSON data cannot be null or empty");
            return OperationResult.error("JSON data cannot be null or empty");
        }
        if (tableDefId == null || tableDefId <= 0) {
            log.warn("Table definition ID must be positive");
            return OperationResult.error("Table definition ID must be positive");
        }

        validateSqlIdentifier(tableName);
        String queryString = String.format(INSERT_DATA, tableName);
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            LocalDateTime localDateTime = LocalDateTime.now();
            String cleanedData = jsonData.replaceAll("\\r\\n", "");

            int index = 1;
            preparedStatement.setInt(index++, tableDefId);
            preparedStatement.setObject(index++, cleanedData);
            preparedStatement.setString(index++, entryIdUuid);
            preparedStatement.setTimestamp(index++, dateUtil.localDateTimeToSqlTimestamp(localDateTime));
            preparedStatement.setNull(index, Types.TIMESTAMP);

            return extractDbEntryOperationResult(preparedStatement);
        } catch (Exception exc) {
            log.error("Error in method insertData()", exc);
            return OperationResult.error("Failed to insert data", exc);
        }
    }

    /**
     * Replaces the JSON data of an existing entry identified by UUID.
     *
     * <p>Uses {@code UPDATE ... RETURNING *} to return the updated row.
     * The {@code data_changed} timestamp is set to {@code NOW()}.</p>
     *
     * @param connection  open JDBC connection (should have auto-commit disabled for transactional use)
     * @param tableName   the data table (validated as a safe SQL identifier)
     * @param jsonData    the new JSON document
     * @param entryIdUuid the UUID of the entry to update
     * @return {@link OperationResult.Success} with the updated {@link DatabaseEntry},
     *         {@link OperationResult.NotFound} if no match exists,
     *         or {@link OperationResult.Error} on failure
     * @throws IllegalArgumentException if {@code tableName} is not a safe SQL identifier
     */
    public OperationResult<DatabaseEntry> updateData(Connection connection, String tableName, String jsonData, String entryIdUuid) {
        if (tableName == null || tableName.trim().isEmpty()) {
            log.warn("Table name cannot be null or empty");
            return OperationResult.error("Table name cannot be null or empty");
        }
        if (entryIdUuid == null || entryIdUuid.trim().isEmpty()) {
            log.warn("Entry ID UUID cannot be null or empty");
            return OperationResult.error("Entry ID UUID cannot be null or empty");
        }
        if (jsonData == null || jsonData.trim().isEmpty()) {
            log.warn("JSON data cannot be null or empty");
            return OperationResult.error("JSON data cannot be null or empty");
        }

        validateSqlIdentifier(tableName);
        String queryString = String.format(UPDATE_BY_UUID, tableName);
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            String cleanedData = jsonData.replaceAll("\\r\\n", "");
            preparedStatement.setObject(1, cleanedData);
            preparedStatement.setString(2, entryIdUuid);

            return extractDbEntryOperationResult(preparedStatement);
        } catch (Exception exc) {
            log.error("Error in method updateData()", exc);
            return OperationResult.error("Failed to update data", exc);
        }
    }

    /**
     * Removes an element from a JSON array within an existing entry.
     *
     * <p>Uses PostgreSQL's {@code jsonb_set} with the {@code -} operator to remove the
     * element at the given index from the array at the specified key path.
     * Returns the updated row via {@code RETURNING *}.</p>
     *
     * @param connection  open JDBC connection (should have auto-commit disabled for transactional use)
     * @param entryIdUuid the UUID of the entry containing the array
     * @param tableName   the data table (validated as a safe SQL identifier)
     * @param key         the JSON key path of the array (validated as a safe SQL identifier)
     * @param index       the zero-based index of the element to remove (must be non-negative)
     * @return {@link OperationResult.Success} with the updated {@link DatabaseEntry},
     *         {@link OperationResult.NotFound} if no match exists,
     *         or {@link OperationResult.Error} on failure
     * @throws IllegalArgumentException if {@code tableName} or {@code key} is not a safe SQL identifier
     */
    public OperationResult<DatabaseEntry> deleteArrayElement(Connection connection, String entryIdUuid, String tableName, String key, Integer index) {
        if (tableName == null || tableName.trim().isEmpty()) {
            log.warn("Table name cannot be null or empty");
            return OperationResult.error("Table name cannot be null or empty");
        }
        if (entryIdUuid == null || entryIdUuid.trim().isEmpty()) {
            log.warn("Entry ID UUID cannot be null or empty");
            return OperationResult.error("Entry ID UUID cannot be null or empty");
        }
        if (key == null || key.trim().isEmpty()) {
            log.warn("Key cannot be null or empty");
            return OperationResult.error("Key cannot be null or empty");
        }
        if (index == null || index < 0) {
            log.warn("Index must be non-negative");
            return OperationResult.error("Index must be non-negative");
        }

        // Validate both table name and key against injection
        validateSqlIdentifier(tableName);
        validateSqlIdentifier(key);

        String queryString = String.format(DELETE_ARRAY_ELEMENT, tableName, key, key);
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            int paramIndex = 1;
            preparedStatement.setInt(paramIndex++, index);
            preparedStatement.setString(paramIndex, entryIdUuid);

            return extractDbEntryOperationResult(preparedStatement);
        } catch (Exception exc) {
            log.error("Error in method deleteArrayElement()", exc);
            return OperationResult.error("Failed to delete array element", exc);
        }
    }
}
