package io.github.deltatango.pgjson.client;

import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import io.github.deltatango.pgjson.model.TableDef;
import io.github.deltatango.pgjson.util.DatabaseConfigurationUtil;
import io.github.deltatango.pgjson.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class PostgreSqlJsonClientTabledefTest extends DatabaseConfigurationUtil {

    FileUtil fileUtil = new FileUtil();

    @Test
    public void getTableDefById() throws PostgreJsonException {
        Integer tableDefId = 1;
        TableDef tableDef = postgresqlJsonClient.getTableDefByIdFromMemory(tableDefId).getOrThrow();
        log.debug("Tabledef: {}", tableDef);
        assertNotNull(tableDef);
        assertEquals(tableDefId, tableDef.getTableDefId());
    }

    @Test
    public void getTableDefByIdUuid() throws PostgreJsonException {
        String idUuid = "6dc4bf18-49e7-4c10-8a23-a4752e48f6d0";
        TableDef tableDef = postgresqlJsonClient.getTableDefByIdUuid(idUuid).getOrThrow();
        assertNotNull(tableDef);
        assertEquals(idUuid, tableDef.getIdUuid());
    }

    @Test
    public void getTableDefByIdUuidFromMemory() throws PostgreJsonException {
        String idUuid = "6dc4bf18-49e7-4c10-8a23-a4752e48f6d0";
        String tableName = "table1";
        TableDef tableDef = postgresqlJsonClient.getTableDefByIdUuidFromMemory(idUuid).getOrThrow();
        assertNotNull(tableDef);
        assertEquals(idUuid, tableDef.getIdUuid());
        assertEquals(tableName, tableDef.getTableName());
    }

    @Test
    public void insertSchema() throws java.io.IOException, PostgreJsonException {

        String tableName = "table1";
        String schemaName = "table1_schema-insert";
        String schemaData = fileUtil.readStringFromFile("json/test-schema-insert.json");

        String result = postgresqlJsonClient.insertSchema(tableName, schemaName, schemaData).getOrThrow();
        log.info("Result: {}", result);
        assertNotNull(result);
    }

    @Test
    public void isDatabaseRunning() {
        boolean result = postgresqlJsonClient.isDatabaseRunning();
        assertTrue(result);
    }
}
