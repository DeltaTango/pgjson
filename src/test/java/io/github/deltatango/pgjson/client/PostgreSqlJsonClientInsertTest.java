package io.github.deltatango.pgjson.client;

import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import io.github.deltatango.pgjson.model.operations.Result;
import io.github.deltatango.pgjson.util.DatabaseConfigurationUtil;
import io.github.deltatango.pgjson.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class PostgreSqlJsonClientInsertTest extends DatabaseConfigurationUtil {

    FileUtil fileUtil = new FileUtil();

    String tableName = "table1";

    @Test
    public void insertData() throws java.io.IOException, PostgreJsonException {
        String fileData = fileUtil.readStringFromFile("json/sample_data.json");
        OperationResult<Result> opResult = postgresqlJsonClient.insertData(tableName, fileData);
        assertTrue(opResult.isSuccess());
        Result result = opResult.getOrThrow();
        log.info("Result: {}", result);
        assertNotNull(result);
        assertEquals(true, result.getResultStatus());
    }

    @Test
    public void insertDataFailsValidation() throws java.io.IOException, PostgreJsonException {
        String fileData = fileUtil.readStringFromFile("json/sample_data_not_valid.json");
        OperationResult<Result> opResult = postgresqlJsonClient.insertData(tableName, fileData);
        assertTrue(opResult.isSuccess() || opResult.isError());
        if (opResult.isSuccess()) {
            Result result = opResult.getOrThrow();
            log.info("Result: {}", result);
            assertNotNull(result);
            assertEquals(false, result.getResultStatus());
            assertNull(result.getIduuid());
        } else {
            assertTrue(opResult.isError());
        }
    }

    // table2_schema1
    @Test
    public void insertDataToTableWithMultipleSchema() throws java.io.IOException, PostgreJsonException {
        String fileData = fileUtil.readStringFromFile("json/insert_table2_schema1.json");
        OperationResult<Result> opResult = postgresqlJsonClient.insertData("table2", "table2_schema1", fileData);
        assertTrue(opResult.isSuccess());
        Result result = opResult.getOrThrow();
        log.info("Result: {}", result);
        assertNotNull(result);
        assertEquals(true, result.getResultStatus());

        String fileData2 = fileUtil.readStringFromFile("json/insert_table2_schema2_v2.json");
        OperationResult<Result> opResult2 = postgresqlJsonClient.insertData("table2", "table2_schema2", fileData2);
        assertTrue(opResult2.isSuccess());
        Result result2 = opResult2.getOrThrow();
        log.info("Result: {}", result2);
        assertNotNull(result2);
        assertEquals(true, result2.getResultStatus());

        String fileData3 = fileUtil.readStringFromFile("json/insert_table2_schema2_v1.json");
        OperationResult<Result> opResult3 = postgresqlJsonClient.insertData("table2", "table2_schema2", fileData3);
        assertTrue(opResult3.isSuccess() || opResult3.isError());
        if (opResult3.isSuccess()) {
            Result result3 = opResult3.getOrThrow();
            log.info("Result: {}", result3);
            assertNotNull(result3);
            assertEquals(false, result3.getResultStatus());
        } else {
            assertTrue(opResult3.isError());
        }
    }
}
