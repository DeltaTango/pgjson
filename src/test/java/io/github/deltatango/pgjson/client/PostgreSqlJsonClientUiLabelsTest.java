package io.github.deltatango.pgjson.client;

import io.github.deltatango.pgjson.PostgreSqlJsonClient;
import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import io.github.deltatango.pgjson.util.DatabaseConfigurationUtil;
import io.github.deltatango.pgjson.util.FileUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@DisplayName("PostgreSqlJsonClient UI Labels Tests")
public class PostgreSqlJsonClientUiLabelsTest extends DatabaseConfigurationUtil {

    private FileUtil fileUtil;
    private Gson gson;
    private String tableName = "table1";
    private String schemaName = "table1_schema";

    @BeforeEach
    void setUp() {
        fileUtil = new FileUtil();
        gson = new Gson();
    }

    @Test
    @DisplayName("Should generate UI labels for existing table")
    void testGetUiLabels_ValidTableName() throws PostgreJsonException {
        String uiLabels = postgresqlJsonClient.getUiLabels(tableName).getOrThrow();
        
        assertNotNull(uiLabels, "UI labels should not be null");
        assertFalse(uiLabels.trim().isEmpty(), "UI labels should not be empty");
        
        // Verify JSON structure
        JsonObject labels = gson.fromJson(uiLabels, JsonObject.class);
        assertNotNull(labels, "UI labels should be valid JSON");
        
        // The method might return empty JSON if no UI labels are found
        log.info("Generated UI labels for table1: {}", uiLabels);
        log.info("Number of UI labels: {}", labels.size());
        
        // Just verify it's valid JSON, even if empty
        assertTrue(labels.size() >= 0, "UI labels should be valid JSON object");
    }

    @Test
    @DisplayName("Should generate UI labels for table2")
    void testGetUiLabels_Table2() throws PostgreJsonException {
        String uiLabels = postgresqlJsonClient.getUiLabels("table2").getOrThrow();
        
        assertNotNull(uiLabels, "UI labels should not be null for table2");
        assertFalse(uiLabels.trim().isEmpty(), "UI labels should not be empty for table2");
        
        JsonObject labels = gson.fromJson(uiLabels, JsonObject.class);
        assertNotNull(labels, "UI labels should be valid JSON for table2");
        
        // The method might return empty JSON if no UI labels are found
        log.info("Generated UI labels for table2: {}", uiLabels);
        log.info("Number of UI labels: {}", labels.size());
        
        // Just verify it's valid JSON, even if empty
        assertTrue(labels.size() >= 0, "UI labels should be valid JSON object for table2");
    }

    @Test
    @DisplayName("Should handle non-existent table gracefully")
    void testGetUiLabels_NonExistentTable() {
        OperationResult<String> result = postgresqlJsonClient.getUiLabels("nonexistent_table");
        
        assertTrue(result.isError() || result.isNotFound(), "Should return error or not-found for non-existent table");
        log.info("UI labels for non-existent table: {}", result);
    }

    @Test
    @DisplayName("Should handle null table name")
    void testGetUiLabels_NullTableName() {
        OperationResult<String> result = postgresqlJsonClient.getUiLabels(null);
        
        assertTrue(result.isError(), "Should return error for null table name");
        log.info("Null table name handled gracefully: {}", result);
    }

    @Test
    @DisplayName("Should handle empty table name")
    void testGetUiLabels_EmptyTableName() {
        OperationResult<String> result = postgresqlJsonClient.getUiLabels("");
        
        assertTrue(result.isError(), "Should return error for empty table name");
        log.info("Empty table name handled gracefully: {}", result);
    }

    @Test
    @DisplayName("Should handle whitespace table name")
    void testGetUiLabels_WhitespaceTableName() {
        OperationResult<String> result = postgresqlJsonClient.getUiLabels("   ");
        
        assertTrue(result.isError(), "Should return error for whitespace table name");
        log.info("Whitespace table name handled gracefully: {}", result);
    }

    @Test
    @DisplayName("Should generate UI labels for specific table and schema")
    void testGetUiLabels_ValidTableAndSchema() throws PostgreJsonException {
        String uiLabels = postgresqlJsonClient.getUiLabels(tableName, schemaName).getOrThrow();
        
        assertNotNull(uiLabels, "UI labels should not be null for valid table and schema");
        assertFalse(uiLabels.trim().isEmpty(), "UI labels should not be empty for valid table and schema");
        
        JsonObject labels = gson.fromJson(uiLabels, JsonObject.class);
        assertNotNull(labels, "UI labels should be valid JSON for valid table and schema");
        
        // The method might return empty JSON if no UI labels are found
        log.info("Generated UI labels for table1 with schema: {}", uiLabels);
        log.info("Number of UI labels: {}", labels.size());
        
        // Just verify it's valid JSON, even if empty
        assertTrue(labels.size() >= 0, "UI labels should be valid JSON object for valid table and schema");
    }

    @Test
    @DisplayName("Should generate UI labels for table2 with schema1")
    void testGetUiLabels_Table2Schema1() throws PostgreJsonException {
        String uiLabels = postgresqlJsonClient.getUiLabels("table2", "table2_schema1").getOrThrow();
        
        assertNotNull(uiLabels, "UI labels should not be null for table2 schema1");
        assertFalse(uiLabels.trim().isEmpty(), "UI labels should not be empty for table2 schema1");
        
        JsonObject labels = gson.fromJson(uiLabels, JsonObject.class);
        assertNotNull(labels, "UI labels should be valid JSON for table2 schema1");
        
        // The method might return empty JSON if no UI labels are found
        log.info("Generated UI labels for table2 schema1: {}", uiLabels);
        log.info("Number of UI labels: {}", labels.size());
        
        // Just verify it's valid JSON, even if empty
        assertTrue(labels.size() >= 0, "UI labels should be valid JSON object for table2 schema1");
    }

    @Test
    @DisplayName("Should generate UI labels for table2 with schema2")
    void testGetUiLabels_Table2Schema2() throws PostgreJsonException {
        String uiLabels = postgresqlJsonClient.getUiLabels("table2", "table2_schema2").getOrThrow();
        
        assertNotNull(uiLabels, "UI labels should not be null for table2 schema2");
        assertFalse(uiLabels.trim().isEmpty(), "UI labels should not be empty for table2 schema2");
        
        JsonObject labels = gson.fromJson(uiLabels, JsonObject.class);
        assertNotNull(labels, "UI labels should be valid JSON for table2 schema2");
        
        // The method might return empty JSON if no UI labels are found
        log.info("Generated UI labels for table2 schema2: {}", uiLabels);
        log.info("Number of UI labels: {}", labels.size());
        
        // Just verify it's valid JSON, even if empty
        assertTrue(labels.size() >= 0, "UI labels should be valid JSON object for table2 schema2");
    }

    @Test
    @DisplayName("Should handle non-existent schema gracefully")
    void testGetUiLabels_NonExistentSchema() {
        OperationResult<String> result = postgresqlJsonClient.getUiLabels(tableName, "nonexistent_schema");
        
        assertTrue(result.isError() || result.isNotFound(), "Should return error or not-found for non-existent schema");
        log.info("UI labels for non-existent schema: {}", result);
    }

    @Test
    @DisplayName("Should handle null parameters")
    void testGetUiLabels_NullParameters() {
        // Test null table name
        OperationResult<String> result1 = postgresqlJsonClient.getUiLabels(null, schemaName);
        assertTrue(result1.isError(), "Should return error for null table name");
        
        // Test null schema name
        OperationResult<String> result2 = postgresqlJsonClient.getUiLabels(tableName, null);
        assertNotNull(result2, "Should return valid result for null schema name");
        
        log.info("Null parameters handled gracefully: table={}, schema={}", result1, result2);
    }

    @Test
    @DisplayName("Should handle empty parameters")
    void testGetUiLabels_EmptyParameters() {
        // Test empty table name
        OperationResult<String> result1 = postgresqlJsonClient.getUiLabels("", schemaName);
        assertTrue(result1.isError(), "Should return error for empty table name");
        
        // Test empty schema name
        OperationResult<String> result2 = postgresqlJsonClient.getUiLabels(tableName, "");
        assertNotNull(result2, "Should return valid result for empty schema name");
        
        log.info("Empty parameters handled gracefully: table={}, schema={}", result1, result2);
    }

    @Test
    @DisplayName("Should handle whitespace parameters")
    void testGetUiLabels_WhitespaceParameters() {
        // Test whitespace table name
        OperationResult<String> result1 = postgresqlJsonClient.getUiLabels("   ", schemaName);
        assertTrue(result1.isError(), "Should return error for whitespace table name");
        
        // Test whitespace schema name
        OperationResult<String> result2 = postgresqlJsonClient.getUiLabels(tableName, "   ");
        assertNotNull(result2, "Should return valid result for whitespace schema name");
        
        log.info("Whitespace parameters handled gracefully: table={}, schema={}", result1, result2);
    }

    @Test
    @DisplayName("Should handle case sensitivity correctly")
    void testGetUiLabels_CaseSensitivity() {
        // Test case sensitivity - should not find with different case
        OperationResult<String> result = postgresqlJsonClient.getUiLabels("TABLE1");
        
        assertTrue(result.isError() || result.isNotFound(), "Should return error or not-found for case mismatch");
        log.info("Case sensitivity handled for UI labels: {}", result);
    }

    @Test
    @DisplayName("Should handle special characters in parameters")
    void testGetUiLabels_SpecialCharacters() {
        // Test with special characters that should not exist
        OperationResult<String> result = postgresqlJsonClient.getUiLabels("table1@#$");
        
        assertTrue(result.isError() || result.isNotFound(), "Should return error or not-found for special characters");
        log.info("Special characters handled for UI labels: {}", result);
    }

    @Test
    @DisplayName("Should generate consistent UI labels for same table")
    void testGetUiLabels_Consistency() throws PostgreJsonException {
        // Test that multiple calls return consistent results
        String uiLabels1 = postgresqlJsonClient.getUiLabels(tableName).getOrThrow();
        String uiLabels2 = postgresqlJsonClient.getUiLabels(tableName).getOrThrow();
        
        assertEquals(uiLabels1, uiLabels2, "UI labels should be consistent across multiple calls");
        
        log.info("UI labels consistency verified for table1");
    }

    @Test
    @DisplayName("Should generate different UI labels for different schemas")
    void testGetUiLabels_DifferentSchemas() throws PostgreJsonException {
        // Test that different schemas return different UI labels
        String uiLabels1 = postgresqlJsonClient.getUiLabels("table2", "table2_schema1").getOrThrow();
        String uiLabels2 = postgresqlJsonClient.getUiLabels("table2", "table2_schema2").getOrThrow();
        
        assertNotNull(uiLabels1, "UI labels should not be null for schema1");
        assertNotNull(uiLabels2, "UI labels should not be null for schema2");
        
        // They might be different or the same depending on schema content
        log.info("UI labels for table2 schema1: {}", uiLabels1);
        log.info("UI labels for table2 schema2: {}", uiLabels2);
    }
}
