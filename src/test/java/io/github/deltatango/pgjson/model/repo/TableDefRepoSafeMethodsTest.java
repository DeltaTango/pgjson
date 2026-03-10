package io.github.deltatango.pgjson.model.repo;

import io.github.deltatango.pgjson.model.TableDef;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import io.github.deltatango.pgjson.util.DatabaseConfigurationUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@DisplayName("TableDefRepo OperationResult Methods Tests")
public class TableDefRepoSafeMethodsTest extends DatabaseConfigurationUtil {

    private TableDefRepo tableDefRepo;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        tableDefRepo = new TableDefRepo();
        connection = postgresqlJsonClient.getDbUtil().getConnection();
    }

    @AfterEach
    void tearDown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.warn("Error closing connection after test", e);
            }
            connection = null;
        }
    }

    // ---- selectTableDefByUuid ----

    @Test
    @DisplayName("selectTableDefByUuid should return Error when UUID is null")
    void testSelectTableDefByUuidWithNullUuid() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByUuid(connection, null);

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("selectTableDefByUuid should return Error when UUID is empty")
    void testSelectTableDefByUuidWithEmptyUuid() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByUuid(connection, "");

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
    }

    @Test
    @DisplayName("selectTableDefByUuid should return Error when UUID is whitespace")
    void testSelectTableDefByUuidWithWhitespaceUuid() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByUuid(connection, "   ");

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
    }

    @Test
    @DisplayName("selectTableDefByUuid should return NotFound when UUID is invalid")
    void testSelectTableDefByUuidWithInvalidUuid() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByUuid(connection, "invalid-uuid");

        assertNotNull(result);
        assertInstanceOf(OperationResult.NotFound.class, result);
        assertTrue(result.isNotFound());
    }

    @Test
    @DisplayName("selectTableDefByUuid should return NotFound for non-existent UUID")
    void testSelectTableDefByUuidWithNonExistentUuid() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByUuid(connection, "non-existent-uuid");

        assertNotNull(result);
        assertInstanceOf(OperationResult.NotFound.class, result);
    }

    // ---- selectTableDefById ----

    @Test
    @DisplayName("selectTableDefById should return Error when ID is null")
    void testSelectTableDefByIdWithNullId() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefById(connection, null);

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
    }

    @Test
    @DisplayName("selectTableDefById should return Error when ID is zero")
    void testSelectTableDefByIdWithZeroId() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefById(connection, 0);

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
    }

    @Test
    @DisplayName("selectTableDefById should return Error when ID is negative")
    void testSelectTableDefByIdWithNegativeId() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefById(connection, -1);

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
    }

    @Test
    @DisplayName("selectTableDefById should return NotFound when ID doesn't exist")
    void testSelectTableDefByIdWithNonExistentId() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefById(connection, 99999);

        assertNotNull(result);
        assertInstanceOf(OperationResult.NotFound.class, result);
    }

    @Test
    @DisplayName("selectTableDefById should return NotFound for large non-existent ID")
    void testSelectTableDefByIdWithLargeNonExistentId() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefById(connection, 99999);

        assertNotNull(result);
        assertInstanceOf(OperationResult.NotFound.class, result);
    }

    // ---- selectTableDefByTableName ----

    @Test
    @DisplayName("selectTableDefByTableName should return Error when tableName is null")
    void testSelectTableDefByTableNameWithNullTableName() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByTableName(connection, null);

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
    }

    @Test
    @DisplayName("selectTableDefByTableName should return Error when tableName is empty")
    void testSelectTableDefByTableNameWithEmptyTableName() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByTableName(connection, "");

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
    }

    @Test
    @DisplayName("selectTableDefByTableName should return Error when tableName is whitespace")
    void testSelectTableDefByTableNameWithWhitespaceTableName() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByTableName(connection, "   ");

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
    }

    @Test
    @DisplayName("selectTableDefByTableName should return NotFound when tableName doesn't exist")
    void testSelectTableDefByTableNameWithNonExistentTableName() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByTableName(connection, "non_existent_table");

        assertNotNull(result);
        assertInstanceOf(OperationResult.NotFound.class, result);
    }

    @Test
    @DisplayName("selectTableDefByTableName should return NotFound for non-existent table")
    void testSelectTableDefByTableNameNotFound() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByTableName(connection, "non_existent_table");

        assertNotNull(result);
        assertInstanceOf(OperationResult.NotFound.class, result);
    }

    // ---- selectTableDefByTableNameAndName ----

    @Test
    @DisplayName("selectTableDefByTableNameAndName should return Error when tableName is null")
    void testSelectTableDefByTableNameAndNameWithNullTableName() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByTableNameAndName(connection, null, "schema");

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
    }

    @Test
    @DisplayName("selectTableDefByTableNameAndName should return Error when tableName is empty")
    void testSelectTableDefByTableNameAndNameWithEmptyTableName() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByTableNameAndName(connection, "", "schema");

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
    }

    @Test
    @DisplayName("selectTableDefByTableNameAndName should return Error when tableName is whitespace")
    void testSelectTableDefByTableNameAndNameWithWhitespaceTableName() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByTableNameAndName(connection, "   ", "schema");

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
    }

    @Test
    @DisplayName("selectTableDefByTableNameAndName should return Error when schemaName is null")
    void testSelectTableDefByTableNameAndNameWithNullSchemaName() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByTableNameAndName(connection, "table", null);

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
    }

    @Test
    @DisplayName("selectTableDefByTableNameAndName should return Error when schemaName is empty")
    void testSelectTableDefByTableNameAndNameWithEmptySchemaName() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByTableNameAndName(connection, "table", "");

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
    }

    @Test
    @DisplayName("selectTableDefByTableNameAndName should return Error when schemaName is whitespace")
    void testSelectTableDefByTableNameAndNameWithWhitespaceSchemaName() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByTableNameAndName(connection, "table", "   ");

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
    }

    @Test
    @DisplayName("selectTableDefByTableNameAndName should return Error when both parameters are null")
    void testSelectTableDefByTableNameAndNameWithBothNull() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByTableNameAndName(connection, null, null);

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
    }

    @Test
    @DisplayName("selectTableDefByTableNameAndName should return Error when both parameters are empty")
    void testSelectTableDefByTableNameAndNameWithBothEmpty() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByTableNameAndName(connection, "", "");

        assertNotNull(result);
        assertInstanceOf(OperationResult.Error.class, result);
    }

    @Test
    @DisplayName("selectTableDefByTableNameAndName should return NotFound when table doesn't exist")
    void testSelectTableDefByTableNameAndNameWithNonExistentTable() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByTableNameAndName(connection, "non_existent_table", "non_existent_schema");

        assertNotNull(result);
        assertInstanceOf(OperationResult.NotFound.class, result);
    }

    @Test
    @DisplayName("selectTableDefByTableNameAndName should return NotFound for non-existent data")
    void testSelectTableDefByTableNameAndNameNotFound() {
        OperationResult<TableDef> result = tableDefRepo.selectTableDefByTableNameAndName(connection, "non_existent_table", "non_existent_schema");

        assertNotNull(result);
        assertInstanceOf(OperationResult.NotFound.class, result);
    }

    // ---- Cross-cutting concerns ----

    @Test
    @DisplayName("All methods should handle null connection gracefully")
    void testMethodsWithNullConnection() {
        assertDoesNotThrow(() -> {
            OperationResult<TableDef> result1 = tableDefRepo.selectTableDefByUuid(null, "test-uuid");
            OperationResult<TableDef> result2 = tableDefRepo.selectTableDefById(null, 1);
            OperationResult<TableDef> result3 = tableDefRepo.selectTableDefByTableName(null, "test-table");
            OperationResult<TableDef> result4 = tableDefRepo.selectTableDefByTableNameAndName(null, "test-table", "test-schema");

            // All should return Error due to null connection
            assertInstanceOf(OperationResult.Error.class, result1);
            assertInstanceOf(OperationResult.Error.class, result2);
            assertInstanceOf(OperationResult.Error.class, result3);
            assertInstanceOf(OperationResult.Error.class, result4);
        });
    }

    @Test
    @DisplayName("Methods should return consistent OperationResult types")
    void testMethodsReturnConsistentOperationResultTypes() {
        OperationResult<TableDef> result1 = tableDefRepo.selectTableDefByUuid(connection, "test-uuid");
        OperationResult<TableDef> result2 = tableDefRepo.selectTableDefById(connection, 99998);
        OperationResult<TableDef> result3 = tableDefRepo.selectTableDefByTableName(connection, "test-table");
        OperationResult<TableDef> result4 = tableDefRepo.selectTableDefByTableNameAndName(connection, "test-table", "test-schema");

        // All should be OperationResult instances (not null)
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertNotNull(result4);

        // All should be NotFound for non-existent data
        assertInstanceOf(OperationResult.NotFound.class, result1);
        assertInstanceOf(OperationResult.NotFound.class, result2);
        assertInstanceOf(OperationResult.NotFound.class, result3);
        assertInstanceOf(OperationResult.NotFound.class, result4);
    }

    @Test
    @DisplayName("Methods should handle SQL injection attempts gracefully")
    void testMethodsWithSqlInjectionAttempts() {
        String sqlInjection = "'; DROP TABLE users; --";

        OperationResult<TableDef> result1 = tableDefRepo.selectTableDefByUuid(connection, sqlInjection);
        OperationResult<TableDef> result2 = tableDefRepo.selectTableDefByTableName(connection, sqlInjection);
        OperationResult<TableDef> result3 = tableDefRepo.selectTableDefByTableNameAndName(connection, sqlInjection, sqlInjection);

        // Should return NotFound without throwing exceptions
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertFalse(result1.isSuccess());
        assertFalse(result2.isSuccess());
        assertFalse(result3.isSuccess());
    }

    @Test
    @DisplayName("Methods should handle very long strings gracefully")
    void testMethodsWithVeryLongStrings() {
        String veryLongString = "a".repeat(10000);

        OperationResult<TableDef> result1 = tableDefRepo.selectTableDefByUuid(connection, veryLongString);
        OperationResult<TableDef> result2 = tableDefRepo.selectTableDefByTableName(connection, veryLongString);
        OperationResult<TableDef> result3 = tableDefRepo.selectTableDefByTableNameAndName(connection, veryLongString, veryLongString);

        // Should handle long strings without throwing exceptions
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertFalse(result1.isSuccess());
        assertFalse(result2.isSuccess());
        assertFalse(result3.isSuccess());
    }

    @Test
    @DisplayName("Methods should handle special characters gracefully")
    void testMethodsWithSpecialCharacters() {
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

        OperationResult<TableDef> result1 = tableDefRepo.selectTableDefByUuid(connection, specialChars);
        OperationResult<TableDef> result2 = tableDefRepo.selectTableDefByTableName(connection, specialChars);
        OperationResult<TableDef> result3 = tableDefRepo.selectTableDefByTableNameAndName(connection, specialChars, specialChars);

        // Should handle special characters without throwing exceptions
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertFalse(result1.isSuccess());
        assertFalse(result2.isSuccess());
        assertFalse(result3.isSuccess());
    }
}
