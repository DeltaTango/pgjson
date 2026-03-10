package io.github.deltatango.pgjson.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Extracts index hints from JSON Schema 2020-12 schemas using PgJson vocabulary extensions.
 *
 * <p>Reads {@code x-pgjson-index} annotations from schema properties and produces a map
 * of property paths to index types. This map is used during schema insertion to automatically
 * create the corresponding PostgreSQL GIN indexes on the data table.</p>
 *
 * <p>Supports nested properties and array items with recursive traversal.</p>
 *
 * @see io.github.deltatango.pgjson.model.enums.IndexType
 */
@Slf4j
public class IndexUtil {

    /**
     * Extracts index hints from a JSON Schema 2020-12 schema using vocabulary extensions.
     *
     * <p>This method reads only x-pgjson-index vocabulary extensions from the schema,
     * ignoring any legacy 'index' properties. It recursively traverses the schema
     * structure to find all indexed properties.</p>
     *
     * @param schemaJson the JSON schema as a string
     * @return Map of property paths to index types
     */
    public Map<String, String> getIndexes(String schemaJson) {
        Map<String, String> indexes = new HashMap<>();
        JsonObject schema = JsonParser.parseString(schemaJson).getAsJsonObject();

        if (schema.has("properties")) {
            JsonObject properties = schema.getAsJsonObject("properties");
            detectIndexes(properties, indexes, "");
        }

        log.debug("Extracted indexes: {}", indexes);
        return indexes;
    }

    /**
     * Recursively detects index hints from schema properties using vocabulary extensions.
     *
     * @param properties the properties object to analyze
     * @param indexes the map to store found indexes
     * @param parentPath the parent path for nested properties
     */
    private void detectIndexes(JsonObject properties, Map<String, String> indexes, String parentPath) {
        for (String key : properties.keySet()) {
            try {
                JsonObject property = properties.getAsJsonObject(key);
                String propertyPath = parentPath.isEmpty() ? key : parentPath + "." + key;

                // Check for x-pgjson-index vocabulary extension
                String indexHint = getIndexHint(property);
                if (indexHint != null) {
                    indexes.put(propertyPath, indexHint);
                    log.debug("Found index hint: {} -> {}", propertyPath, indexHint);
                }

                // Recursively process nested properties
                if (property.has("properties")) {
                    JsonObject nestedProperties = property.getAsJsonObject("properties");
                    detectIndexes(nestedProperties, indexes, propertyPath);
                }

                // Process array items
                if (property.has("items") && property.get("items").isJsonObject()) {
                    JsonObject items = property.getAsJsonObject("items");

                    // Check if the array items themselves have index hints
                    String itemIndexHint = getIndexHint(items);
                    if (itemIndexHint != null) {
                        indexes.put(key + "[]", itemIndexHint);
                        log.debug("Found index hint: {} -> {}", key + "[]", itemIndexHint);
                    }

                    // Process array items if they have properties
                    if (items.has("properties")) {
                        JsonObject itemProperties = items.getAsJsonObject("properties");
                        String arrayPath = parentPath.isEmpty() ? key + "[]" : parentPath + "." + key + "[]";
                        detectIndexes(itemProperties, indexes, arrayPath);
                    }
                }

            } catch (ClassCastException exc) {
                log.debug("Property {} is not an object, skipping", key);
            }
        }
    }

    /**
     * Extracts index hint from a property object using vocabulary extensions.
     *
     * @param property the property object to analyze
     * @return the index hint if found, null otherwise
     */
    private String getIndexHint(JsonObject property) {
        if (property.has("x-pgjson-index") && property.get("x-pgjson-index").isJsonPrimitive()) {
            // Only accept string values, not numbers or other primitives
            if (property.get("x-pgjson-index").getAsJsonPrimitive().isString()) {
                String hint = property.get("x-pgjson-index").getAsString();
                // Only return non-empty hints
                return hint.isEmpty() ? null : hint;
            }
        }
        return null;
    }

}

