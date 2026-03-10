package io.github.deltatango.pgjson.model.repo;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import io.github.deltatango.pgjson.model.IndexInfo;
import io.github.deltatango.pgjson.model.enums.IndexType;
import io.github.deltatango.pgjson.model.enums.LogicalOperator;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL injection regression tests for DatabaseEntryRepo.
 * 
 * These tests verify that all user-supplied inputs are properly validated or parameterized
 * to prevent SQL injection attacks through search term construction, key names, and table names.
 */
@DisplayName("SQL Injection Prevention Tests")
public class DatabaseEntryRepoSqlInjectionTest {

    private final DatabaseEntryRepo repo = new DatabaseEntryRepo();

    // --- Helper methods ---

    private List<IndexInfo> createIndexInfoList(String key, IndexType indexType) {
        List<IndexInfo> indexes = new ArrayList<>();
        IndexInfo info = new IndexInfo();
        info.setIndexName(String.format("test_%s_%s", indexType.name(), key));
        info.setIndexType(indexType.name());
        info.setKeyPath(key);
        return indexes;
    }

    /**
     * Helper to unwrap a successful OperationResult or fail the test.
     */
    private SearchTerm unwrapSuccess(OperationResult<SearchTerm> result) throws PostgreJsonException {
        assertInstanceOf(OperationResult.Success.class, result,
                "Expected Success but got: " + result);
        return result.getOrThrow();
    }

    // --- SQL Identifier Validation Tests ---

    @Nested
    @DisplayName("SQL Identifier Validation")
    class SqlIdentifierValidation {

        @Test
        @DisplayName("Valid identifiers should be accepted")
        void validIdentifiersShouldPass() {
            assertDoesNotThrow(() -> DatabaseEntryRepo.validateSqlIdentifier("users"));
            assertDoesNotThrow(() -> DatabaseEntryRepo.validateSqlIdentifier("user_table"));
            assertDoesNotThrow(() -> DatabaseEntryRepo.validateSqlIdentifier("_private"));
            assertDoesNotThrow(() -> DatabaseEntryRepo.validateSqlIdentifier("Table123"));
            assertDoesNotThrow(() -> DatabaseEntryRepo.validateSqlIdentifier("a"));
        }

        @Test
        @DisplayName("SQL injection attempts in identifiers should be rejected")
        void sqlInjectionAttemptsShouldBeRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> DatabaseEntryRepo.validateSqlIdentifier("'; DROP TABLE users; --"));
            assertThrows(IllegalArgumentException.class,
                    () -> DatabaseEntryRepo.validateSqlIdentifier("name' OR '1'='1"));
            assertThrows(IllegalArgumentException.class,
                    () -> DatabaseEntryRepo.validateSqlIdentifier("table; DELETE FROM users"));
            assertThrows(IllegalArgumentException.class,
                    () -> DatabaseEntryRepo.validateSqlIdentifier("1table"));
            assertThrows(IllegalArgumentException.class,
                    () -> DatabaseEntryRepo.validateSqlIdentifier("table name"));
            assertThrows(IllegalArgumentException.class,
                    () -> DatabaseEntryRepo.validateSqlIdentifier("table-name"));
            assertThrows(IllegalArgumentException.class,
                    () -> DatabaseEntryRepo.validateSqlIdentifier("table.name"));
        }

        @Test
        @DisplayName("Null and empty identifiers should be rejected")
        void nullAndEmptyIdentifiersShouldBeRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> DatabaseEntryRepo.validateSqlIdentifier(null));
            assertThrows(IllegalArgumentException.class,
                    () -> DatabaseEntryRepo.validateSqlIdentifier(""));
        }
    }

    // --- Search Key Injection Tests ---

    @Nested
    @DisplayName("Search Key SQL Injection Prevention")
    class SearchKeyInjection {

        @Test
        @DisplayName("SQL injection via search key in exact match should be rejected")
        void exactMatchKeyInjectionShouldBeRejected() {
            JsonObject searchTerm = new JsonObject();
            searchTerm.addProperty("'; DROP TABLE users; --", "value");

            assertThrows(IllegalArgumentException.class,
                    () -> repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and, "test_table"));
        }

        @Test
        @DisplayName("SQL injection via search key with quote escaping should be rejected")
        void quoteEscapingKeyInjectionShouldBeRejected() {
            JsonObject searchTerm = new JsonObject();
            searchTerm.addProperty("name' OR '1'='1", "value");

            assertThrows(IllegalArgumentException.class,
                    () -> repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and, "test_table"));
        }

        @Test
        @DisplayName("SQL injection via search key with semicolon should be rejected")
        void semicolonKeyInjectionShouldBeRejected() {
            JsonObject searchTerm = new JsonObject();
            searchTerm.addProperty("name; DELETE FROM data", "value");

            assertThrows(IllegalArgumentException.class,
                    () -> repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and, "test_table"));
        }

        @Test
        @DisplayName("SQL injection via search key with dash-dash comment should be rejected")
        void dashCommentKeyInjectionShouldBeRejected() {
            JsonObject searchTerm = new JsonObject();
            searchTerm.addProperty("name--", "value");

            assertThrows(IllegalArgumentException.class,
                    () -> repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and, "test_table"));
        }

        @Test
        @DisplayName("SQL injection via nested object key should be rejected")
        void nestedObjectKeyInjectionShouldBeRejected() {
            JsonObject searchTerm = new JsonObject();
            JsonObject nested = new JsonObject();
            nested.addProperty("'; DROP TABLE users; --", "value");
            searchTerm.add("address", nested);

            // The outer key 'address' is valid, but the nested key injection should be caught
            assertDoesNotThrow(() -> {
                try {
                    SearchTerm result = unwrapSuccess(
                            repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and, "test_table"));
                    // If it succeeds, verify the malicious key is NOT in the SQL string unparameterized
                    assertFalse(result.getSearchTerm().contains("DROP TABLE"));
                } catch (IllegalArgumentException e) {
                    // This is also acceptable - the key was rejected
                    assertTrue(e.getMessage().contains("Invalid SQL identifier"));
                }
            });
        }
    }

    // --- Search Value Injection Tests (Parameterized Queries) ---

    @Nested
    @DisplayName("Search Value SQL Injection Prevention via Parameterization")
    class SearchValueInjection {

        @Test
        @DisplayName("SQL injection via exact match value should be safely parameterized")
        void exactMatchValueInjectionShouldBeParameterized() throws PostgreJsonException {
            JsonObject searchTerm = new JsonObject();
            // Single-word value triggers exact match
            String maliciousValue = "';DROP--";
            searchTerm.addProperty("name", maliciousValue);

            SearchTerm result = unwrapSuccess(
                    repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and, "test_table"));

            // The malicious value should NOT be in the SQL string
            assertFalse(result.getSearchTerm().contains(maliciousValue),
                    "Malicious value should not be embedded in SQL");
            // It should use parameterized placeholder (exact match for single-word)
            assertTrue(result.getSearchTerm().contains("json_data->>'name'=?"),
                    "SQL should use parameterized placeholder");
            // The malicious value should be in the parameters list
            assertTrue(result.getParameters().contains(maliciousValue),
                    "Malicious value should be safely stored as a parameter");
        }

        @Test
        @DisplayName("SQL injection via FTS search value should be safely parameterized")
        void ftsSearchValueInjectionShouldBeParameterized() throws PostgreJsonException {
            JsonObject searchTerm = new JsonObject();
            String maliciousValue = "' OR 1=1 -- malicious search";
            searchTerm.addProperty("description", maliciousValue);

            SearchTerm result = unwrapSuccess(
                    repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and, "test_table"));

            // The malicious value should NOT be in the SQL string
            assertFalse(result.getSearchTerm().contains("OR 1=1"),
                    "Malicious value should not be embedded in SQL");
            // It should use parameterized placeholder
            assertTrue(result.getSearchTerm().contains("to_tsquery('simple', ?)"),
                    "SQL should use parameterized placeholder for FTS");
            assertTrue(result.getParameters().contains(maliciousValue),
                    "Malicious value should be safely stored as a parameter");
        }

        @Test
        @DisplayName("SQL injection via object match value should be safely parameterized")
        void objectMatchValueInjectionShouldBeParameterized() throws PostgreJsonException {
            JsonObject searchTerm = new JsonObject();
            JsonObject maliciousObject = new JsonObject();
            maliciousObject.addProperty("city", "'; DROP TABLE users; --");
            searchTerm.add("address", maliciousObject);

            SearchTerm result = unwrapSuccess(
                    repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and, "test_table"));

            // The malicious value should NOT be in the SQL string
            assertFalse(result.getSearchTerm().contains("DROP TABLE"),
                    "Malicious value should not be embedded in SQL");
            // It should use parameterized placeholder
            assertTrue(result.getSearchTerm().contains("?::jsonb"),
                    "SQL should use parameterized placeholder for object");
        }

        @Test
        @DisplayName("SQL injection via array match values should be safely parameterized")
        void arrayMatchValueInjectionShouldBeParameterized() throws PostgreJsonException {
            JsonObject searchTerm = new JsonObject();
            JsonArray array = new JsonArray();
            array.add("'; DROP TABLE users; --");
            array.add("normal_value");
            searchTerm.add("tags", array);

            SearchTerm result = unwrapSuccess(
                    repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and, "test_table"));

            // Malicious values should NOT be in the SQL string
            assertFalse(result.getSearchTerm().contains("DROP TABLE"),
                    "Malicious value should not be embedded in SQL");
            // Parameters should contain the values
            assertEquals(2, result.getParameters().size(),
                    "Should have two parameterized values");
        }
    }

    // --- Table Name Injection Tests ---

    @Nested
    @DisplayName("Table Name SQL Injection Prevention")
    class TableNameInjection {

        @Test
        @DisplayName("SQL injection via table name should be rejected")
        void tableNameInjectionShouldBeRejected() {
            JsonObject searchTerm = new JsonObject();
            searchTerm.addProperty("name", "John");

            assertThrows(IllegalArgumentException.class,
                    () -> repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and,
                            "users; DROP TABLE data; --"));
        }

        @Test
        @DisplayName("SQL injection via table name with quotes should be rejected")
        void tableNameWithQuotesInjectionShouldBeRejected() {
            JsonObject searchTerm = new JsonObject();
            searchTerm.addProperty("name", "John");

            assertThrows(IllegalArgumentException.class,
                    () -> repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and,
                            "users' OR '1'='1"));
        }

        @Test
        @DisplayName("Table name with spaces should be rejected")
        void tableNameWithSpacesShouldBeRejected() {
            JsonObject searchTerm = new JsonObject();
            searchTerm.addProperty("name", "John");

            assertThrows(IllegalArgumentException.class,
                    () -> repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and,
                            "my table"));
        }

        @Test
        @DisplayName("Table name with dots should be rejected")
        void tableNameWithDotsShouldBeRejected() {
            JsonObject searchTerm = new JsonObject();
            searchTerm.addProperty("name", "John");

            assertThrows(IllegalArgumentException.class,
                    () -> repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and,
                            "public.users"));
        }

        @Test
        @DisplayName("Valid table name should be accepted")
        void validTableNameShouldBeAccepted() throws PostgreJsonException {
            JsonObject searchTerm = new JsonObject();
            searchTerm.addProperty("name", "John");

            OperationResult<SearchTerm> result = repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and, "users_table");
            assertInstanceOf(OperationResult.Success.class, result);
            assertNotNull(result.getOrThrow());
        }
    }

    // --- Combined Attack Vectors ---

    @Nested
    @DisplayName("Combined SQL Injection Attack Vectors")
    class CombinedAttackVectors {

        @Test
        @DisplayName("Multiple injection attempts in one search should all be safe")
        void multipleInjectionAttemptsShouldBeSafe() throws PostgreJsonException {
            JsonObject searchTerm = new JsonObject();
            // Use single-word malicious values to ensure exact match for both
            searchTerm.addProperty("name", "';DROP--");
            searchTerm.addProperty("email", "admin';DELETE--");

            SearchTerm result = unwrapSuccess(
                    repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and, "test_table"));

            // Neither value should be embedded in SQL
            assertFalse(result.getSearchTerm().contains("DROP"), "Malicious value should not be in SQL");
            assertFalse(result.getSearchTerm().contains("DELETE"), "Malicious value should not be in SQL");
            // Both should use parameterized placeholders (exact match for single-word)
            assertTrue(result.getSearchTerm().contains("json_data->>'name'=?"));
            assertTrue(result.getSearchTerm().contains("json_data->>'email'=?"));
            // Both values should be in parameters
            assertEquals(2, result.getParameters().size());
        }

        @Test
        @DisplayName("Union-based injection attempt should be parameterized")
        void unionInjectionShouldBeParameterized() throws PostgreJsonException {
            JsonObject searchTerm = new JsonObject();
            searchTerm.addProperty("name", "' UNION SELECT * FROM pg_user --");

            SearchTerm result = unwrapSuccess(
                    repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and, "test_table"));

            assertFalse(result.getSearchTerm().contains("UNION SELECT"));
            assertTrue(result.getParameters().contains("' UNION SELECT * FROM pg_user --"));
        }

        @Test
        @DisplayName("Boolean-based blind injection attempt should be parameterized")
        void booleanBlindInjectionShouldBeParameterized() throws PostgreJsonException {
            JsonObject searchTerm = new JsonObject();
            searchTerm.addProperty("name", "' AND 1=1 AND '1'='1");

            SearchTerm result = unwrapSuccess(
                    repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and, "test_table"));

            assertFalse(result.getSearchTerm().contains("AND 1=1"));
            assertTrue(result.getParameters().contains("' AND 1=1 AND '1'='1"));
        }

        @Test
        @DisplayName("Time-based blind injection attempt should be parameterized")
        void timeBasedBlindInjectionShouldBeParameterized() throws PostgreJsonException {
            JsonObject searchTerm = new JsonObject();
            searchTerm.addProperty("name", "'; SELECT pg_sleep(10); --");

            SearchTerm result = unwrapSuccess(
                    repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and, "test_table"));

            assertFalse(result.getSearchTerm().contains("pg_sleep"));
            assertTrue(result.getParameters().contains("'; SELECT pg_sleep(10); --"));
        }

        @Test
        @DisplayName("Control fields should be skipped even with malicious names")
        void controlFieldsShouldBeSkipped() throws PostgreJsonException {
            JsonObject searchTerm = new JsonObject();
            searchTerm.addProperty("name", "John");
            // These control fields should be silently skipped, not treated as search keys
            searchTerm.addProperty("logicalOperator", "'; DROP TABLE users; --");
            searchTerm.addProperty("limit", "999999");

            SearchTerm result = unwrapSuccess(
                    repo.createTerm(searchTerm, new ArrayList<>(), 10, LogicalOperator.and, "test_table"));

            assertNotNull(result);
            assertFalse(result.getSearchTerm().contains("DROP TABLE"));
            // Only the legitimate 'name' field should be in the search term
            assertTrue(result.getSearchTerm().contains("json_data->>'name'=?"));
            assertEquals(1, result.getParameters().size());
        }
    }

    // --- Delete Array Element Key Path Injection Tests ---

    @Nested
    @DisplayName("Delete Array Element Key Path Injection Prevention")
    class DeleteArrayElementKeyInjection {

        @Test
        @DisplayName("SQL injection via key parameter in deleteArrayElement should be rejected")
        void keyInjectionInDeleteArrayElementShouldBeRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> repo.deleteArrayElement(null, "some-uuid", "test_table",
                            "'; DROP TABLE users; --", 0));
        }

        @Test
        @DisplayName("Key with semicolon in deleteArrayElement should be rejected")
        void keySemicolonInjectionShouldBeRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> repo.deleteArrayElement(null, "some-uuid", "test_table",
                            "key; DELETE FROM data", 0));
        }

        @Test
        @DisplayName("Key with quotes in deleteArrayElement should be rejected")
        void keyQuoteInjectionShouldBeRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> repo.deleteArrayElement(null, "some-uuid", "test_table",
                            "key' OR '1'='1", 0));
        }

        @Test
        @DisplayName("Key with spaces in deleteArrayElement should be rejected")
        void keySpaceInjectionShouldBeRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> repo.deleteArrayElement(null, "some-uuid", "test_table",
                            "key name", 0));
        }

        @Test
        @DisplayName("Table name injection in deleteArrayElement should be rejected")
        void tableNameInjectionInDeleteArrayElementShouldBeRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> repo.deleteArrayElement(null, "some-uuid",
                            "table; DROP TABLE users", "validkey", 0));
        }

        @Test
        @DisplayName("Null key in deleteArrayElement should return error")
        void nullKeyInDeleteArrayElementShouldReturnError() {
            OperationResult<?> result = repo.deleteArrayElement(null, "some-uuid", "test_table", null, 0);
            assertTrue(result.isError(), "Null key should return error");
        }

        @Test
        @DisplayName("Empty key in deleteArrayElement should return error")
        void emptyKeyInDeleteArrayElementShouldReturnError() {
            OperationResult<?> result = repo.deleteArrayElement(null, "some-uuid", "test_table", "", 0);
            assertTrue(result.isError(), "Empty key should return error");
        }
    }

    // --- Insert/Update/Delete Entry Point Injection Tests ---

    @Nested
    @DisplayName("CRUD Entry Point Injection Prevention")
    class CrudEntryPointInjection {

        @Test
        @DisplayName("SQL injection via table name in insertData should be rejected")
        void tableNameInjectionInInsertDataShouldBeRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> repo.insertData(null, "uuid-123", "table; DROP TABLE users",
                            "{\"key\":\"value\"}", 1));
        }

        @Test
        @DisplayName("SQL injection via table name in updateData should be rejected")
        void tableNameInjectionInUpdateDataShouldBeRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> repo.updateData(null, "table; DROP TABLE users",
                            "{\"key\":\"value\"}", "uuid-123"));
        }

        @Test
        @DisplayName("SQL injection via table name in deleteRecord should be rejected")
        void tableNameInjectionInDeleteRecordShouldBeRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> repo.deleteRecord(null, "table; DROP TABLE users", "uuid-123"));
        }

        @Test
        @DisplayName("SQL injection via table name in selectDataByIdUuid should be rejected")
        void tableNameInjectionInSelectDataByIdUuidShouldBeRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> repo.selectDataByIdUuid(null, "table; DROP TABLE users", "uuid-123"));
        }

        @Test
        @DisplayName("Null table name in insertData should return error")
        void nullTableNameInInsertDataShouldReturnError() {
            OperationResult<?> result = repo.insertData(null, "uuid-123", null, "{\"key\":\"value\"}", 1);
            assertTrue(result.isError(), "Null table name should return error");
        }

        @Test
        @DisplayName("Null entryIdUuid in insertData should return error")
        void nullEntryIdUuidInInsertDataShouldReturnError() {
            OperationResult<?> result = repo.insertData(null, null, "test_table", "{\"key\":\"value\"}", 1);
            assertTrue(result.isError(), "Null entryIdUuid should return error");
        }

        @Test
        @DisplayName("Null jsonData in insertData should return error")
        void nullJsonDataInInsertDataShouldReturnError() {
            OperationResult<?> result = repo.insertData(null, "uuid-123", "test_table", null, 1);
            assertTrue(result.isError(), "Null jsonData should return error");
        }

        @Test
        @DisplayName("Invalid tableDefId in insertData should return error")
        void invalidTableDefIdInInsertDataShouldReturnError() {
            OperationResult<?> result = repo.insertData(null, "uuid-123", "test_table", "{\"key\":\"value\"}", 0);
            assertTrue(result.isError(), "Zero tableDefId should return error");
        }

        @Test
        @DisplayName("Null table name in updateData should return error")
        void nullTableNameInUpdateDataShouldReturnError() {
            OperationResult<?> result = repo.updateData(null, null, "{\"key\":\"value\"}", "uuid-123");
            assertTrue(result.isError(), "Null table name should return error");
        }

        @Test
        @DisplayName("Null entryIdUuid in updateData should return error")
        void nullEntryIdUuidInUpdateDataShouldReturnError() {
            OperationResult<?> result = repo.updateData(null, "test_table", "{\"key\":\"value\"}", null);
            assertTrue(result.isError(), "Null entryIdUuid should return error");
        }

        @Test
        @DisplayName("Negative index in deleteArrayElement should return error")
        void negativeIndexInDeleteArrayElementShouldReturnError() {
            OperationResult<?> result = repo.deleteArrayElement(null, "uuid-123", "test_table", "key", -1);
            assertTrue(result.isError(), "Negative index should return error");
        }
    }

    // --- Schema Name Injection Tests (via TableDefRepo) ---

    @Nested
    @DisplayName("Schema Name Injection Prevention")
    class SchemaNameInjection {

        private final TableDefRepo tableDefRepo = new TableDefRepo();

        @Test
        @DisplayName("Null schema name in selectTableDefByTableNameAndName should return error")
        void nullSchemaNameShouldReturnError() {
            OperationResult<?> result = tableDefRepo.selectTableDefByTableNameAndName(null, "test_table", null);
            assertTrue(result.isError(), "Null schema name should return error");
        }

        @Test
        @DisplayName("Empty schema name in selectTableDefByTableNameAndName should return error")
        void emptySchemaNameShouldReturnError() {
            OperationResult<?> result = tableDefRepo.selectTableDefByTableNameAndName(null, "test_table", "");
            assertTrue(result.isError(), "Empty schema name should return error");
        }

        @Test
        @DisplayName("Whitespace schema name in selectTableDefByTableNameAndName should return error")
        void whitespaceSchemaNameShouldReturnError() {
            OperationResult<?> result = tableDefRepo.selectTableDefByTableNameAndName(null, "test_table", "   ");
            assertTrue(result.isError(), "Whitespace schema name should return error");
        }

        @Test
        @DisplayName("Null table name in selectTableDefByTableNameAndName should return error")
        void nullTableNameShouldReturnError() {
            OperationResult<?> result = tableDefRepo.selectTableDefByTableNameAndName(null, null, "schema1");
            assertTrue(result.isError(), "Null table name should return error");
        }

        @Test
        @DisplayName("Null table name in selectTableDefByTableName should return error")
        void nullTableNameInSelectByTableNameShouldReturnError() {
            OperationResult<?> result = tableDefRepo.selectTableDefByTableName(null, null);
            assertTrue(result.isError(), "Null table name should return error");
        }

        @Test
        @DisplayName("Null UUID in selectTableDefByUuid should return error")
        void nullUuidShouldReturnError() {
            OperationResult<?> result = tableDefRepo.selectTableDefByUuid(null, null);
            assertTrue(result.isError(), "Null UUID should return error");
        }

        @Test
        @DisplayName("Invalid ID in selectTableDefById should return error")
        void invalidIdShouldReturnError() {
            OperationResult<?> result = tableDefRepo.selectTableDefById(null, 0);
            assertTrue(result.isError(), "Zero ID should return error");
        }
    }
}
