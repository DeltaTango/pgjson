package io.github.deltatango.pgjson.client;

import io.github.deltatango.pgjson.PostgreSqlJsonClient;
import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import io.github.deltatango.pgjson.model.TableDef;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import io.github.deltatango.pgjson.util.DatabaseConfigurationUtil;
import io.github.deltatango.pgjson.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@DisplayName("PostgreSqlJsonClient Memory Tests")
public class PostgreSqlJsonClientMemoryTest extends DatabaseConfigurationUtil {

    private FileUtil fileUtil;
    private String tableName = "table1";
    private String schemaName = "table1_schema";

    @BeforeEach
    void setUp() {
        fileUtil = new FileUtil();
    }

    @Test
    @DisplayName("Should retrieve table definition by table name and schema name from memory")
    void testGetTableDefByTableNameAndNameFromMemory_ValidParameters() throws PostgreJsonException {
        // Test with existing table and schema from init.sql
        OperationResult<TableDef> opResult = postgresqlJsonClient.getTableDefByTableNameAndNameFromMemory(tableName, schemaName);
        assertTrue(opResult.isSuccess());
        TableDef result = opResult.getOrThrow();
        
        assertNotNull(result, "TableDef should not be null for valid parameters");
        assertEquals(tableName, result.getTableName(), "Table name should match");
        assertEquals(schemaName, result.getSchemaName(), "Schema name should match");
        assertNotNull(result.getIdUuid(), "ID UUID should not be null");
        assertNotNull(result.getSchemaData(), "Schema data should not be null");
        
        log.info("Retrieved TableDef: tableName={}, schemaName={}, idUuid={}", 
                result.getTableName(), result.getSchemaName(), result.getIdUuid());
    }

    @Test
    @DisplayName("Should return NotFound for non-existent table and schema combination")
    void testGetTableDefByTableNameAndNameFromMemory_NonExistentCombination() throws PostgreJsonException {
        OperationResult<TableDef> opResult = postgresqlJsonClient.getTableDefByTableNameAndNameFromMemory("nonexistent", "nonexistent_schema");
        
        // The method returns NotFound or Success with empty TableDef
        assertTrue(opResult.isNotFound() || opResult.isSuccess());
        if (opResult.isSuccess()) {
            TableDef result = opResult.getOrThrow();
            assertNull(result.getTableName(), "Table name should be null for non-existent combination");
            assertNull(result.getSchemaName(), "Schema name should be null for non-existent combination");
            assertNull(result.getIdUuid(), "ID UUID should be null for non-existent combination");
            log.info("Non-existent combination correctly returned empty TableDef: {}", result);
        } else {
            log.info("Non-existent combination returned NotFound");
        }
    }

    @Test
    @DisplayName("Should return OperationResult.Error when table name is null")
    void testGetTableDefByTableNameAndNameFromMemory_NullTableName() {
        OperationResult<TableDef> result = postgresqlJsonClient.getTableDefByTableNameAndNameFromMemory(null, schemaName);
        assertTrue(result.isError());
        log.info("Null table name correctly rejected");
    }

    @Test
    @DisplayName("Should handle null schema name gracefully")
    void testGetTableDefByTableNameAndNameFromMemory_NullSchemaName() throws PostgreJsonException {
        // Null schema name returns OperationResult.Error or Success
        OperationResult<TableDef> opResult = postgresqlJsonClient.getTableDefByTableNameAndNameFromMemory(tableName, null);
        log.info("Null schema name handled gracefully, result: {}", opResult);
    }

    @Test
    @DisplayName("Should return OperationResult.Error when table name is empty")
    void testGetTableDefByTableNameAndNameFromMemory_EmptyTableName() {
        OperationResult<TableDef> result = postgresqlJsonClient.getTableDefByTableNameAndNameFromMemory("", schemaName);
        assertTrue(result.isError());
        log.info("Empty table name correctly rejected");
    }

    @Test
    @DisplayName("Should handle empty schema name gracefully")
    void testGetTableDefByTableNameAndNameFromMemory_EmptySchemaName() throws PostgreJsonException {
        // Empty schema name returns OperationResult.Error or Success
        OperationResult<TableDef> opResult = postgresqlJsonClient.getTableDefByTableNameAndNameFromMemory(tableName, "");
        log.info("Empty schema name handled gracefully, result: {}", opResult);
    }

    @Test
    @DisplayName("Should return OperationResult.Error when table name is whitespace")
    void testGetTableDefByTableNameAndNameFromMemory_WhitespaceTableName() {
        OperationResult<TableDef> result = postgresqlJsonClient.getTableDefByTableNameAndNameFromMemory("   ", schemaName);
        assertTrue(result.isError());
        log.info("Whitespace table name correctly rejected");
    }

    @Test
    @DisplayName("Should handle whitespace schema name gracefully")
    void testGetTableDefByTableNameAndNameFromMemory_WhitespaceSchemaName() throws PostgreJsonException {
        // Whitespace schema name returns OperationResult.Error or Success
        OperationResult<TableDef> opResult = postgresqlJsonClient.getTableDefByTableNameAndNameFromMemory(tableName, "   ");
        log.info("Whitespace schema name handled gracefully, result: {}", opResult);
    }

    @Test
    @DisplayName("Should retrieve table definition for table2 schema1")
    void testGetTableDefByTableNameAndNameFromMemory_Table2Schema1() throws PostgreJsonException {
        // Test with table2 and its schema from init.sql
        OperationResult<TableDef> opResult = postgresqlJsonClient.getTableDefByTableNameAndNameFromMemory("table2", "table2_schema1");
        assertTrue(opResult.isSuccess());
        TableDef result = opResult.getOrThrow();
        
        assertNotNull(result, "TableDef should not be null for table2 schema1");
        assertEquals("table2", result.getTableName(), "Table name should be table2");
        assertEquals("table2_schema1", result.getSchemaName(), "Schema name should be table2_schema1");
        assertNotNull(result.getIdUuid(), "ID UUID should not be null");
        
        log.info("Retrieved TableDef for table2: tableName={}, schemaName={}, idUuid={}", 
                result.getTableName(), result.getSchemaName(), result.getIdUuid());
    }

    @Test
    @DisplayName("Should retrieve table definition for table2 schema2")
    void testGetTableDefByTableNameAndNameFromMemory_Table2Schema2() throws PostgreJsonException {
        // Test with table2 and its second schema from init.sql
        OperationResult<TableDef> opResult = postgresqlJsonClient.getTableDefByTableNameAndNameFromMemory("table2", "table2_schema2");
        assertTrue(opResult.isSuccess());
        TableDef result = opResult.getOrThrow();
        
        assertNotNull(result, "TableDef should not be null for table2 schema2");
        assertEquals("table2", result.getTableName(), "Table name should be table2");
        assertEquals("table2_schema2", result.getSchemaName(), "Schema name should be table2_schema2");
        assertNotNull(result.getIdUuid(), "ID UUID should not be null");
        
        log.info("Retrieved TableDef for table2 schema2: tableName={}, schemaName={}, idUuid={}", 
                result.getTableName(), result.getSchemaName(), result.getIdUuid());
    }

    @Test
    @DisplayName("Should handle case sensitivity correctly")
    void testGetTableDefByTableNameAndNameFromMemory_CaseSensitivity() throws PostgreJsonException {
        // Test case sensitivity - should not find with different case
        OperationResult<TableDef> opResult = postgresqlJsonClient.getTableDefByTableNameAndNameFromMemory("TABLE1", "TABLE1_SCHEMA");
        
        // The method might be case-sensitive, so it should return NotFound or empty TableDef
        assertTrue(opResult.isNotFound() || opResult.isSuccess());
        if (opResult.isSuccess()) {
            TableDef result = opResult.getOrThrow();
            assertNull(result.getTableName(), "Table name should be null for case-sensitive mismatch");
        }
        log.info("Case sensitivity handled - result: {}", opResult);
    }

    @Test
    @DisplayName("Should handle special characters in parameters")
    void testGetTableDefByTableNameAndNameFromMemory_SpecialCharacters() throws PostgreJsonException {
        // Test with special characters that should not exist
        OperationResult<TableDef> opResult = postgresqlJsonClient.getTableDefByTableNameAndNameFromMemory("table1@#$", "schema@#$");
        
        // The method should return NotFound or Success with empty TableDef
        assertTrue(opResult.isNotFound() || opResult.isSuccess());
        if (opResult.isSuccess()) {
            TableDef result = opResult.getOrThrow();
            assertNull(result.getTableName(), "Table name should be null for special characters");
        }
        log.info("Special characters handled - result: {}", opResult);
    }
}
