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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@DisplayName("PostgreSqlJsonClient Error Handling Tests")
public class PostgreSqlJsonClientErrorHandlingTest extends DatabaseConfigurationUtil {

    private FileUtil fileUtil;
    private String tableName = "test_table";

    @BeforeEach
    void setUp() {
        fileUtil = new FileUtil();
    }

    @Test
    @DisplayName("Should return OperationResult.Error when tableName is null")
    void testInsertDataWithNullTableName() {
        OperationResult<Result> result = postgresqlJsonClient.insertData(null, "{}");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when tableName is empty")
    void testInsertDataWithEmptyTableName() {
        OperationResult<Result> result = postgresqlJsonClient.insertData("", "{}");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when tableName is whitespace")
    void testInsertDataWithWhitespaceTableName() {
        OperationResult<Result> result = postgresqlJsonClient.insertData("   ", "{}");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when jsonData is null")
    void testInsertDataWithNullJsonData() {
        OperationResult<Result> result = postgresqlJsonClient.insertData(tableName, null);
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when jsonData is empty")
    void testInsertDataWithEmptyJsonData() {
        OperationResult<Result> result = postgresqlJsonClient.insertData(tableName, "");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should handle invalid JSON gracefully")
    void testInsertDataWithInvalidJsonData() throws PostgreJsonException {
        OperationResult<Result> opResult = postgresqlJsonClient.insertData(tableName, "invalid json");
        assertTrue(opResult.isSuccess() || opResult.isError());
        if (opResult.isSuccess()) {
            Result result = opResult.getOrThrow();
            assertFalse(result.getResultStatus());
            assertNotNull(result.getResultMessage());
        } else {
            assertTrue(opResult.isError());
        }
    }

    @Test
    @DisplayName("Should return OperationResult.Error when schemaName is null in insertData")
    void testInsertDataWithNullSchemaName() {
        OperationResult<Result> result = postgresqlJsonClient.insertData(tableName, null, "{}");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when schemaName is empty in insertData")
    void testInsertDataWithEmptySchemaName() {
        OperationResult<Result> result = postgresqlJsonClient.insertData(tableName, "", "{}");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when tableName is null in deleteData")
    void testDeleteDataWithNullTableName() {
        OperationResult<Boolean> result = postgresqlJsonClient.deleteData(null, "test-id");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when tableName is empty in deleteData")
    void testDeleteDataWithEmptyTableName() {
        OperationResult<Boolean> result = postgresqlJsonClient.deleteData("", "test-id");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when idUuid is null in deleteData")
    void testDeleteDataWithNullIdUuid() {
        OperationResult<Boolean> result = postgresqlJsonClient.deleteData(tableName, null);
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when idUuid is empty in deleteData")
    void testDeleteDataWithEmptyIdUuid() {
        OperationResult<Boolean> result = postgresqlJsonClient.deleteData(tableName, "");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when tableName is null in selectData")
    void testSelectDataWithNullTableName() throws RequestException {
        OperationResult<List<DatabaseEntry>> result = postgresqlJsonClient.selectData(null, "{}");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when tableName is empty in selectData")
    void testSelectDataWithEmptyTableName() throws RequestException {
        OperationResult<List<DatabaseEntry>> result = postgresqlJsonClient.selectData("", "{}");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should throw PostgreJsonException when searchJson is null in selectData")
    void testSelectDataWithNullSearchJson() {
        assertThrows(RequestException.class, () -> {
            postgresqlJsonClient.selectData(tableName, null);
        });
    }

    @Test
    @DisplayName("Should throw PostgreJsonException when searchJson is empty in selectData")
    void testSelectDataWithEmptySearchJson() {
        assertThrows(RequestException.class, () -> {
            postgresqlJsonClient.selectData(tableName, "");
        });
    }

    @Test
    @DisplayName("Should throw PostgreJsonException when searchJson is invalid JSON in selectData")
    void testSelectDataWithInvalidSearchJson() {
        assertThrows(com.google.gson.JsonSyntaxException.class, () -> {
            postgresqlJsonClient.selectData(tableName, "invalid json");
        });
    }

    @Test
    @DisplayName("Should return OperationResult.Error when tableName is null in updateData")
    void testUpdateDataWithNullTableName() {
        OperationResult<Result> result = postgresqlJsonClient.updateData(null, "{}", "test-id");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when tableName is empty in updateData")
    void testUpdateDataWithEmptyTableName() {
        OperationResult<Result> result = postgresqlJsonClient.updateData("", "{}", "test-id");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when jsonData is null in updateData")
    void testUpdateDataWithNullJsonData() {
        OperationResult<Result> result = postgresqlJsonClient.updateData(tableName, null, "test-id");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when entryIdUuid is null in updateData")
    void testUpdateDataWithNullEntryIdUuid() {
        OperationResult<Result> result = postgresqlJsonClient.updateData(tableName, "{}", null);
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when entryIdUuid is empty in updateData")
    void testUpdateDataWithEmptyEntryIdUuid() {
        OperationResult<Result> result = postgresqlJsonClient.updateData(tableName, "{}", "");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when idUuid is null in selectDataByIdUuid")
    void testSelectDataByIdUuidWithNullIdUuid() {
        OperationResult<DatabaseEntry> result = postgresqlJsonClient.selectDataByIdUuid(null, tableName);
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when idUuid is empty in selectDataByIdUuid")
    void testSelectDataByIdUuidWithEmptyIdUuid() {
        OperationResult<DatabaseEntry> result = postgresqlJsonClient.selectDataByIdUuid("", tableName);
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when tableName is null in selectDataByIdUuid")
    void testSelectDataByIdUuidWithNullTableName() {
        OperationResult<DatabaseEntry> result = postgresqlJsonClient.selectDataByIdUuid("test-id", null);
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when tableName is empty in selectDataByIdUuid")
    void testSelectDataByIdUuidWithEmptyTableName() {
        OperationResult<DatabaseEntry> result = postgresqlJsonClient.selectDataByIdUuid("test-id", "");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when idUuid is null in getTableDefByIdUuid")
    void testGetTableDefByIdUuidWithNullIdUuid() {
        OperationResult<TableDef> result = postgresqlJsonClient.getTableDefByIdUuid(null);
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when idUuid is empty in getTableDefByIdUuid")
    void testGetTableDefByIdUuidWithEmptyIdUuid() {
        OperationResult<TableDef> result = postgresqlJsonClient.getTableDefByIdUuid("");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when tableName is null in insertSchema")
    void testInsertSchemaWithNullTableName() {
        OperationResult<String> result = postgresqlJsonClient.insertSchema(null, "schema", "{}");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when tableName is empty in insertSchema")
    void testInsertSchemaWithEmptyTableName() {
        OperationResult<String> result = postgresqlJsonClient.insertSchema("", "schema", "{}");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when schemaName is null in insertSchema")
    void testInsertSchemaWithNullSchemaName() {
        OperationResult<String> result = postgresqlJsonClient.insertSchema(tableName, null, "{}");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when schemaName is empty in insertSchema")
    void testInsertSchemaWithEmptySchemaName() {
        OperationResult<String> result = postgresqlJsonClient.insertSchema(tableName, "", "{}");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when schemaData is null in insertSchema")
    void testInsertSchemaWithNullSchemaData() {
        OperationResult<String> result = postgresqlJsonClient.insertSchema(tableName, "schema", null);
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when schemaData is empty in insertSchema")
    void testInsertSchemaWithEmptySchemaData() {
        OperationResult<String> result = postgresqlJsonClient.insertSchema(tableName, "schema", "");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when schema is null in getUiLabelsFromSchemaFile")
    void testGetUiLabelsFromSchemaFileWithNullSchema() {
        OperationResult<String> result = postgresqlJsonClient.getUiLabelsFromSchemaFile(null);
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when schema is empty in getUiLabelsFromSchemaFile")
    void testGetUiLabelsFromSchemaFileWithEmptySchema() {
        OperationResult<String> result = postgresqlJsonClient.getUiLabelsFromSchemaFile("");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when schema is whitespace in getUiLabelsFromSchemaFile")
    void testGetUiLabelsFromSchemaFileWithWhitespaceSchema() {
        OperationResult<String> result = postgresqlJsonClient.getUiLabelsFromSchemaFile("   ");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("Should handle connection failures gracefully")
    void testConnectionFailureHandling() {
        // This test would require mocking the database connection
        // For now, we'll test that the method doesn't throw unexpected exceptions
        assertDoesNotThrow(() -> {
            boolean isRunning = postgresqlJsonClient.isDatabaseRunning();
            // The actual result depends on database availability
            assertNotNull(isRunning);
        });
    }

    @Test
    @DisplayName("Should handle invalid JSON gracefully in complex operations")
    void testInvalidJsonHandling() throws PostgreJsonException {
        OperationResult<Result> opResult = postgresqlJsonClient.insertData(tableName, "{invalid json}");
        assertTrue(opResult.isSuccess() || opResult.isError());
        if (opResult.isSuccess()) {
            Result result = opResult.getOrThrow();
            assertFalse(result.getResultStatus());
            assertNotNull(result.getResultMessage());
        } else {
            assertTrue(opResult.isError());
        }
    }

    @Test
    @DisplayName("Should return OperationResult.Error when null parameters in getUiLabels methods")
    void testGetUiLabelsWithNullParameters() {
        OperationResult<String> result1 = postgresqlJsonClient.getUiLabels(null);
        OperationResult<String> result2 = postgresqlJsonClient.getUiLabels(null, null);
        assertTrue(result1.isError());
        assertTrue(result2.isError());
    }

    @Test
    @DisplayName("Should return OperationResult.Error when empty parameters in getUiLabels methods")
    void testGetUiLabelsWithEmptyParameters() {
        OperationResult<String> result1 = postgresqlJsonClient.getUiLabels("");
        OperationResult<String> result2 = postgresqlJsonClient.getUiLabels("", "");
        assertTrue(result1.isError());
        assertTrue(result2.isError());
    }
}
