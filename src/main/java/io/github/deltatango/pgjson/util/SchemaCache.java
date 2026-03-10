package io.github.deltatango.pgjson.util;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe cache for compiled JSON Schemas.
 *
 * <p>Uses the json-schema-validator 3.0.0 SchemaRegistry API to compile and cache
 * Schema instances. Schemas are keyed by tabledefId and validated against their hash
 * to detect schema changes.</p>
 */
@Slf4j
public class SchemaCache {

    private final ConcurrentMap<Integer, CompiledSchema> cache;
    private final SchemaRegistry schemaRegistry;
    private final ObjectMapper objectMapper;

    private static class CompiledSchema {
        private final Schema schema;
        private final String schemaHash;

        CompiledSchema(Schema schema, String schemaHash) {
            this.schema = schema;
            this.schemaHash = schemaHash;
        }

        Schema getSchema() {
            return schema;
        }
        String getSchemaHash() {
            return schemaHash;
        }
    }

    public SchemaCache() {
        this.cache = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper();

        // Configure the schema registry for JSON Schema 2020-12
        this.schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    }

    /**
     * Gets or compiles a schema from the cache.
     *
     * @param tabledefId the table definition ID (cache key)
     * @param schemaHash the expected schema hash for validation
     * @param schemaData the schema JSON string
     * @return the compiled Schema
     */
    public Schema getCompiledSchema(Integer tabledefId, String schemaHash, String schemaData) {
        CompiledSchema cached = cache.get(tabledefId);

        if (cached != null) {
            if (!cached.getSchemaHash().equals(schemaHash)) {
                log.warn("Schema hash mismatch for tabledefId {}, invalidating cache", tabledefId);
                cache.remove(tabledefId);
            } else {
                log.trace("Cache hit for tabledefId: {}", tabledefId);
                return cached.getSchema();
            }
        }

        return cache.computeIfAbsent(tabledefId, key -> {
            try {
                log.debug("Compiling and caching schema for tabledefId: {}", tabledefId);

                JsonNode schemaNode = objectMapper.readTree(schemaData);
                JsonNode normalizedSchema = normalizeSchemaUris(schemaNode);
                JsonNode validationSchema = replaceMetaSchema(normalizedSchema);
                Schema compiledSchema = schemaRegistry.getSchema(validationSchema);

                return new CompiledSchema(compiledSchema, schemaHash);
            } catch (Exception e) {
                log.error("Failed to compile schema for tabledefId: {}", tabledefId, e);
                throw new RuntimeException("Failed to compile schema", e);
            }
        }).getSchema();
    }

    /**
     * Invalidates a specific schema in the cache.
     *
     * @param tabledefId the table definition ID to invalidate
     */
    public void invalidate(Integer tabledefId) {
        cache.remove(tabledefId);
        log.debug("Invalidated cached schema for tabledefId: {}", tabledefId);
    }

    /**
     * Clears all schemas from the cache.
     */
    public void clear() {
        cache.clear();
        log.debug("Cleared all cached schemas");
    }

    /**
     * Returns the number of schemas currently cached.
     *
     * @return the cache size
     */
    public int size() {
        return cache.size();
    }

    /**
     * Normalizes $id fields in JSON schemas to ensure they are valid URIs.
     * UUIDs are converted to urn:uuid: format.
     */
    private JsonNode normalizeSchemaUris(JsonNode node) {
        if (node.isObject()) {
            ObjectNode result = objectMapper.createObjectNode();
            ObjectNode originalNode = (ObjectNode) node;

            originalNode.properties().forEach(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                if ("$id".equals(key) && value.isTextual()) {
                    String idValue = value.asText();
                    if (!idValue.startsWith("http") && !idValue.startsWith("urn:")) {
                        result.put("$id", "urn:uuid:" + idValue);
                    } else {
                        result.set("$id", value);
                    }
                } else if (value.isObject() || value.isArray()) {
                    result.set(key, normalizeSchemaUris(value));
                } else {
                    result.set(key, value);
                }
            });
            return result;
        } else if (node.isArray()) {
            ArrayNode result = objectMapper.createArrayNode();
            for (JsonNode element : node) {
                result.add(normalizeSchemaUris(element));
            }
            return result;
        }
        return node;
    }

    /**
     * Replaces PgJson meta-schema references with standard JSON Schema 2020-12.
     */
    private JsonNode replaceMetaSchema(JsonNode node) {
        if (node.isObject()) {
            ObjectNode result = objectMapper.createObjectNode();
            ObjectNode originalNode = (ObjectNode) node;

            originalNode.properties().forEach(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                if ("$schema".equals(key) && value.isTextual()) {
                    String schemaValue = value.asText();
                    if ("https://pgjson.bitbucket.io/meta-schema/v1".equals(schemaValue)) {
                        result.put("$schema", "https://json-schema.org/draft/2020-12/schema");
                        log.debug("Replaced PgJson meta-schema with standard 2020-12 schema");
                    } else {
                        result.set("$schema", value);
                    }
                } else if (value.isObject() || value.isArray()) {
                    result.set(key, replaceMetaSchema(value));
                } else {
                    result.set(key, value);
                }
            });
            return result;
        } else if (node.isArray()) {
            ArrayNode result = objectMapper.createArrayNode();
            for (JsonNode element : node) {
                result.add(replaceMetaSchema(element));
            }
            return result;
        }
        return node;
    }
}
