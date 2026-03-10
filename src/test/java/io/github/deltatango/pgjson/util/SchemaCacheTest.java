package io.github.deltatango.pgjson.util;

import com.networknt.schema.Schema;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@DisplayName("SchemaCache Unit Tests")
public class SchemaCacheTest {

    private SchemaCache schemaCache;
    private FileUtil fileUtil;

    @BeforeEach
    void setUp() {
        schemaCache = new SchemaCache();
        fileUtil = new FileUtil();
    }

    @Test
    @DisplayName("Should cache compiled schema on first access")
    void testCacheOnFirstAccess() throws Exception {
        String schemaData = fileUtil.readStringFromFile("json/test-schema.json");
        Integer tabledefId = 1;
        String schemaHash = "hash123";

        assertEquals(0, schemaCache.size());

        Schema schema = schemaCache.getCompiledSchema(tabledefId, schemaHash, schemaData);

        assertNotNull(schema);
        assertEquals(1, schemaCache.size());
    }

    @Test
    @DisplayName("Should return cached schema on subsequent access")
    void testCacheHit() throws Exception {
        String schemaData = fileUtil.readStringFromFile("json/test-schema.json");
        Integer tabledefId = 1;
        String schemaHash = "hash123";

        Schema schema1 = schemaCache.getCompiledSchema(tabledefId, schemaHash, schemaData);
        Schema schema2 = schemaCache.getCompiledSchema(tabledefId, schemaHash, schemaData);

        assertSame(schema1, schema2);
        assertEquals(1, schemaCache.size());
    }

    @Test
    @DisplayName("Should invalidate cache on hash mismatch")
    void testHashMismatch() throws Exception {
        String schemaData = fileUtil.readStringFromFile("json/test-schema.json");
        Integer tabledefId = 1;
        String schemaHash1 = "hash123";
        String schemaHash2 = "hash456";

        Schema schema1 = schemaCache.getCompiledSchema(tabledefId, schemaHash1, schemaData);
        Schema schema2 = schemaCache.getCompiledSchema(tabledefId, schemaHash2, schemaData);

        assertNotSame(schema1, schema2);
        assertEquals(1, schemaCache.size());
    }

    @Test
    @DisplayName("Should handle multiple schemas")
    void testMultipleSchemas() throws Exception {
        String schemaData1 = fileUtil.readStringFromFile("json/test-schema.json");
        String schemaData2 = fileUtil.readStringFromFile("json/test_schema2.json");

        Schema schema1 = schemaCache.getCompiledSchema(1, "hash1", schemaData1);
        Schema schema2 = schemaCache.getCompiledSchema(2, "hash2", schemaData2);

        assertNotNull(schema1);
        assertNotNull(schema2);
        assertNotSame(schema1, schema2);
        assertEquals(2, schemaCache.size());
    }

    @Test
    @DisplayName("Should invalidate specific schema")
    void testInvalidate() throws Exception {
        String schemaData = fileUtil.readStringFromFile("json/test-schema.json");
        Integer tabledefId = 1;

        schemaCache.getCompiledSchema(tabledefId, "hash123", schemaData);
        assertEquals(1, schemaCache.size());

        schemaCache.invalidate(tabledefId);
        assertEquals(0, schemaCache.size());
    }

    @Test
    @DisplayName("Should clear all cached schemas")
    void testClear() throws Exception {
        String schemaData1 = fileUtil.readStringFromFile("json/test-schema.json");
        String schemaData2 = fileUtil.readStringFromFile("json/test_schema2.json");

        schemaCache.getCompiledSchema(1, "hash1", schemaData1);
        schemaCache.getCompiledSchema(2, "hash2", schemaData2);
        assertEquals(2, schemaCache.size());

        schemaCache.clear();
        assertEquals(0, schemaCache.size());
    }

    @Test
    @DisplayName("Should be thread-safe")
    void testConcurrentAccess() throws Exception {
        String schemaData = fileUtil.readStringFromFile("json/test-schema.json");
        Integer tabledefId = 1;
        String schemaHash = "hash123";

        // Run 10 threads accessing cache concurrently
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                assertDoesNotThrow(() -> {
                    Schema schema = schemaCache.getCompiledSchema(tabledefId, schemaHash, schemaData);
                    assertNotNull(schema);
                });
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(1, schemaCache.size());
    }
}
