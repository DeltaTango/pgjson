package io.github.deltatango.pgjson.client;

import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import io.github.deltatango.pgjson.model.operations.Result;
import io.github.deltatango.pgjson.util.DatabaseConfigurationUtil;
import io.github.deltatango.pgjson.util.FileUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class PostgreSqlJsonClientUpdateTest extends DatabaseConfigurationUtil {

    FileUtil fileUtil = new FileUtil();
    Gson gson = new Gson();

    @Test
    public void updateDataWithMerge() throws java.io.IOException, PostgreJsonException {
        String tableName = "table1";
        String fileData = fileUtil.readStringFromFile("json/dataUpdate/data_update.json");
        String idUuid = "f54be158-0ff9-49d1-a070-954e6b64caac";
        Result result = postgresqlJsonClient.updateDataWithMerge(tableName, fileData, idUuid).getOrThrow();
        log.info("Result: {}", result);
        assertNotNull(result);
        assertTrue(result.getValidationResult().getValidationStatus());
        assertTrue(result.getResultStatus());
    }

    @Test
    public void updateDataWithMerge2() throws java.io.IOException, PostgreJsonException {
        String tableName = "table1";
        String fileData = fileUtil.readStringFromFile("json/dataUpdate/data_update2.json");
        String idUuid = "5a458cfb-cd4d-4f15-91f1-c1f0e7cd32cb";
        Result result = postgresqlJsonClient.updateDataWithMerge(tableName, fileData, idUuid).getOrThrow();
        log.info("Result: {}", result);
        assertNotNull(result);
        assertTrue(result.getResultStatus());
    }

    @Test
    public void updateDataWithMerge3() throws java.io.IOException, PostgreJsonException {
        String tableName = "table1";
        String fileData = fileUtil.readStringFromFile("json/dataUpdate/data_update3.json");
        String idUuid = "5a458cfb-cd4d-4f15-91f1-c1f0e7cd32cb";
        Result result = postgresqlJsonClient.updateDataWithMerge(tableName, fileData, idUuid).getOrThrow();
        log.info("Result: {}", result);
        assertNotNull(result);
        assertEquals(true, result.getResultStatus());
        JsonObject restulObject = gson.fromJson(result.getFinalJsonData(), JsonObject.class);
        JsonArray attribute4Array = restulObject.getAsJsonArray("attribute4");
        assertEquals(1, attribute4Array.size()); // REPLACE strategy: array is replaced with new data
        JsonObject arrayItem = attribute4Array.get(0).getAsJsonObject();
        String attribute10 = arrayItem.get("attribute10").getAsString();
        assertEquals("XXXXXXXXXXXXXXXXX", attribute10);
    }

    @Test
    public void updateData() throws java.io.IOException, PostgreJsonException {
        String tableName = "table1";
        String fileData = fileUtil.readStringFromFile("json/dataUpdate/data_update4.json");
        String idUuid = "0d08578e-477e-40b2-b7db-3a61bb0acc4f";
        Result result = postgresqlJsonClient.updateData(tableName, fileData, idUuid).getOrThrow();
        assertNotNull(result);
        log.info("Result: {}", result);
        assertEquals(true, result.getResultStatus());
        JsonObject restulObject = gson.fromJson(result.getFinalJsonData(), JsonObject.class);
        JsonArray attribute4Array = restulObject.getAsJsonArray("attribute4");
        assertEquals(1, attribute4Array.size());
        JsonObject arrayItem = attribute4Array.get(0).getAsJsonObject();
        String attribute10 = arrayItem.get("attribute10").getAsString();
        assertEquals("tehk", attribute10);
    }

    @Test
    public void deleteArrayElement() throws java.io.IOException, PostgreJsonException {
        String tableName = "table1";
        String fileData = fileUtil.readStringFromFile("json/dataUpdate/identificator_delete.json");
        String idUuid = "c11c49b7-6687-47f3-ac4f-b0b6b17626e4";
        Result result = postgresqlJsonClient.deleteArrayElement(tableName, fileData, idUuid).getOrThrow();
        log.info("Result: {}", result);
        assertNotNull(result);
        assertEquals(true, result.getResultStatus());
        JsonObject restulObject = gson.fromJson(result.getFinalJsonData(), JsonObject.class);
        JsonArray attribute4Array = restulObject.getAsJsonArray("attribute4");
        assertEquals(4, attribute4Array.size());
    }

    @Test
    public void deleteData() throws PostgreJsonException {
        String tableName = "table1";
        String idUuid = "caffe464-b867-456c-b40a-e7803e3ac113";
        Boolean result = postgresqlJsonClient.deleteData(tableName, idUuid).getOrThrow();
        log.info("Result: {}", result);
        assertNotNull(result);
        assertEquals(true, result);
    }
}
