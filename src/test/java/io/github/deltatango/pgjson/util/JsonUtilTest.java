package io.github.deltatango.pgjson.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonUtil Array Merge Tests")
public class JsonUtilTest {

    private JsonUtil jsonUtil;

    @BeforeEach
    void setUp() {
        jsonUtil = new JsonUtil();
    }

    @Test
    @DisplayName("Should replace array when no identity keys provided")
    void testMergeArrays_ReplaceStrategy_NoIdentityKeys() {
        String array1 = "[{\"id\":1,\"name\":\"A\"},{\"id\":2,\"name\":\"B\"}]";
        String array2 = "[{\"id\":3,\"name\":\"C\"}]";

        String result = jsonUtil.mergeArrays(array1, array2);
        JsonArray resultArray = jsonUtil.getJsonArray(result);

        assertEquals(1, resultArray.size());
        assertEquals("C", resultArray.get(0).getAsJsonObject().get("name").getAsString());
    }

    @Test
    @DisplayName("Should replace array with multiple arrays when no identity keys")
    void testMergeArrays_ReplaceStrategy_MultipleArrays() {
        String array1 = "[{\"id\":1,\"name\":\"A\"}]";
        String array2 = "[{\"id\":2,\"name\":\"B\"}]";
        String array3 = "[{\"id\":3,\"name\":\"C\"}]";

        String result = jsonUtil.mergeArrays(array1, array2, array3);
        JsonArray resultArray = jsonUtil.getJsonArray(result);

        assertEquals(1, resultArray.size());
        assertEquals("C", resultArray.get(0).getAsJsonObject().get("name").getAsString());
    }

    @Test
    @DisplayName("Should ignore null elements in replace strategy")
    void testMergeArrays_ReplaceStrategy_IgnoreNulls() {
        String array1 = "[{\"id\":1,\"name\":\"A\"}]";
        String array2 = "[null,{\"id\":2,\"name\":\"B\"},null]";

        String result = jsonUtil.mergeArrays(array1, array2);
        JsonArray resultArray = jsonUtil.getJsonArray(result);

        assertEquals(1, resultArray.size());
        assertEquals("B", resultArray.get(0).getAsJsonObject().get("name").getAsString());
    }

    @Test
    @DisplayName("Should merge array by identity keys when provided")
    void testMergeArrays_MergeStrategy_WithIdentityKeys() {
        Map<String, String[]> keyCombinations = new HashMap<>();
        keyCombinations.put("items", new String[]{"id"});

        String array1 = "[{\"id\":1,\"name\":\"A\"}]";
        String array2 = "[{\"id\":1,\"name\":\"Updated\"},{\"id\":2,\"name\":\"B\"}]";

        String result = jsonUtil.mergeArrays("items", keyCombinations, array1, array2);
        JsonArray resultArray = jsonUtil.getJsonArray(result);

        assertEquals(2, resultArray.size());
        assertEquals("Updated", resultArray.get(0).getAsJsonObject().get("name").getAsString());
        assertEquals("B", resultArray.get(1).getAsJsonObject().get("name").getAsString());
    }

    @Test
    @DisplayName("Should handle empty arrays in replace strategy")
    void testMergeArrays_ReplaceStrategy_EmptyArrays() {
        String array1 = "[]";
        String array2 = "[{\"id\":1,\"name\":\"A\"}]";
        String array3 = "[]";

        String result = jsonUtil.mergeArrays(array1, array2, array3);
        JsonArray resultArray = jsonUtil.getJsonArray(result);

        assertEquals(0, resultArray.size());
    }

    @Test
    @DisplayName("Should handle primitive arrays in replace strategy")
    void testMergeArrays_ReplaceStrategy_PrimitiveArrays() {
        String array1 = "[\"A\",\"B\"]";
        String array2 = "[\"C\"]";

        String result = jsonUtil.mergeArrays(array1, array2);
        JsonArray resultArray = jsonUtil.getJsonArray(result);

        assertEquals(1, resultArray.size());
        assertEquals("C", resultArray.get(0).getAsString());
    }

    @Test
    @DisplayName("Should handle mixed content arrays in replace strategy")
    void testMergeArrays_ReplaceStrategy_MixedContent() {
        String array1 = "[{\"id\":1}, \"string\", 42]";
        String array2 = "[{\"id\":2}, \"updated\"]";

        String result = jsonUtil.mergeArrays(array1, array2);
        JsonArray resultArray = jsonUtil.getJsonArray(result);

        assertEquals(2, resultArray.size());
        assertEquals(2, resultArray.get(0).getAsJsonObject().get("id").getAsInt());
        assertEquals("updated", resultArray.get(1).getAsString());
    }

    @Test
    @DisplayName("Should merge complex nested objects with identity keys")
    void testMergeArrays_MergeStrategy_ComplexNestedObjects() {
        Map<String, String[]> keyCombinations = new HashMap<>();
        keyCombinations.put("items", new String[]{"id"});

        String array1 = "[{\"id\":1,\"name\":\"A\",\"details\":{\"active\":true}}]";
        String array2 = "[{\"id\":1,\"name\":\"Updated\",\"details\":{\"active\":false,\"new\":\"field\"}}]";

        String result = jsonUtil.mergeArrays("items", keyCombinations, array1, array2);
        JsonArray resultArray = jsonUtil.getJsonArray(result);

        assertEquals(1, resultArray.size());
        JsonObject mergedObject = resultArray.get(0).getAsJsonObject();
        assertEquals("Updated", mergedObject.get("name").getAsString());
        assertEquals(false, mergedObject.get("details").getAsJsonObject().get("active").getAsBoolean());
        assertEquals("field", mergedObject.get("details").getAsJsonObject().get("new").getAsString());
    }

    @Test
    @DisplayName("Should handle multiple identity keys")
    void testMergeArrays_MergeStrategy_MultipleIdentityKeys() {
        Map<String, String[]> keyCombinations = new HashMap<>();
        keyCombinations.put("items", new String[]{"id", "type"});

        String array1 = "[{\"id\":1,\"type\":\"A\",\"name\":\"Item1\"}]";
        String array2 = "[{\"id\":1,\"type\":\"A\",\"name\":\"Updated\"},{\"id\":1,\"type\":\"B\",\"name\":\"Different\"}]";

        String result = jsonUtil.mergeArrays("items", keyCombinations, array1, array2);
        JsonArray resultArray = jsonUtil.getJsonArray(result);

        assertEquals(2, resultArray.size());
        assertEquals("Updated", resultArray.get(0).getAsJsonObject().get("name").getAsString());
        assertEquals("Different", resultArray.get(1).getAsJsonObject().get("name").getAsString());
    }
}
