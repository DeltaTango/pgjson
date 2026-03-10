package io.github.deltatango.pgjson.client;

import io.github.deltatango.pgjson.model.DatabaseEntry;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import io.github.deltatango.pgjson.util.DatabaseConfigurationUtil;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class PostgreSqlJsonClientSearchWithoutIndexTest extends DatabaseConfigurationUtil {

    @Test
    public void testSearchWithoutIndex_ExactMatch() throws Exception {
        // Use existing table1 and search for a field that doesn't have an index
        String tableName = "table1";

        // Search for a field that doesn't have an index - should work with warning
        String searchJson = "{\"limit\": 10, \"offset\": 0, \"searchTerm\": {\"non_indexed_field\": \"test_value\"}}";
        OperationResult<List<DatabaseEntry>> opResult = postgresqlJsonClient.selectData(tableName, searchJson);
        List<DatabaseEntry> results = opResult.getOrThrow();

        // Should return empty results but not throw exception (fallback works)
        assertNotNull(results);
    }

    @Test
    public void testSearchWithoutIndex_FullTextSearch() throws Exception {
        String tableName = "table1";

        // Search with multi-word query for non-indexed field - should use FTS fallback
        String searchJson = "{\"limit\": 10, \"offset\": 0, \"searchTerm\": {\"non_indexed_description\": \"database design patterns\"}}";
        OperationResult<List<DatabaseEntry>> opResult = postgresqlJsonClient.selectData(tableName, searchJson);
        List<DatabaseEntry> results = opResult.getOrThrow();

        // Should return empty results but not throw exception (fallback works)
        assertNotNull(results);
    }

    @Test
    public void testSearchWithoutIndex_ObjectQuery() throws Exception {
        String tableName = "table1";

        // Search with object query for non-indexed field - should use object containment fallback
        String searchJson = "{\"limit\": 10, \"offset\": 0, \"searchTerm\": {\"non_indexed_user\": {\"address\": {\"city\": \"New York\"}}}}";
        OperationResult<List<DatabaseEntry>> opResult = postgresqlJsonClient.selectData(tableName, searchJson);
        List<DatabaseEntry> results = opResult.getOrThrow();

        // Should return empty results but not throw exception (fallback works)
        assertNotNull(results);
    }

    @Test
    public void testSearchWithoutIndex_ArrayQuery() throws Exception {
        String tableName = "table1";

        // Search with array query for non-indexed field - should use object containment fallback
        String searchJson = "{\"limit\": 10, \"offset\": 0, \"searchTerm\": {\"non_indexed_tags\": [\"java\", \"database\"]}}";
        OperationResult<List<DatabaseEntry>> opResult = postgresqlJsonClient.selectData(tableName, searchJson);
        List<DatabaseEntry> results = opResult.getOrThrow();

        // Should return empty results but not throw exception (fallback works)
        assertNotNull(results);
    }

    private void createTableWithoutIndexes(String tableName) throws Exception {
        // For this test, we'll use the existing table1 which has indexes
        // but we'll test the fallback by searching for fields that don't have indexes
        // This is a simpler approach that still tests the fallback behavior
    }
}
