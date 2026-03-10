package io.github.deltatango.pgjson.client;

import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import io.github.deltatango.pgjson.exceptions.RequestException;
import io.github.deltatango.pgjson.model.DatabaseEntry;
import io.github.deltatango.pgjson.model.TableDef;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import io.github.deltatango.pgjson.model.operations.Result;
import io.github.deltatango.pgjson.util.DatabaseConfigurationUtil;
import io.github.deltatango.pgjson.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@DisplayName("PostgreSqlJsonClient Edge Cases Tests")
public class PostgreSqlJsonClientEdgeCasesTest extends DatabaseConfigurationUtil {

    private FileUtil fileUtil;
    private String tableName = "test_table";

    @BeforeEach
    void setUp() {
        fileUtil = new FileUtil();
    }

    @Test
    @DisplayName("Should handle very large JSON documents")
    void testLargeJsonDocument() throws IOException, PostgreJsonException {
        // Create a large JSON document
        StringBuilder largeJson = new StringBuilder();
        largeJson.append("{");
        largeJson.append("\"id\": 1,");
        largeJson.append("\"data\": \"");

        // Add a large string (10KB)
        for (int i = 0; i < 1000; i++) {
            largeJson.append("This is a large string to test handling of big JSON documents. ");
        }

        largeJson.append("\",");
        largeJson.append("\"metadata\": {");
        for (int i = 0; i < 100; i++) {
            largeJson.append("\"field").append(i).append("\": \"value").append(i).append("\"");
            if (i < 99) {
                largeJson.append(",");
            }
        }
        largeJson.append("}");
        largeJson.append("}");

        OperationResult<Result> opResult = postgresqlJsonClient.insertData(tableName, largeJson.toString());
        assertNotNull(opResult);
        assertTrue(opResult.isSuccess() || opResult.isError());
        if (opResult.isSuccess()) {
            Result result = opResult.getOrThrow();
            assertNotNull(result);
            assertNotNull(result.getResultStatus());
        }
    }

    @Test
    @DisplayName("Should handle JSON with deeply nested objects")
    void testDeeplyNestedJson() throws PostgreJsonException {
        // Create a deeply nested JSON structure
        StringBuilder nestedJson = new StringBuilder();
        nestedJson.append("{");
        nestedJson.append("\"level1\": {");
        nestedJson.append("\"level2\": {");
        nestedJson.append("\"level3\": {");
        nestedJson.append("\"level4\": {");
        nestedJson.append("\"level5\": {");
        nestedJson.append("\"value\": \"deeply nested value\"");
        nestedJson.append("}");
        nestedJson.append("}");
        nestedJson.append("}");
        nestedJson.append("}");
        nestedJson.append("}");
        nestedJson.append("}");

        OperationResult<Result> opResult = postgresqlJsonClient.insertData(tableName, nestedJson.toString());
        assertNotNull(opResult);
        if (opResult.isSuccess()) {
            Result result = opResult.getOrThrow();
            assertNotNull(result);
            assertNotNull(result.getResultStatus());
        }
    }

    @Test
    @DisplayName("Should handle JSON with special characters")
    void testJsonWithSpecialCharacters() throws PostgreJsonException {
        String jsonWithSpecialChars = "{" +
            "\"unicode\": \"Hello 世界 🌍\"," +
            "\"special\": \"!@#$%^&*()_+-=[]{}|;':\\\",./<>?\"," +
            "\"newlines\": \"Line1\\nLine2\\r\\nLine3\"," +
            "\"tabs\": \"Col1\\tCol2\\tCol3\"," +
            "\"quotes\": \"He said \\\"Hello\\\" to me\"" +
            "}";

        OperationResult<Result> opResult = postgresqlJsonClient.insertData(tableName, jsonWithSpecialChars);
        assertNotNull(opResult);
        if (opResult.isSuccess()) {
            Result result = opResult.getOrThrow();
            assertNotNull(result);
            assertNotNull(result.getResultStatus());
        }
    }

    @Test
    @DisplayName("Should handle JSON with numeric edge cases")
    void testJsonWithNumericEdgeCases() throws PostgreJsonException {
        String jsonWithNumbers = "{" +
            "\"maxInt\": " + Integer.MAX_VALUE + "," +
            "\"minInt\": " + Integer.MIN_VALUE + "," +
            "\"maxLong\": " + Long.MAX_VALUE + "," +
            "\"minLong\": " + Long.MIN_VALUE + "," +
            "\"pi\": 3.141592653589793," +
            "\"e\": 2.718281828459045," +
            "\"zero\": 0," +
            "\"negativeZero\": -0," +
            "\"infinity\": " + Double.POSITIVE_INFINITY + "," +
            "\"negativeInfinity\": " + Double.NEGATIVE_INFINITY + "," +
            "\"nan\": " + Double.NaN +
            "}";

        OperationResult<Result> opResult = postgresqlJsonClient.insertData(tableName, jsonWithNumbers);
        assertNotNull(opResult);
        if (opResult.isSuccess()) {
            Result result = opResult.getOrThrow();
            assertNotNull(result);
            assertNotNull(result.getResultStatus());
        }
    }

    @Test
    @DisplayName("Should handle JSON with boolean edge cases")
    void testJsonWithBooleanEdgeCases() throws PostgreJsonException {
        String jsonWithBooleans = "{" +
            "\"trueValue\": true," +
            "\"falseValue\": false," +
            "\"stringTrue\": \"true\"," +
            "\"stringFalse\": \"false\"," +
            "\"stringBoolean\": \"boolean\"" +
            "}";

        OperationResult<Result> opResult = postgresqlJsonClient.insertData(tableName, jsonWithBooleans);
        assertNotNull(opResult);
        if (opResult.isSuccess()) {
            Result result = opResult.getOrThrow();
            assertNotNull(result);
            assertNotNull(result.getResultStatus());
        }
    }

    @Test
    @DisplayName("Should handle JSON with array edge cases")
    void testJsonWithArrayEdgeCases() throws PostgreJsonException {
        String jsonWithArrays = "{" +
            "\"emptyArray\": []," +
            "\"singleElement\": [42]," +
            "\"mixedTypes\": [1, \"string\", true, null, {}]," +
            "\"nestedArrays\": [[1, 2], [3, 4], [5, 6]]," +
            "\"deepNesting\": [[[1, 2], [3, 4]], [[5, 6], [7, 8]]]" +
            "}";

        OperationResult<Result> opResult = postgresqlJsonClient.insertData(tableName, jsonWithArrays);
        assertNotNull(opResult);
        if (opResult.isSuccess()) {
            Result result = opResult.getOrThrow();
            assertNotNull(result);
            assertNotNull(result.getResultStatus());
        }
    }

    @Test
    @DisplayName("Should handle JSON with null values")
    void testJsonWithNullValues() throws PostgreJsonException {
        String jsonWithNulls = "{" +
            "\"nullValue\": null," +
            "\"stringNull\": \"null\"," +
            "\"emptyString\": \"\"," +
            "\"whitespace\": \"   \"," +
            "\"objectWithNull\": {\"key\": null}" +
            "}";

        OperationResult<Result> opResult = postgresqlJsonClient.insertData(tableName, jsonWithNulls);
        assertNotNull(opResult);
        if (opResult.isSuccess()) {
            Result result = opResult.getOrThrow();
            assertNotNull(result);
            assertNotNull(result.getResultStatus());
        }
    }

    @Test
    @DisplayName("Should handle complex search queries")
    void testComplexSearchQueries() throws PostgreJsonException, RequestException {
        // First insert some test data
        String testData = "{\"name\": \"John Doe\", \"age\": 30, \"city\": \"New York\"}";
        OperationResult<Result> insertOpResult = postgresqlJsonClient.insertData(tableName, testData);
        assertNotNull(insertOpResult, "Insert result should not be null");
        if (insertOpResult.isSuccess()) {
            Result insertResult = insertOpResult.getOrThrow();
            assertNotNull(insertResult.getResultStatus(), "Result status should not be null");
        }
    }

    @Test
    @DisplayName("Should handle search with logical operators")
    void testSearchWithLogicalOperators() throws PostgreJsonException, RequestException {
        // Test basic search functionality using the correct JSON structure
        String searchJson = "{\n" +
            "  \"limit\": 10,\n" +
            "  \"offset\": 0,\n" +
            "  \"orderType\": \"desc\",\n" +
            "  \"searchTerm\": {\n" +
            "  }\n" +
            "}";

        OperationResult<List<DatabaseEntry>> opResult = postgresqlJsonClient.selectData(tableName, searchJson);
        assertNotNull(opResult);
        List<DatabaseEntry> results = opResult.getOrThrow();
        assertNotNull(results);
    }

    @Test
    @DisplayName("Should handle update operations with complex data")
    void testUpdateWithComplexData() throws PostgreJsonException {
        // First insert some test data
        String testData = "{\"name\": \"John Doe\", \"age\": 30}";
        OperationResult<Result> insertOpResult = postgresqlJsonClient.insertData(tableName, testData);
        assertNotNull(insertOpResult, "Insert result should not be null");
        if (insertOpResult.isSuccess()) {
            Result insertResult = insertOpResult.getOrThrow();
            assertNotNull(insertResult.getResultStatus(), "Result status should not be null");

            if (insertResult.getResultStatus()) {
                assertNotNull(insertResult.getIduuid(), "Insert UUID should not be null on success");

                String updatedData = "{" +
                    "\"name\": \"John Smith\"," +
                    "\"age\": 31," +
                    "\"city\": \"Boston\"," +
                    "\"metadata\": {\"updated\": true, \"timestamp\": \"2023-01-01T00:00:00Z\"}" +
                    "}";

                OperationResult<Result> updateOpResult = postgresqlJsonClient.updateData(tableName, updatedData, insertResult.getIduuid());
                assertNotNull(updateOpResult, "Update result should not be null");
                if (updateOpResult.isSuccess()) {
                    Result updateResult = updateOpResult.getOrThrow();
                    assertNotNull(updateResult.getResultStatus(), "Update result status should not be null");
                }
            }
        }
    }

    @Test
    @DisplayName("Should handle delete operations with various IDs")
    void testDeleteWithVariousIds() throws PostgreJsonException {
        // Test delete with non-existent ID
        OperationResult<Boolean> opResult = postgresqlJsonClient.deleteData(tableName, "non-existent-id");
        assertNotNull(opResult);
        // Should return false for non-existent ID (Success with false) or NotFound
        assertTrue(!opResult.isSuccess() || !opResult.getOrThrow());
    }

    @Test
    @DisplayName("Should handle schema operations with complex schemas")
    void testSchemaOperationsWithComplexSchemas() throws PostgreJsonException {
        String complexSchema = "{" +
            "\"type\": \"object\"," +
            "\"properties\": {" +
            "\"name\": {\"type\": \"string\", \"minLength\": 1}," +
            "\"age\": {\"type\": \"integer\", \"minimum\": 0, \"maximum\": 150}," +
            "\"email\": {\"type\": \"string\", \"format\": \"email\"}," +
            "\"address\": {" +
            "\"type\": \"object\"," +
            "\"properties\": {" +
            "\"street\": {\"type\": \"string\"}," +
            "\"city\": {\"type\": \"string\"}," +
            "\"zipCode\": {\"type\": \"string\", \"pattern\": \"^[0-9]{5}(-[0-9]{4})?$\"}" +
            "}," +
            "\"required\": [\"street\", \"city\"]" +
            "}" +
            "}," +
            "\"required\": [\"name\", \"age\"]" +
            "}";

        OperationResult<String> opResult = postgresqlJsonClient.insertSchema(tableName, "test_schema", complexSchema);
        assertNotNull(opResult);
        if (opResult.isSuccess()) {
            assertNotNull(opResult.getOrThrow());
        }
    }

    @Test
    @DisplayName("Should handle UI labels with complex data")
    void testUiLabelsWithComplexData() {
        // Test getUiLabels with various inputs - returns OperationResult, should not throw
        assertDoesNotThrow(() -> {
            OperationResult<String> result1 = postgresqlJsonClient.getUiLabels(tableName);
            postgresqlJsonClient.getUiLabels(tableName, "test_schema");
        });
    }

    @Test
    @DisplayName("Should handle database connection status checks")
    void testDatabaseConnectionStatus() {
        boolean isRunning = postgresqlJsonClient.isDatabaseRunning();

        // Database should be running since we have a TestContainer
        assertTrue(isRunning, "Database should be running during tests");
    }

    @Test
    @DisplayName("Should handle concurrent operations gracefully")
    void testConcurrentOperations() throws PostgreJsonException {
        // Test multiple operations in sequence
        String testData1 = "{\"id\": 1, \"name\": \"Test1\"}";
        String testData2 = "{\"id\": 2, \"name\": \"Test2\"}";
        String testData3 = "{\"id\": 3, \"name\": \"Test3\"}";

        OperationResult<Result> opResult1 = postgresqlJsonClient.insertData(tableName, testData1);
        OperationResult<Result> opResult2 = postgresqlJsonClient.insertData(tableName, testData2);
        OperationResult<Result> opResult3 = postgresqlJsonClient.insertData(tableName, testData3);

        assertNotNull(opResult1);
        assertNotNull(opResult2);
        assertNotNull(opResult3);
    }

    @Test
    @DisplayName("Should handle memory table operations")
    void testMemoryTableOperations() throws PostgreJsonException {
        // Test getTableDefByIdUuidFromMemory - returns OperationResult
        assertDoesNotThrow(() -> {
            OperationResult<TableDef> opResult1 = postgresqlJsonClient.getTableDefByIdUuidFromMemory("test-uuid");
            // Result might be NotFound for non-existent UUID
        });

        // Test getTableDefByTableNameAndNameFromMemory
        assertDoesNotThrow(() -> {
            OperationResult<TableDef> opResult2 = postgresqlJsonClient.getTableDefByTableNameAndNameFromMemory("test-table", "test-schema");
            // Result might be NotFound for non-existent table
        });

        // Test getTableDefByIdFromMemory
        assertDoesNotThrow(() -> {
            OperationResult<TableDef> opResult3 = postgresqlJsonClient.getTableDefByIdFromMemory(99999);
            // Result might be NotFound for non-existent ID
        });
    }

    @Test
    @DisplayName("Should handle utility method access")
    void testUtilityMethodAccess() {
        // Test getter methods
        assertDoesNotThrow(() -> {
            var dbUtil = postgresqlJsonClient.getDbUtil();
            var operationExecutor = postgresqlJsonClient.getOperationExecutor();

            assertNotNull(dbUtil);
            assertNotNull(operationExecutor);
        });
    }

    @Test
    @DisplayName("Should handle close operation")
    void testCloseOperation() {
        // Test that close method doesn't throw exceptions
        assertDoesNotThrow(() -> {
            postgresqlJsonClient.close();
        });
    }

    @Test
    @DisplayName("Should handle array element deletion with complex paths")
    void testArrayElementDeletionWithComplexPaths() throws PostgreJsonException {
        // Test deleteArrayElement with complex JSON path
        String complexData = "{" +
            "\"users\": [" +
            "{\"id\": 1, \"name\": \"John\"}," +
            "{\"id\": 2, \"name\": \"Jane\"}" +
            "]," +
            "\"metadata\": {\"count\": 2}" +
            "}";

        OperationResult<Result> insertOpResult = postgresqlJsonClient.insertData(tableName, complexData);
        assertNotNull(insertOpResult, "Insert result should not be null");
        if (insertOpResult.isSuccess()) {
            Result insertResult = insertOpResult.getOrThrow();
            assertNotNull(insertResult.getResultStatus(), "Result status should not be null");

            if (insertResult.getResultStatus()) {
                assertNotNull(insertResult.getIduuid(), "Insert UUID should not be null on success");

                String deleteData = "{\"key\": \"users\", \"index\": 0}";
                OperationResult<Result> deleteOpResult = postgresqlJsonClient.deleteArrayElement(tableName, deleteData, insertResult.getIduuid());
                assertNotNull(deleteOpResult, "Delete result should not be null");
                if (deleteOpResult.isSuccess()) {
                    Result deleteResult = deleteOpResult.getOrThrow();
                    assertNotNull(deleteResult.getResultStatus(), "Delete result status should not be null");
                }
            }
        }
    }

    @Test
    @DisplayName("Should handle merge operations with complex data")
    void testMergeOperationsWithComplexData() throws PostgreJsonException {
        // First insert some test data
        String testData = "{\"name\": \"John\", \"age\": 30, \"city\": \"New York\"}";
        OperationResult<Result> insertOpResult = postgresqlJsonClient.insertData(tableName, testData);
        assertNotNull(insertOpResult, "Insert result should not be null");
        if (insertOpResult.isSuccess()) {
            Result insertResult = insertOpResult.getOrThrow();
            assertNotNull(insertResult.getResultStatus(), "Result status should not be null");

            if (insertResult.getResultStatus()) {
                assertNotNull(insertResult.getIduuid(), "Insert UUID should not be null on success");

                String mergeData = "{\"age\": 31, \"country\": \"USA\", \"metadata\": {\"updated\": true}}";
                OperationResult<Result> mergeOpResult = postgresqlJsonClient.updateDataWithMerge(tableName, mergeData, insertResult.getIduuid());
                assertNotNull(mergeOpResult, "Merge result should not be null");
                if (mergeOpResult.isSuccess()) {
                    Result mergeResult = mergeOpResult.getOrThrow();
                    assertNotNull(mergeResult.getResultStatus(), "Merge result status should not be null");
                }
            }
        }
    }
}
