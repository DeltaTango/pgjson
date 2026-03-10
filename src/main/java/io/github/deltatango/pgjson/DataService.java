package io.github.deltatango.pgjson;

import io.github.deltatango.pgjson.model.DatabaseEntry;
import io.github.deltatango.pgjson.model.TableDef;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import io.github.deltatango.pgjson.model.operations.Result;
import io.github.deltatango.pgjson.model.repo.DatabaseEntryRepo;
import io.github.deltatango.pgjson.model.validation.ValidationResult;
import io.github.deltatango.pgjson.util.JsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.util.UUID;

/**
 * Internal service responsible for CRUD data operations.
 *
 * <p>This class is package-private and not part of the public API.
 * All public access goes through {@link PostgreSqlJsonClient}.</p>
 */
@Slf4j
class DataService {

    private final DatabaseEntryRepo databaseEntryRepo;
    private final SchemaService schemaService;
    private final JsonUtil jsonUtil;
    private final Gson gson;

    DataService(DatabaseEntryRepo databaseEntryRepo, SchemaService schemaService, JsonUtil jsonUtil, Gson gson) {
        this.databaseEntryRepo = databaseEntryRepo;
        this.schemaService = schemaService;
        this.jsonUtil = jsonUtil;
        this.gson = gson;
    }

    // ---- Insert ----

    OperationResult<Result> insertData(Connection connection, String tableName, String schemaName, String jsonData) {
        ValidationResult validationResult = new ValidationResult();
        boolean resultStatus = false;
        String resultMessage;
        UUID entryIdUuid = UUID.randomUUID();
        String entryIdUuidString = entryIdUuid.toString();

        OperationResult<TableDef> tableDefResult = schemaService.resolveTableDefByTableNameAndName(connection, tableName, schemaName);
        if (tableDefResult instanceof OperationResult.Success<TableDef> s) {
            TableDef tableDef = s.value();
            validationResult = schemaService.validateData(
                tableDef.getTableDefId(),
                tableDef.getSchemaHash(),
                tableDef.getSchemaData(),
                jsonData
            );
            if (Boolean.TRUE.equals(validationResult.getValidationStatus())) {
                OperationResult<DatabaseEntry> insertResult = databaseEntryRepo.insertData(
                        connection, entryIdUuidString, tableName, jsonData, tableDef.getTableDefId());
                if (insertResult instanceof OperationResult.Success<DatabaseEntry> is) {
                    DatabaseEntry insertedEntry = is.value();
                    resultStatus = true;
                    entryIdUuidString = insertedEntry.getEntryIdUuid();
                    jsonData = insertedEntry.getJsonData();
                    resultMessage = "Database entry inserted in database";
                } else {
                    resultStatus = false;
                    entryIdUuidString = null;
                    jsonData = null;
                    resultMessage = "Database entry did not inserted in database due to SQL error";
                }
            } else {
                resultStatus = false;
                entryIdUuidString = null;
                jsonData = null;
                resultMessage = "Data validation failed";
            }
        } else {
            resultMessage = "Table def not found for table";
            log.warn("Table def not found for table {}", tableName);
        }
        Result result = new Result(entryIdUuidString, jsonData, tableName, validationResult, resultStatus, resultMessage);
        return resultStatus ? OperationResult.success(result) : OperationResult.error(resultMessage);
    }

    // ---- Update ----

    OperationResult<Result> updateData(Connection connection, String tableName, String jsonData, String entryIdUuid) {
        OperationResult<DatabaseEntry> entryResult = selectDataByIdUuid(connection, entryIdUuid, tableName);
        if (!(entryResult instanceof OperationResult.Success<DatabaseEntry> s)) {
            log.warn("Entry not found with idUuid {} in database table {}", entryIdUuid, tableName);
            return OperationResult.notFound("Database entry not found for idUuid: " + entryIdUuid);
        }
        DatabaseEntry databaseEntry = s.value();
        databaseEntry.setJsonData(jsonData);
        log.trace("jsonDataToBeUpdate: {}", databaseEntry.getJsonData());
        return doValidationAndUpdate(connection, tableName, databaseEntry);
    }

    OperationResult<Result> updateDataWithMerge(Connection connection, String tableName, String data, String entryIdUuid) {
        OperationResult<DatabaseEntry> entryResult = selectDataByIdUuid(connection, entryIdUuid, tableName);
        if (!(entryResult instanceof OperationResult.Success<DatabaseEntry> s)) {
            log.warn("Entry not found with idUuid {} in database table {}", entryIdUuid, tableName);
            return OperationResult.notFound("Database entry not found for idUuid: " + entryIdUuid);
        }
        DatabaseEntry databaseEntry = s.value();
        log.trace("oldJsonData: {}", databaseEntry.getJsonData());
        String jsonData = jsonUtil.mergeObjects(databaseEntry.getJsonData(), data);
        databaseEntry.setJsonData(jsonData);
        log.trace("newJsonData: {}", jsonData);
        return doValidationAndUpdate(connection, tableName, databaseEntry);
    }

    private OperationResult<Result> doValidationAndUpdate(Connection connection, String tableName, DatabaseEntry databaseEntry) {
        ValidationResult validationResult = new ValidationResult();
        boolean resultStatus = false;
        String resultMessage = "";

        try {
            if (databaseEntry.getSchemaDbId() > 0) {
                OperationResult<TableDef> tableDefResult = schemaService.resolveTableDefById(connection, databaseEntry.getSchemaDbId());
                if (tableDefResult instanceof OperationResult.Success<TableDef> s) {
                    TableDef tableDef = s.value();
                    validationResult = schemaService.validateData(
                        tableDef.getTableDefId(),
                        tableDef.getSchemaHash(),
                        tableDef.getSchemaData(),
                        databaseEntry.getJsonData()
                    );
                    if (Boolean.TRUE.equals(validationResult.getValidationStatus())) {
                        OperationResult<DatabaseEntry> updateResult = databaseEntryRepo.updateData(connection,
                                tableName, databaseEntry.getJsonData(), databaseEntry.getEntryIdUuid());
                        if (updateResult.isSuccess()) {
                            resultStatus = true;
                            resultMessage = "Database entry updated in database";
                        } else {
                            resultStatus = false;
                            resultMessage = "Database entry did not updated in database";
                        }
                    } else {
                        resultStatus = false;
                    }
                }
            }
            Result result = new Result(databaseEntry.getEntryIdUuid(), databaseEntry.getJsonData(),
                    tableName, validationResult, resultStatus, resultMessage);
            return resultStatus ? OperationResult.success(result) : OperationResult.error(resultMessage);
        } catch (Exception exc) {
            log.error("Exception in doValidationAndUpdate", exc);
            return OperationResult.error("Validation and update failed", exc);
        }
    }

    // ---- Delete ----

    OperationResult<Boolean> deleteData(Connection connection, String tableName, String idUuid) {
        OperationResult<Integer> result = databaseEntryRepo.deleteRecord(connection, tableName, idUuid);
        if (result instanceof OperationResult.Success<Integer> s) {
            return OperationResult.success(s.value() > 0);
        } else if (result instanceof OperationResult.Error<Integer> e) {
            return OperationResult.error(e.message(), e.cause());
        }
        return OperationResult.error("Unexpected result from deleteRecord");
    }

    OperationResult<Result> deleteArrayElement(Connection connection, String tableName, String data, String entryIdUuid) {
        JsonObject dataObject = gson.fromJson(data, JsonObject.class);
        String key = null;
        Integer index = null;
        if (dataObject != null && !dataObject.isJsonNull()) {
            JsonElement keyObject = dataObject.get("key");
            if (keyObject != null && !keyObject.isJsonNull()) {
                key = keyObject.getAsString();
            }

            JsonElement indexObject = dataObject.get("index");
            if (keyObject != null && !indexObject.isJsonNull()) {
                index = indexObject.getAsInt();
            }
        }

        log.debug("Deleting array element {}, index {}", key, index);

        if (key != null && index != null) {
            OperationResult<DatabaseEntry> deleteResult = databaseEntryRepo.deleteArrayElement(
                    connection, entryIdUuid, tableName, key, index);
            if (deleteResult instanceof OperationResult.Success<DatabaseEntry> s) {
                DatabaseEntry changedEntry = s.value();
                return OperationResult.success(new Result(
                        changedEntry.getEntryIdUuid(),
                        changedEntry.getJsonData(),
                        tableName,
                        null,
                        true,
                        "Delete done"));
            } else {
                return OperationResult.error("Failed to delete array element");
            }
        } else {
            return OperationResult.error("Either key or index is missing");
        }
    }

    // ---- Select ----

    OperationResult<DatabaseEntry> selectDataByIdUuid(Connection connection, String idUuid, String tableName) {
        try {
            return databaseEntryRepo.selectDataByIdUuid(connection, tableName, idUuid);
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument in selectDataByIdUuid", e);
            throw e;
        } catch (Exception exc) {
            log.error("Unexpected error in selectDataByIdUuid", exc);
            return OperationResult.error("Failed to retrieve data", exc);
        }
    }
}
