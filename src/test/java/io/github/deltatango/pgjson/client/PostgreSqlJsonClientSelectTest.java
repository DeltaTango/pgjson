package io.github.deltatango.pgjson.client;

import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import io.github.deltatango.pgjson.exceptions.RequestException;
import io.github.deltatango.pgjson.model.DatabaseEntry;
import io.github.deltatango.pgjson.util.DatabaseConfigurationUtil;
import io.github.deltatango.pgjson.util.FileUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class PostgreSqlJsonClientSelectTest extends DatabaseConfigurationUtil {

    Gson gson = new GsonBuilder().create();
    FileUtil fileUtil = new FileUtil();

    String tableName = "table1";

    @Test
    public void selectDataByIdUuid() throws PostgreJsonException {
        String idUuid = "f54be158-0ff9-49d1-a070-954e6b64caac";
        DatabaseEntry data = postgresqlJsonClient.selectDataByIdUuid(idUuid, tableName).getOrThrow();
        log.debug("data: {}", data);
        assertNotNull(data);
    }

    @Test
    public void selectDataByIdUuid2() throws PostgreJsonException {
        String idUuid = "f54be158-0ff9-49d1-a070-954e6b64caac";
        DatabaseEntry data = postgresqlJsonClient.selectDataByIdUuid(idUuid, tableName).getOrThrow();
        log.debug("data: {}", data);
        assertNotNull(data);
    }

    @Test
    public void searchDataTest() throws Exception {
        String searchJson = fileUtil.readStringFromFile("json/searchData/searchData1.json");
        log.debug("searchJson: {}", searchJson);
        List<DatabaseEntry> result = postgresqlJsonClient.selectData(tableName, searchJson).getOrThrow();
        // limit is 10 in query
        assertEquals(10, result.size());
        log.info("Element: {}", result.get(0));
        assertEquals(11, result.get(0).getEntryDbId().intValue());
        // 10th element id is 2
        assertEquals(2, result.get(9).getEntryDbId().intValue());
    }

    @Test
    public void searchData2Test() throws Exception {
        String searchJson = fileUtil.readStringFromFile("json/searchData/searchData2.json");
        log.debug("searchJson: {}", searchJson);

        RequestException thrown = assertThrows(
                RequestException.class,
                () -> postgresqlJsonClient.selectData(tableName, searchJson),
                "Expected postgresqlJsonClient.selectData() to throw, but it didn't"
        );

        assertTrue(thrown.getMessage().equals("Query limit value invalid, should be at least 1"));
    }

    @Test
    public void searchData3Test() throws Exception {
        String searchJson = fileUtil.readStringFromFile("json/searchData/searchData3.json");
        log.debug("searchJson: {}", searchJson);
        List<DatabaseEntry> result = postgresqlJsonClient.selectData(tableName, searchJson).getOrThrow();
        assertEquals(1, result.size());
        log.debug("Result: {}", result);
        assertEquals(Integer.valueOf(1), result.get(0).getEntryDbId());
    }

    @Test
    public void searchData4Test() throws Exception {
        String searchJson = fileUtil.readStringFromFile("json/searchData/searchData4.json");
        log.debug("searchJson: {}", searchJson);
        List<DatabaseEntry> result = postgresqlJsonClient.selectData(tableName, searchJson).getOrThrow();
        assertEquals(1, result.size());
        log.debug("Result: {}", result);
        assertEquals(Integer.valueOf(1), result.get(0).getEntryDbId());
    }

    @Test
    public void searchData5Test() throws Exception {
        String searchJson = fileUtil.readStringFromFile("json/searchData/searchData5.json");
        log.debug("searchJson: {}", searchJson);
        List<DatabaseEntry> result = postgresqlJsonClient.selectData(tableName, searchJson).getOrThrow();
        assertEquals(1, result.size());
        log.debug("Result: {}", result);
        assertEquals(Integer.valueOf(7), result.get(0).getEntryDbId());
    }

    @Test
    public void searchData6Test() throws Exception {
        String searchJson = fileUtil.readStringFromFile("json/searchData/searchData6.json");
        log.debug("searchJson: {}", searchJson);
        List<DatabaseEntry> result = postgresqlJsonClient.selectData(tableName, searchJson).getOrThrow();
        assertEquals(10, result.size());
        log.debug("Result: {}", result);
        assertEquals(Integer.valueOf(1), result.get(0).getEntryDbId());
        assertEquals(Integer.valueOf(5), result.get(4).getEntryDbId());
    }

    @Test
    public void searchData7Test() throws Exception {
        String searchJson = fileUtil.readStringFromFile("json/searchData/searchData7.json");
        log.debug("searchJson: {}", searchJson);

        // This test should now pass without throwing an exception since searchTerm is optional
        List<DatabaseEntry> result = postgresqlJsonClient.selectData(tableName, searchJson).getOrThrow();
        assertNotNull(result);
        log.debug("Result: {}", result);
    }

    @Test
    public void searchData8Test() throws Exception {
        String searchJson = fileUtil.readStringFromFile("json/searchData/searchData8.json");
        log.debug("searchJson: {}", searchJson);
        List<DatabaseEntry> result = postgresqlJsonClient.selectData(tableName, searchJson).getOrThrow();
        assertEquals(1, result.size());
    }

    @Test
    public void searchData9Test() throws Exception {
        String searchJson = fileUtil.readStringFromFile("json/searchData/searchData9.json");
        log.debug("searchJson: {}", searchJson);
        List<DatabaseEntry> result = postgresqlJsonClient.selectData(tableName, searchJson).getOrThrow();
        assertEquals(1, result.size());
    }

    @Test
    public void searchData10Test() throws Exception {
        String searchJson = fileUtil.readStringFromFile("json/searchData/searchData10.json");
        log.debug("searchJson: {}", searchJson);
        List<DatabaseEntry> result = postgresqlJsonClient.selectData(tableName, searchJson).getOrThrow();
        assertEquals(1, result.size());
        assertEquals("0d08578e-477e-40b2-b7db-3a61bb0acc4f", result.get(0).getEntryIdUuid());
    }

    @Test
    public void searchData11Test() throws Exception {
        String searchJson = fileUtil.readStringFromFile("json/searchData/searchData11.json");
        log.debug("searchJson: {}", searchJson);
        List<DatabaseEntry> result = postgresqlJsonClient.selectData(tableName, searchJson).getOrThrow();
        assertEquals(1, result.size());
        log.debug("Result: {}", result);
        assertEquals(Integer.valueOf(1), result.get(0).getEntryDbId());
    }

    @Test
    public void getUiLabelsTest() throws IOException, PostgreJsonException {
        String uiLabels =
                postgresqlJsonClient.getUiLabelsFromSchemaFile(
                        fileUtil.readStringFromFile("json/test-schema.json")).getOrThrow();
        log.debug("UI LABELS: {} ", uiLabels);

        JsonObject jsonObject = gson.fromJson(uiLabels, JsonObject.class);
        assertEquals(12, jsonObject.size());
    }

    @Test
    public void searchData12Test() throws Exception {
        String searchJson = fileUtil.readStringFromFile("json/searchData/searchData12.json");
        log.debug("searchJson: {}", searchJson);
        List<DatabaseEntry> result = postgresqlJsonClient.selectData(tableName, searchJson).getOrThrow();
        assertEquals(1, result.size());
        log.debug("Result: {}", result);
        assertEquals(Integer.valueOf(11), result.get(0).getEntryDbId());
    }

    @Test
    public void searchDataFromTable2Test() throws Exception {
        String searchJson = fileUtil.readStringFromFile("json/searchData/searchData_table2_schema1.json");
        log.debug("searchJson: {}", searchJson);
        List<DatabaseEntry> result = postgresqlJsonClient.selectData("table2", "table2_schema1", searchJson).getOrThrow();
        assertEquals(1, result.size());
        log.debug("Result: {}", result);
        assertEquals(Integer.valueOf(2), result.get(0).getEntryDbId());
    }

    @Test
    public void searchDataFromTable2_attribute3_Test() throws Exception {
        String searchJson = fileUtil.readStringFromFile("json/searchData/searchData_table2_schema2_v1.json");
        log.debug("searchJson: {}", searchJson);
        List<DatabaseEntry> result = postgresqlJsonClient.selectData("table2", "table2_schema2", searchJson).getOrThrow();
        assertEquals(1, result.size());
        log.debug("Result: {}", result);
        assertEquals(Integer.valueOf(4), result.get(0).getEntryDbId());
    }

    @Test
    public void searchData13Test() throws Exception {
        String searchJson = fileUtil.readStringFromFile("json/searchData/searchData13.json");
        log.debug("searchJson: {}", searchJson);
        List<DatabaseEntry> result = postgresqlJsonClient.selectData(tableName, searchJson).getOrThrow();
        log.info("Results: {}", result);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).getEntryDbId());
        assertEquals(Integer.valueOf(11), result.get(1).getEntryDbId());
    }

    @Test
    public void searchData14Test() throws Exception {
        String searchJson = fileUtil.readStringFromFile("json/searchData/searchData14.json");
        log.debug("searchJson: {}", searchJson);
        List<DatabaseEntry> result = postgresqlJsonClient.selectData("table3", searchJson).getOrThrow();
        log.info("Results: {}", result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).getEntryDbId());
    }
}
