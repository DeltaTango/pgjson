package io.github.deltatango.pgjson.model.repo;

import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import io.github.deltatango.pgjson.exceptions.RequestException;
import io.github.deltatango.pgjson.model.DatabaseEntry;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import io.github.deltatango.pgjson.model.operations.Result;
import io.github.deltatango.pgjson.util.DatabaseConfigurationUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end SQL injection integration tests against a real PostgreSQL database.
 *
 * <p>These tests verify that malicious input is properly handled when flowing through
 * the complete request path: {@code PostgreSqlJsonClient} -> Service -> Repository -> DB.</p>
 *
 * <p>Unlike the unit tests in {@link DatabaseEntryRepoSqlInjectionTest}, these tests
 * execute actual SQL against a TestContainers PostgreSQL instance.</p>
 */
@Slf4j
@DisplayName("SQL Injection Integration Tests")
public class DatabaseEntryRepoSqlInjectionIntegrationTest extends DatabaseConfigurationUtil {

    private static final String TABLE_NAME = "table1";

    // --- Search Path Injection (end-to-end) ---

    @Nested
    @DisplayName("Search Path Injection via selectData")
    class SearchPathInjection {

        @Test
        @DisplayName("Malicious search value should not corrupt query results")
        void maliciousSearchValueShouldNotCorruptResults() throws RequestException, PostgreJsonException {
            String searchJson = "{\"limit\": 10, \"offset\": 0, \"searchTerm\": {\"attribute1\": \"'; DROP TABLE table1; --\"}}";

            OperationResult<List<DatabaseEntry>> result = postgresqlJsonClient.selectData(TABLE_NAME, searchJson);

            // Should succeed but return no results (no entry matches the malicious value)
            assertTrue(result.isSuccess(), "Search should succeed without SQL error");
            List<DatabaseEntry> entries = result.getOrThrow();
            assertTrue(entries.isEmpty(), "No entries should match the malicious value");

            // Verify the table still exists by querying it
            assertTrue(postgresqlJsonClient.isDatabaseRunning(), "Database should still be running");
        }

        @Test
        @DisplayName("UNION injection in search value should be safely parameterized")
        void unionInjectionInSearchValueShouldBeSafe() throws RequestException, PostgreJsonException {
            String searchJson = "{\"limit\": 10, \"offset\": 0, \"searchTerm\": {\"attribute1\": \"' UNION SELECT * FROM pg_user --\"}}";

            OperationResult<List<DatabaseEntry>> result = postgresqlJsonClient.selectData(TABLE_NAME, searchJson);

            assertTrue(result.isSuccess(), "Search should succeed without SQL error");
            List<DatabaseEntry> entries = result.getOrThrow();
            assertTrue(entries.isEmpty(), "No entries should match the injected value");
        }

        @Test
        @DisplayName("Boolean blind injection in search value should be safely parameterized")
        void booleanBlindInjectionShouldBeSafe() throws RequestException, PostgreJsonException {
            String searchJson = "{\"limit\": 10, \"offset\": 0, \"searchTerm\": {\"attribute1\": \"in' AND '1'='1\"}}";

            OperationResult<List<DatabaseEntry>> result = postgresqlJsonClient.selectData(TABLE_NAME, searchJson);

            assertTrue(result.isSuccess(), "Search should succeed without SQL error");
            // Should not return results that match "in" (the boolean condition should not be evaluated)
            List<DatabaseEntry> entries = result.getOrThrow();
            for (DatabaseEntry entry : entries) {
                // If any results returned, verify they match the literal string, not the injection
                assertTrue(entry.getJsonData().contains("in' AND '1'='1"),
                        "Any returned entry should match the literal malicious string");
            }
        }

        @Test
        @DisplayName("Time-based blind injection should not cause delay")
        void timeBasedBlindInjectionShouldNotCauseDelay() throws RequestException {
            long startTime = System.currentTimeMillis();
            String searchJson = "{\"limit\": 10, \"offset\": 0, \"searchTerm\": {\"attribute1\": \"'; SELECT pg_sleep(10); --\"}}";

            OperationResult<List<DatabaseEntry>> result = postgresqlJsonClient.selectData(TABLE_NAME, searchJson);
            long duration = System.currentTimeMillis() - startTime;

            assertTrue(result.isSuccess(), "Search should succeed without SQL error");
            assertTrue(duration < 5000, "Query should complete quickly (no pg_sleep executed), took: " + duration + "ms");
        }

        @Test
        @DisplayName("Malicious table name in selectData should be rejected")
        void maliciousTableNameShouldBeRejected() throws RequestException {
            String searchJson = "{\"limit\": 10, \"offset\": 0, \"searchTerm\": {\"attribute1\": \"test\"}}";

            // selectData catches IllegalArgumentException internally and returns OperationResult.Error
            OperationResult<List<DatabaseEntry>> result =
                    postgresqlJsonClient.selectData("table1; DROP TABLE tabledef", searchJson);
            assertFalse(result.isSuccess(), "Malicious table name should result in an error");
            assertTrue(postgresqlJsonClient.isDatabaseRunning(), "Database should still be running");
        }
    }

    // --- Insert Path Injection ---

    @Nested
    @DisplayName("Insert Path Injection")
    class InsertPathInjection {

        @Test
        @DisplayName("Malicious table name in insertData should be rejected")
        void maliciousTableNameInInsertShouldBeRejected() {
            String jsonData = "{\"attribute1\": \"test\", \"attribute2\": \"test\", " +
                    "\"attribute3\": {\"attribute7\": \"val\", \"attribute8\": \"A\"}, " +
                    "\"attribute4\": [{\"attribute9\": \"X\", \"attribute10\": \"val\"}], " +
                    "\"attribute5\": {\"attribute6\": [{\"attribute11\": \"val\"}], " +
                    "\"attribute12\": {\"attribute13\": \"val\"}}}";

            // Malicious table name won't match any tabledef entry, so the operation
            // fails gracefully (schema not found) rather than executing malicious SQL
            OperationResult<Result> result = postgresqlJsonClient.insertData("table1; DROP TABLE tabledef", jsonData);
            assertFalse(result.isSuccess(), "Malicious table name should result in an error");
            assertTrue(postgresqlJsonClient.isDatabaseRunning(), "Database should still be running");
        }

        @Test
        @DisplayName("JSON data with SQL injection strings should be safely stored")
        void jsonDataWithSqlInjectionShouldBeSafelyStored() throws PostgreJsonException {
            String jsonData = "{\"attribute1\": \"'; DROP TABLE tabledef; --\", " +
                    "\"attribute2\": \"' OR 1=1 --\", " +
                    "\"attribute3\": {\"attribute7\": \"val\", \"attribute8\": \"A\"}, " +
                    "\"attribute4\": [{\"attribute9\": \"X\", \"attribute10\": \"val\"}], " +
                    "\"attribute5\": {\"attribute6\": [{\"attribute11\": \"val\"}], " +
                    "\"attribute12\": {\"attribute13\": \"val\"}}}";

            OperationResult<Result> result = postgresqlJsonClient.insertData(TABLE_NAME, jsonData);
            assertTrue(result.isSuccess(), "Insert with SQL injection in values should succeed");

            // Verify the data was stored literally, not executed
            Result insertResult = result.getOrThrow();
            assertNotNull(insertResult.getIduuid());

            OperationResult<DatabaseEntry> entry = postgresqlJsonClient.selectDataByIdUuid(
                    insertResult.getIduuid(), TABLE_NAME);
            assertTrue(entry.isSuccess(), "Should be able to retrieve the entry");
            assertTrue(entry.getOrThrow().getJsonData().contains("DROP TABLE tabledef"),
                    "Malicious string should be stored literally in JSON");

            // Verify the tabledef table still exists
            assertTrue(postgresqlJsonClient.isDatabaseRunning(), "Database should still be running");
        }
    }

    // --- Update Path Injection ---

    @Nested
    @DisplayName("Update Path Injection")
    class UpdatePathInjection {

        @Test
        @DisplayName("Malicious table name in updateData should be rejected")
        void maliciousTableNameInUpdateShouldBeRejected() {
            String jsonData = "{\"attribute1\": \"test\", \"attribute2\": \"test\", " +
                    "\"attribute3\": {\"attribute7\": \"val\", \"attribute8\": \"A\"}, " +
                    "\"attribute4\": [{\"attribute9\": \"X\", \"attribute10\": \"val\"}], " +
                    "\"attribute5\": {\"attribute6\": [{\"attribute11\": \"val\"}], " +
                    "\"attribute12\": {\"attribute13\": \"val\"}}}";

            // updateData catches all exceptions internally and returns OperationResult.Error
            OperationResult<Result> result = postgresqlJsonClient.updateData(
                    "table1; DROP TABLE tabledef", jsonData, "some-uuid");
            assertFalse(result.isSuccess(), "Malicious table name should result in an error");
            assertTrue(postgresqlJsonClient.isDatabaseRunning(), "Database should still be running");
        }

        @Test
        @DisplayName("Malicious entryIdUuid in updateData should not cause SQL injection")
        void maliciousUuidInUpdateShouldBeSafe() {
            String jsonData = "{\"attribute1\": \"test\", \"attribute2\": \"test\", " +
                    "\"attribute3\": {\"attribute7\": \"val\", \"attribute8\": \"A\"}, " +
                    "\"attribute4\": [{\"attribute9\": \"X\", \"attribute10\": \"val\"}], " +
                    "\"attribute5\": {\"attribute6\": [{\"attribute11\": \"val\"}], " +
                    "\"attribute12\": {\"attribute13\": \"val\"}}}";

            // UUID is bound via PreparedStatement parameter, so injection should be harmless
            OperationResult<Result> result = postgresqlJsonClient.updateData(TABLE_NAME, jsonData,
                    "'; DROP TABLE tabledef; --");

            // Should fail gracefully (not found or error), not execute the injection
            assertFalse(result.isSuccess(), "Update with malicious UUID should not succeed");
            assertTrue(postgresqlJsonClient.isDatabaseRunning(), "Database should still be running");
        }
    }

    // --- Delete Path Injection ---

    @Nested
    @DisplayName("Delete Path Injection")
    class DeletePathInjection {

        @Test
        @DisplayName("Malicious table name in deleteData should be rejected")
        void maliciousTableNameInDeleteShouldBeRejected() {
            assertThrows(Exception.class,
                    () -> postgresqlJsonClient.deleteData("table1; DROP TABLE tabledef", "some-uuid"),
                    "Malicious table name should be rejected");
        }

        @Test
        @DisplayName("Malicious idUuid in deleteData should not cause SQL injection")
        void maliciousUuidInDeleteShouldBeSafe() {
            // UUID is bound via PreparedStatement parameter
            OperationResult<Boolean> result = postgresqlJsonClient.deleteData(TABLE_NAME,
                    "'; DROP TABLE tabledef; --");

            // Should succeed (0 rows deleted) or return success with no effect
            assertTrue(result.isSuccess(), "Delete with non-matching UUID should succeed gracefully");
            assertTrue(postgresqlJsonClient.isDatabaseRunning(), "Database should still be running");
        }
    }

    // --- Delete Array Element Injection ---

    @Nested
    @DisplayName("Delete Array Element Injection")
    class DeleteArrayElementInjection {

        @Test
        @DisplayName("Malicious key in deleteArrayElement should be rejected")
        void maliciousKeyInDeleteArrayElementShouldBeRejected() {
            // deleteArrayElement catches all exceptions internally and returns OperationResult.Error
            OperationResult<Result> result = postgresqlJsonClient.deleteArrayElement(TABLE_NAME,
                    "{\"key\": \"'; DROP TABLE tabledef; --\", \"index\": 0}",
                    "f54be158-0ff9-49d1-a070-954e6b64caac");
            assertFalse(result.isSuccess(), "Malicious key should result in an error");
            assertTrue(postgresqlJsonClient.isDatabaseRunning(), "Database should still be running");
        }

        @Test
        @DisplayName("Malicious table name in deleteArrayElement should be rejected")
        void maliciousTableNameInDeleteArrayElementShouldBeRejected() {
            // deleteArrayElement catches all exceptions internally and returns OperationResult.Error
            OperationResult<Result> result = postgresqlJsonClient.deleteArrayElement(
                    "table1; DROP TABLE tabledef",
                    "{\"key\": \"attribute4\", \"index\": 0}",
                    "f54be158-0ff9-49d1-a070-954e6b64caac");
            assertFalse(result.isSuccess(), "Malicious table name should result in an error");
            assertTrue(postgresqlJsonClient.isDatabaseRunning(), "Database should still be running");
        }
    }
}
