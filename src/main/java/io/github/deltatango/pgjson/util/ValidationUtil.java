package io.github.deltatango.pgjson.util;

import io.github.deltatango.pgjson.model.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.Error;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ArrayNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for validating JSON data against JSON Schema 2020-12.
 *
 * <p>This class uses the networknt json-schema-validator 3.0.0 API with:</p>
 * <ul>
 *   <li>Schema/SchemaRegistry instead of the legacy JsonSchema/JsonSchemaFactory</li>
 *   <li>Format assertions enabled by default for strict validation</li>
 *   <li>JSON Pointer path type for error locations</li>
 * </ul>
 */
@Slf4j
public class ValidationUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final SchemaRegistry SCHEMA_REGISTRY;

    static {
        // Configure the schema registry for JSON Schema 2020-12
        SCHEMA_REGISTRY = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    }

    private final SchemaCache schemaCache;

    public ValidationUtil() {
        this.schemaCache = new SchemaCache();
    }

    /**
     * Validates JSON data against a JSON Schema 2020-12 schema with caching support.
     * Uses compiled schema cache for improved performance.
     *
     * @param tabledefId unique table definition ID (cache key)
     * @param schemaHash SHA-256 hash for consistency verification
     * @param schemaData the JSON schema as a string
     * @param data the JSON data to validate as a string
     * @return ValidationResult containing validation status and error messages
     */
    public ValidationResult validateData(Integer tabledefId, String schemaHash, String schemaData, String data) {
        try {
            log.debug("Validating data against JSON Schema 2020-12 (with caching)");
            log.trace("Data: {}", data);

            Schema schema = schemaCache.getCompiledSchema(tabledefId, schemaHash, schemaData);
            JsonNode dataNode = OBJECT_MAPPER.readTree(data);

            // Validate and get list of errors
            List<Error> errors = schema.validate(dataNode);

            if (errors.isEmpty()) {
                log.debug("Validation succeeded");
                return new ValidationResult(true, "Data is valid");
            } else {
                String errorMessage = buildErrorMessage(errors);
                log.debug("Validation failed: {}", errorMessage);
                return new ValidationResult(false, errorMessage);
            }

        } catch (Exception exc) {
            log.warn("Error in method validateData", exc);
            return new ValidationResult(false, "Validation error: " + exc.getMessage());
        }
    }

    /**
     * Validates JSON data against a JSON Schema 2020-12 schema.
     * Only supports schemas using https://pgjson.bitbucket.io/meta-schema/v1
     *
     * <p>This method validates the provided JSON data against the given JSON schema.
     * It supports JSON Schema 2020-12 with PgJson vocabulary extensions and provides detailed validation error messages.</p>
     *
     * <h4>Validation Process:</h4>
     * <ol>
     *   <li>Parse the JSON schema from the provided schema data</li>
     *   <li>Parse the JSON data to be validated</li>
     *   <li>Normalize schema URIs to ensure valid $id fields</li>
     *   <li>Create a Schema instance from the normalized schema</li>
     *   <li>Validate the data against the schema</li>
     *   <li>Return validation results with detailed error messages</li>
     * </ol>
     *
     * <h4>Error Handling:</h4>
     * <ul>
     *   <li><strong>Validation Errors:</strong> Returns detailed error messages for schema violations</li>
     *   <li><strong>Parse Errors:</strong> Handles JSON parsing errors gracefully</li>
     *   <li><strong>Schema Errors:</strong> Handles invalid schema definitions</li>
     * </ul>
     *
     * @param schemaData the JSON schema as a string
     * @param data the JSON data to validate as a string
     * @return ValidationResult containing validation status and error messages
     *
     * @see ValidationResult
     * @see Schema
     * @see Error
     */
    public ValidationResult validateData(String schemaData, String data) {
        try {
            log.debug("Validating data against JSON Schema 2020-12");
            log.trace("Data: {}", data);

            JsonNode schemaNode = OBJECT_MAPPER.readTree(schemaData);
            JsonNode normalizedSchema = normalizeSchemaUris(schemaNode);

            // Replace PgJson meta-schema with standard 2020-12 for validation
            JsonNode validationSchema = replaceMetaSchema(normalizedSchema);

            Schema schema = SCHEMA_REGISTRY.getSchema(validationSchema);

            JsonNode dataNode = OBJECT_MAPPER.readTree(data);
            List<Error> errors = schema.validate(dataNode);

            if (errors.isEmpty()) {
                log.debug("Validation succeeded");
                return new ValidationResult(true, "Data is valid");
            } else {
                String errorMessage = buildErrorMessage(errors);
                log.debug("Validation failed: {}", errorMessage);
                return new ValidationResult(false, errorMessage);
            }

        } catch (Exception exc) {
            log.warn("Error in method validateData", exc);
            return new ValidationResult(false, "Validation error: " + exc.getMessage());
        }
    }

    /**
     * Builds an error message string from validation errors.
     *
     * @param errors the list of validation errors
     * @return a formatted error message string
     */
    private String buildErrorMessage(List<Error> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Validation failed";
        }

        return errors.stream()
                .map(Error::getMessage)
                .collect(Collectors.joining(" | "));
    }

    /**
     * Ensures all $id fields are valid URIs (converts UUIDs to urn:uuid: format).
     *
     * <p>This method normalizes $id fields in JSON schemas to ensure they are valid URIs.
     * UUIDs are converted to urn:uuid: format, while existing URIs are preserved.</p>
     *
     * @param node the JSON node to normalize
     * @return a new JSON node with normalized $id fields
     */
    JsonNode normalizeSchemaUris(JsonNode node) {
        if (node.isObject()) {
            ObjectNode result = OBJECT_MAPPER.createObjectNode();
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
            ArrayNode result = OBJECT_MAPPER.createArrayNode();
            for (JsonNode element : node) {
                result.add(normalizeSchemaUris(element));
            }
            return result;
        }
        return node;
    }

    /**
     * Replaces PgJson meta-schema references with standard JSON Schema 2020-12 for validation.
     * This allows validation to work without requiring the PgJson meta-schema to be available online.
     *
     * @param node the JSON node to process
     * @return a new JSON node with meta-schema references replaced
     */
    JsonNode replaceMetaSchema(JsonNode node) {
        if (node.isObject()) {
            ObjectNode result = OBJECT_MAPPER.createObjectNode();
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
            ArrayNode result = OBJECT_MAPPER.createArrayNode();
            for (JsonNode element : node) {
                result.add(replaceMetaSchema(element));
            }
            return result;
        }
        return node;
    }
}
