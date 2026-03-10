package io.github.deltatango.pgjson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.deltatango.pgjson.model.TableDef;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import io.github.deltatango.pgjson.constants.ApplicationConstants;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.util.Map;
import java.util.Set;

/**
 * Internal service responsible for UI label extraction from JSON schemas.
 *
 * <p>This class is package-private and not part of the public API.
 * All public access goes through {@link PostgreSqlJsonClient}.</p>
 */
@Slf4j
class UiLabelService {

    private final SchemaService schemaService;
    private final Gson gson;

    UiLabelService(SchemaService schemaService, Gson gson) {
        this.schemaService = schemaService;
        this.gson = gson;
    }

    OperationResult<String> getUiLabels(Connection connection, String tableName, String schemaName) {
        OperationResult<TableDef> tableDefResult = schemaService.resolveTableDefByTableNameAndName(connection, tableName, schemaName);
        if (tableDefResult instanceof OperationResult.Success<TableDef> s) {
            TableDef tableDef = s.value();
            if (tableDef.getSchemaData() != null && !tableDef.getSchemaData().trim().isEmpty()) {
                log.debug("Found tableDef with idUuid {} ", tableDef.getIdUuid());
                JsonObject uiLabelCollectionObject = new JsonObject();
                JsonObject schemaJsonObject = gson.fromJson(tableDef.getSchemaData(), JsonObject.class);

                if (schemaJsonObject != null && !schemaJsonObject.isJsonNull()) {
                    iterateAndSaveUiLabels(uiLabelCollectionObject, schemaJsonObject.entrySet());
                    return OperationResult.success(uiLabelCollectionObject.toString());
                } else {
                    log.warn("Invalid schema data for table {}", tableName);
                    return OperationResult.error("Invalid schema data for table: " + tableName);
                }
            }
        }
        return OperationResult.notFound("Table def not found for table: " + tableName);
    }

    OperationResult<String> getUiLabelsFromSchemaFile(String schema) {
        try {
            JsonObject uiLabelCollectionObject = new JsonObject();
            JsonObject schemaJsonObject = gson.fromJson(schema, JsonObject.class);

            if (schemaJsonObject != null && !schemaJsonObject.isJsonNull()) {
                iterateAndSaveUiLabels(uiLabelCollectionObject, schemaJsonObject.entrySet());
                log.debug("LABELS: {} ", uiLabelCollectionObject.entrySet().size());
                return OperationResult.success(uiLabelCollectionObject.toString());
            } else {
                log.warn("Invalid schema JSON format");
                return OperationResult.error("Invalid schema JSON format");
            }
        } catch (Exception exc) {
            log.error("Exception in method getUiLabelsFromSchemaFile", exc);
            return OperationResult.error("Failed to extract UI labels from schema", exc);
        }
    }

    private void iterateAndSaveUiLabels(JsonObject uiLabelCollectionObject, Set<Map.Entry<String, JsonElement>> entrySet) {
        if (uiLabelCollectionObject == null || entrySet == null) {
            log.warn(ApplicationConstants.WARN_CANNOT_ITERATE_UI_LABELS);
            return;
        }

        for (Map.Entry<String, JsonElement> entry : entrySet) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }

            JsonElement entryElement = entry.getValue();
            if (entryElement.isJsonObject()) {
                String key = entry.getKey();
                JsonObject entryObject = entryElement.getAsJsonObject();
                if (entryObject != null && !entryObject.isJsonNull()) {
                    JsonElement uiLabelElement = entryObject.get("x-pgjson-uiLabel");
                    if (uiLabelElement != null && !uiLabelElement.isJsonNull() && uiLabelElement.isJsonObject()) {
                        JsonObject uiLabelObject = uiLabelElement.getAsJsonObject();
                        if (uiLabelObject != null && !uiLabelObject.isJsonNull()) {
                            JsonObject uiLabelDataObject = new JsonObject();
                            for (Map.Entry<String, JsonElement> uiLabelEntry : uiLabelObject.entrySet()) {
                                if (uiLabelEntry != null && uiLabelEntry.getKey() != null && uiLabelEntry.getValue() != null) {
                                    uiLabelDataObject.add(uiLabelEntry.getKey(), uiLabelEntry.getValue());
                                }
                            }
                            uiLabelCollectionObject.add(key, uiLabelDataObject);
                        }
                    }
                    iterateAndSaveUiLabels(uiLabelCollectionObject, entryObject.entrySet());
                }
            }
        }
    }
}
