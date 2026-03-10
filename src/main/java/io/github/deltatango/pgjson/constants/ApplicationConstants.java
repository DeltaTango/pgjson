package io.github.deltatango.pgjson.constants;

/**
 * Application-wide constants to eliminate magic numbers and strings throughout the PostgreSQL JSON Client.
 *
 * <p>This class provides centralized constants for all application-wide values, including
 * default values, validation limits, error messages, and configuration parameters. Using
 * these constants improves code maintainability and reduces the risk of typos in string
 * literals.</p>
 *
 * <h2>Constant Categories:</h2>
 * <ul>
 *   <li><strong>Default Values:</strong> Default settings for queries, operators, and JSON objects</li>
 *   <li><strong>Validation Limits:</strong> Minimum values for limits, offsets, and IDs</li>
 *   <li><strong>Order Types:</strong> Sort order constants (asc/desc)</li>
 *   <li><strong>Logical Operators:</strong> Query logical operators (AND/OR)</li>
 *   <li><strong>JSON Field Names:</strong> Standard field names for JSON operations</li>
 *   <li><strong>Error Messages:</strong> Standardized error messages for validation and operations</li>
 *   <li><strong>Success Messages:</strong> Standardized success messages for operations</li>
 *   <li><strong>Warning Messages:</strong> Standardized warning messages for edge cases</li>
 *   <li><strong>Log Messages:</strong> Standardized log messages for debugging and monitoring</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Instead of magic strings
 * if (orderType.equals("asc")) { ... }
 *
 * // Use constants
 * if (orderType.equals(ApplicationConstants.ORDER_ASC)) { ... }
 *
 * // Error handling
 * throw new RequestException(ApplicationConstants.ERROR_LIMIT_INVALID);
 * }</pre>
 *
 * <h2>Benefits:</h2>
 * <ul>
 *   <li><strong>Maintainability:</strong> Single source of truth for all constants</li>
 *   <li><strong>Type Safety:</strong> Compile-time checking for constant usage</li>
 *   <li><strong>Consistency:</strong> Standardized values across the application</li>
 *   <li><strong>Refactoring:</strong> Easy to update values across the entire codebase</li>
 *   <li><strong>Documentation:</strong> Self-documenting code with meaningful constant names</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe as it only contains
 * static final constants with no mutable state.</p>
 *
 * @author PostgreSQL JSON Client Team
 * @version 25.10.1
 * @since 1.0.0
 */
public final class ApplicationConstants {

    // Private constructor to prevent instantiation
    private ApplicationConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Default values
    /** Default sort order for query results */
    public static final String DEFAULT_ORDER = "asc";
    /** Default logical operator for combining search terms */
    public static final String DEFAULT_LOGICAL_OPERATOR = "AND";
    /** Default empty JSON object */
    public static final String DEFAULT_JSON_OBJECT = "{}";

    // Validation limits
    /** Minimum value for query limit parameter */
    public static final int MIN_LIMIT = 1;
    /** Minimum value for query offset parameter */
    public static final int MIN_OFFSET = 0;
    /** Minimum value for table definition ID */
    public static final int MIN_TABLE_DEF_ID = 1;
    /** Minimum value for array index parameter */
    public static final int MIN_INDEX = 0;

    // Order types
    /** Ascending sort order */
    public static final String ORDER_ASC = "asc";
    /** Descending sort order */
    public static final String ORDER_DESC = "desc";

    // Logical operators
    /** AND logical operator for combining search terms */
    public static final String LOGICAL_OPERATOR_AND = "AND";
    /** OR logical operator for combining search terms */
    public static final String LOGICAL_OPERATOR_OR = "OR";

    // JSON field names
    /** JSON field name for sort order type */
    public static final String JSON_FIELD_ORDER_TYPE = "orderType";
    /** JSON field name for logical operator */
    public static final String JSON_FIELD_LOGICAL_OPERATOR = "logicalOperator";
    /** JSON field name for query limit */
    public static final String JSON_FIELD_LIMIT = "limit";
    /** JSON field name for query offset */
    public static final String JSON_FIELD_OFFSET = "offset";
    /** JSON field name for search term */
    public static final String JSON_FIELD_SEARCH_TERM = "searchTerm";
    /** JSON field name for UI label */
    public static final String JSON_FIELD_UI_LABEL = "uiLabel";

    // Error messages
    public static final String ERROR_SEARCH_JSON_NULL = "Search JSON cannot be null or empty";
    public static final String ERROR_REQUEST_EMPTY = "Request was empty";
    public static final String ERROR_LIMIT_MISSING = "Query limit is missing";
    public static final String ERROR_LIMIT_INVALID = "Query limit value invalid, should be at least 1";
    public static final String ERROR_OFFSET_MISSING = "Query offset is missing";
    public static final String ERROR_OFFSET_INVALID = "Query offset value is invalid, should be at least 0";
    public static final String ERROR_SEARCH_TERM_INVALID = "Search term must be a JSON object";
    public static final String ERROR_FAULTY_REQUEST = "Faulty request - search term is null";
    public static final String ERROR_TABLE_NAME_NULL = "Table name cannot be null or empty";
    public static final String ERROR_ID_UUID_NULL = "ID UUID cannot be null or empty";
    public static final String ERROR_JSON_DATA_NULL = "JSON data cannot be null or empty";
    public static final String ERROR_SCHEMA_NULL = "Schema cannot be null or empty";
    public static final String ERROR_TABLE_DEF_ID_INVALID = "Table definition ID must be positive";
    public static final String ERROR_INDEX_INVALID = "Index must be non-negative";
    public static final String ERROR_KEY_NULL = "Key cannot be null or empty";

    // Success messages
    public static final String SUCCESS_DATA_INSERTED = "Database entry inserted in database";
    public static final String SUCCESS_DATA_UPDATED = "Database entry updated in database";
    public static final String SUCCESS_DATA_DELETED = "Delete done";
    public static final String SUCCESS_VALIDATION_PASSED = "Data is valid";

    // Warning messages
    public static final String WARN_TABLE_DEF_NOT_FOUND = "Table def not found for table";
    public static final String WARN_ENTRY_NOT_FOUND = "Entry not found with idUuid {} in database table {}";
    public static final String WARN_INVALID_SCHEMA = "Invalid schema data for table";
    public static final String WARN_INVALID_JSON = "Invalid schema JSON format";
    public static final String WARN_CANNOT_ITERATE_UI_LABELS = "Cannot iterate UI labels with null parameters";

    // Log messages
    public static final String LOG_LOADING_TABLE_DEFS = "Loading effective table definitions into memory";
    public static final String LOG_FOUND_TABLE_DEF = "Found tableDef with idUuid {}";
    public static final String LOG_ARRAY_QUERY_DETECTED = "Array query detected, setting limit to null";
    public static final String LOG_SINGLE_TERM_GENERATED = "Single term generated";
    public static final String LOG_FINAL_TERM = "Final term: {}";
    public static final String LOG_TERMS = "Terms: {}";
    public static final String LOG_LABELS_COUNT = "LABELS: {}";

    // Database operations
    public static final String DB_OPERATION_INSERT = "INSERT";
    public static final String DB_OPERATION_UPDATE = "UPDATE";
    public static final String DB_OPERATION_DELETE = "DELETE";
    public static final String DB_OPERATION_SELECT = "SELECT";

    // Exception messages
    public static final String EXCEPTION_HAPPENED_MESSAGE = "Operation failed due to an unexpected error";
    public static final String EXCEPTION_DATABASE_ENTRY_NOT_FOUND = "Database entry not found";
    public static final String EXCEPTION_DATA_VALIDATION_FAILED = "Data validation failed";
    public static final String EXCEPTION_DATABASE_ENTRY_NOT_INSERTED = "Database entry was not inserted in database due to SQL error";
    public static final String EXCEPTION_DATABASE_ENTRY_NOT_UPDATED = "Database entry was not updated in database";
    public static final String EXCEPTION_COULD_NOT_GENERATE_SEARCH_TERM = "Could not generate search term";
    public static final String EXCEPTION_EITHER_KEY_OR_INDEX_MISSING = "Either key or index is missing";
}
