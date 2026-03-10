package io.github.deltatango.pgjson.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for merging and parsing JSON objects and arrays using Gson.
 *
 * <p>Provides deep-merge capabilities for JSON objects and arrays with two strategies:</p>
 * <ul>
 *   <li><strong>REPLACE</strong> (no identity keys): arrays are replaced wholesale</li>
 *   <li><strong>MERGE</strong> (with identity keys): array elements are matched by identity
 *       keys and merged recursively</li>
 * </ul>
 *
 * <p>Object merging is recursive: nested objects are merged, arrays follow the chosen strategy,
 * and primitive values are overwritten by the last object. {@code null} values remove the key.</p>
 */
@Slf4j
public class JsonUtil {

    /**
     * https://stackoverflow.com/questions/34092373/merge-extend-json-objects-using-gson-in-java Merge
     * given JSON-objects. Same keys are merged for objects and overwritten by last object for
     * primitive types.
     *
     * @param keyCombinations Key names for unique object identification. Or empty collection.
     * @param objects         Any amount of JSON-objects to merge.
     * @return Merged JSON-object.
     */
    public JsonObject mergeObjects(
            @NonNull Map<String, String[]> keyCombinations, Object... objects) {

        JsonObject mergedObject = new JsonObject();
        for (Object object : objects) {
            JsonObject jsonObject = (JsonObject) object;
            for (String key : jsonObject.keySet()) {
                JsonElement parameter = jsonObject.get(key);
                if (mergedObject.has(key)) {
                    // Key name matches:
                    if (jsonObject.get(key).isJsonObject()) {
                        // This is object - merge:
                        parameter =
                                mergeObjects(
                                        keyCombinations,
                                        mergedObject.get(key).getAsJsonObject(),
                                        jsonObject.get(key).getAsJsonObject());

                    } else if (jsonObject.get(key).isJsonArray()) {
                        // This is array - merge:
                        parameter =
                                mergeArrays(
                                        key,
                                        keyCombinations,
                                        mergedObject.get(key).getAsJsonArray(),
                                        jsonObject.get(key).getAsJsonArray());
                    } else {
                        // This is neither object nor array - replace value:
                        if (parameter.isJsonNull()) {
                            mergedObject.remove(key);
                        } else {
                            mergedObject.add(key, parameter);
                        }
                    }
                }
                // If the element's value is not null and this element does not yet exist, add the element.
                if (!parameter.isJsonNull()) {
                    mergedObject.add(key, parameter);
                }
            }
        }
        return mergedObject;
    }

    /**
     * Merge JSON objects with field-level restrictions.
     *
     * <p>Behaves like {@link #mergeObjects(Map, Object...)} but skips overwriting any key
     * listed in {@code restrictions}. Restricted keys retain the value from the first object
     * that defined them.</p>
     *
     * @param keyCombinations key names for unique object identification in array merges
     * @param restrictions    list of key names that should not be overwritten
     * @param objects         any number of JSON objects to merge
     * @return merged JSON object
     */
    public JsonObject mergeObjectsWithRestrictions(
            @NonNull Map<String, String[]> keyCombinations,
            List<String> restrictions,
            Object... objects) {

        JsonObject mergedObject = new JsonObject();
        for (Object object : objects) {
            JsonObject jsonObject = (JsonObject) object;
            for (String key : jsonObject.keySet()) {
                JsonElement parameter = jsonObject.get(key);
                if (mergedObject.has(key)) {
                    if (restrictions.contains(key)) {
                        continue;
                    }
                    // Key name matches:
                    if (jsonObject.get(key).isJsonObject()) {
                        // This is object - merge:
                        parameter =
                                mergeObjects(
                                        keyCombinations,
                                        mergedObject.get(key).getAsJsonObject(),
                                        jsonObject.get(key).getAsJsonObject());

                    } else if (jsonObject.get(key).isJsonArray()) {
                        // This is array - merge:
                        parameter =
                                mergeArrays(
                                        key,
                                        keyCombinations,
                                        mergedObject.get(key).getAsJsonArray(),
                                        jsonObject.get(key).getAsJsonArray());
                    } else {
                        // This is neither object nor array - replace value:
                        if (parameter.isJsonNull()) {
                            mergedObject.remove(key);
                        } else {
                            mergedObject.add(key, parameter);
                        }
                    }
                }
                // If the element's value is not null and this element does not yet exist, add the element.
                if (!parameter.isJsonNull()) {
                    mergedObject.add(key, parameter);
                }
            }
        }
        return mergedObject;
    }

    /**
     * Get GSON-object from string.
     *
     * @param jsonString JSON-object as string.
     * @return JsonObject (GSON).
     */
    public JsonObject getJsonObject(String jsonString) {
        JsonObject jsonObject = new JsonObject();
        if (jsonString != null) {
            jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        }
        return jsonObject;
    }

    /**
     * Alternative - no object identity keys are set. See {@link JsonUtil#mergeObjects(Map,
     * Object...)}
     */
    public String mergeObjects(String... jsonObjects) {
        ArrayList<JsonObject> objects = new ArrayList<>();
        for (String jsonObject : jsonObjects) {
            objects.add(getJsonObject(jsonObject));
        }
        return (mergeObjects(new HashMap<>(), objects.toArray()).toString());
    }

    /**
     * Alternative - no object identity keys are set and restrictions which field not to overwrite are
     * added. See {@link JsonUtil#mergeObjects(Map, Object...)}
     */
    public String mergeObjects(List<String> restrictions, String... jsonObjects) {
        ArrayList<JsonObject> objects = new ArrayList<>();
        for (String jsonObject : jsonObjects) {
            objects.add(getJsonObject(jsonObject));
        }
        return (mergeObjectsWithRestrictions(new HashMap<>(), restrictions, objects.toArray())
                .toString());
    }

    /**
     * See {@link JsonUtil#mergeArrays(String, Map, Object...)}
     */
    public String mergeArrays(
            String arrayName, Map<String, String[]> keyCombinations, String... jsonArrays) {
        ArrayList<JsonArray> arrays = new ArrayList<>();
        for (String jsonArray : jsonArrays) {
            arrays.add(getJsonArray(jsonArray));
        }
        return (mergeArrays(arrayName, keyCombinations, arrays.toArray()).toString());
    }

    /**
     * Alternative - no object identity keys are set. See {@link JsonUtil#mergeArrays(String, Map,
     * Object...)}
     */
    public String mergeArrays(String... jsonArrays) {
        ArrayList<JsonArray> arrays = new ArrayList<>();
        for (String jsonArray : jsonArrays) {
            arrays.add(getJsonArray(jsonArray));
        }
        return (mergeArrays("", new HashMap<>(), arrays.toArray()).toString());
    }

    /**
     * Alternative - no object identity keys are set. Seee {@link JsonUtil#mergeArrays(String, Map,
     * Object...)}
     */
    public JsonArray mergeArrays(Object... jsonArrays) {
        return (mergeArrays("", new HashMap<>(), jsonArrays));
    }

    /**
     * Merge arrays following different strategies based on identity keys.
     * 
     * <p><strong>REPLACE Strategy (no identity keys):</strong></p>
     * <ul>
     *   <li>Arrays are completely replaced with the latest array</li>
     *   <li>Previous array contents are discarded</li>
     *   <li>Null elements are ignored</li>
     * </ul>
     * 
     * <p><strong>MERGE Strategy (with identity keys):</strong></p>
     * <ul>
     *   <li>Duplicate elements are added until their amount is equal in both arrays</li>
     *   <li>Objects are considered identical if their identifier-keys match</li>
     *   <li>Matching objects are merged recursively</li>
     * </ul>
     *
     * @param arrayName       Merged arrays name or empty string. Used to choose from key combinations.
     * @param keyCombinations Array objects identifier-key names. If null or empty, uses REPLACE strategy.
     * @param jsonArrays      Any amount of JSON-arrays to merge.
     * @return Merged array.
     */
    public JsonArray mergeArrays(
            @NonNull String arrayName,
            @NonNull Map<String, String[]> keyCombinations,
            Object... jsonArrays) {

        JsonArray resultArray = new JsonArray();
        
        // Check if identity keys are provided for this array
        boolean hasIdentityKeys = keyCombinations.get(arrayName) != null 
                                   && keyCombinations.get(arrayName).length > 0;
        
        for (Object jsonArray : jsonArrays) {
            JsonArray array = (JsonArray) jsonArray;
            
            if (!hasIdentityKeys) {
                // No identity keys: REPLACE strategy
                // Simply use the latest array, overwriting previous ones
                resultArray = new JsonArray();
                for (JsonElement item : array) {
                    if (!item.isJsonNull()) {
                        resultArray.add(item);
                    }
                }
            } else {
                // Identity keys provided: MERGE strategy (existing logic)
                int iterator = 0;
                for (JsonElement item : array) {
                    if (item.isJsonObject()) {
                        ArrayList<JsonElement> resultArrayObjectsFound =
                                (ArrayList<JsonElement>)
                                        getArrayObjectsByKeyValues(
                                                resultArray, item.getAsJsonObject(), keyCombinations.get(arrayName));

                        if (!resultArrayObjectsFound.isEmpty()) {
                            // Such field is already present, merge is required:
                            JsonObject resultArrayObjectFound = resultArrayObjectsFound.get(0).getAsJsonObject();
                            JsonObject mergedObject =
                                    mergeObjects(keyCombinations, resultArrayObjectFound, item.getAsJsonObject());
                            resultArray.remove(resultArrayObjectFound);
                            resultArray.add(mergedObject);
                            continue;
                        }
                    }

                    if (!item.isJsonNull()) {
                        if (!resultArray.contains(item)) {
                            // No such element - add:
                            log.debug("*{}*, [{}]", item, iterator);
                            resultArray.add(item);
                        } else if (count(resultArray, item) < count(array, item)) {
                            log.debug("add: {} {}", item, iterator);
                            // There are more duplicates of the element - add:
                            resultArray.add(item);
                        }
                    } else {
                        log.debug("Delete: {}, [{}]", item, iterator);
                        resultArray.remove(iterator);
                    }
                    iterator++;
                }
            }
        }

        return resultArray;
    }

    /**
     * Convert String to JSON-Array (GSON).
     *
     * @param jsonString JSON-array as string.
     * @return JSON-array as GSON-array.
     */
    public JsonArray getJsonArray(String jsonString) {
        JsonArray jsonArray = new JsonArray();

        try {
            jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();
        } catch (Exception ignore) {
            log.trace("ignore");
        }

        return jsonArray;
    }

    /**
     * Find array objects that have required identity keys and match the values.
     *
     * @param array  Array to search in.
     * @param object Example object for search. Contains required keys and values.
     * @param keys   Object identity keys.
     * @return Matching JSON-elements.
     */
    public List<JsonElement> getArrayObjectsByKeyValues(
            JsonArray array, JsonObject object, String[] keys) {
        ArrayList<JsonElement> elements = new ArrayList<>();
        for (JsonElement arrayElement : array) {
            if (arrayElement.isJsonObject()) {
                JsonObject jsonObject = arrayElement.getAsJsonObject();
                boolean hasAllKeysThatMatch = true;
                for (String key : keys) {
                    if (!jsonObject.has(key)) {
                        // One of the keys is not found:
                        hasAllKeysThatMatch = false;
                        break;
                    } else {
                        if (jsonObject.get(key).isJsonPrimitive()
                                && !jsonObject.get(key).equals(object.get(key))) {
                            // Primitive type key values don't match:
                            hasAllKeysThatMatch = false;
                            break;
                        }
                        if ((jsonObject.get(key).isJsonObject() || jsonObject.get(key).isJsonArray())
                                && !jsonObject.get(key).toString().equals(object.get(key).toString())) {
                            // Complex type key values don't match:
                            hasAllKeysThatMatch = false;
                            break;
                        }
                    }
                }

                if (hasAllKeysThatMatch) {
                    // Key values match:
                    elements.add(jsonObject);
                }
            }
        }
        return elements;
    }

    /**
     * Count given elements in array.
     *
     * @param element Element to find.
     * @return Amount of given elements in array.
     */
    public int count(JsonArray array, JsonElement element) {
        int count = 0;
        for (JsonElement currentElement : array) {
            if (currentElement.isJsonPrimitive() && currentElement.equals(element)) {
                count++;
            }

            if (currentElement.isJsonObject()
                    || currentElement.isJsonArray() && currentElement.toString().equals(element.toString())) {
                count++;
            }
        }
        return count;
    }
}
