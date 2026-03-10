package io.github.deltatango.pgjson;

import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import io.github.deltatango.pgjson.model.IndexInfo;
import io.github.deltatango.pgjson.model.TableDef;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import io.github.deltatango.pgjson.model.repo.TableDefRepo;
import io.github.deltatango.pgjson.util.ValidationUtil;
import io.github.deltatango.pgjson.model.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.*;

/**
 * Internal service responsible for schema and table definition operations.
 *
 * <p>This class is package-private and not part of the public API.
 * All public access goes through {@link PostgreSqlJsonClient}.</p>
 */
@Slf4j
class SchemaService {

    private List<TableDef> effectiveTableDef;
    private final TableDefRepo tableDefRepo;
    private final ValidationUtil validationUtil;

    SchemaService(TableDefRepo tableDefRepo, ValidationUtil validationUtil) {
        this.tableDefRepo = tableDefRepo;
        this.validationUtil = validationUtil;
    }

    /**
     * Loads all effective table definitions from the database into memory.
     * Called once during client initialization.
     */
    List<TableDef> loadAllEffectiveTableDef(Connection connection) throws PostgreJsonException {
        try {
            log.debug("Loading effective table definitions into memory");
            List<TableDef> returnedTableDefs = new ArrayList<>();
            OperationResult<List<TableDef>> tableDefsResult = tableDefRepo.selectAllEffectiveTableDef(connection);
            List<TableDef> tableDefs = tableDefsResult.getOrThrow();
            for (TableDef item : tableDefs) {
                OperationResult<List<IndexInfo>> indexResult = tableDefRepo.selectAllIndexOfTable(connection, item.getTableName());
                item.setIndexInfo(new ArrayList<>(indexResult.isSuccess()
                        ? ((OperationResult.Success<List<IndexInfo>>) indexResult).value()
                        : List.of()));
                returnedTableDefs.add(item);
            }
            effectiveTableDef = Collections.unmodifiableList(returnedTableDefs);
            return effectiveTableDef;
        } catch (PostgreJsonException e) {
            throw e;
        } catch (Exception exc) {
            log.error("Unexpected error in loadAllEffectiveTableDef", exc);
            throw new PostgreJsonException("Failed to load table definitions", exc);
        }
    }

    /**
     * Resolves a TableDef from the in-memory cache or falls back to the database.
     * Returns null if not found anywhere.
     */
    TableDef resolveTableDef(Connection connection, Integer tableDefId, String idUuid, String tableName, String schemaName) {
        // Try memory first
        TableDef found = null;
        if (idUuid != null) {
            found = effectiveTableDef.stream()
                    .filter(td -> idUuid.equals(td.getIdUuid()))
                    .findFirst().orElse(null);
        } else if (tableDefId != null) {
            found = effectiveTableDef.stream()
                    .filter(td -> tableDefId.equals(td.getTableDefId()))
                    .findFirst().orElse(null);
        } else if (tableName != null && schemaName != null) {
            found = effectiveTableDef.stream()
                    .filter(td -> tableName.equals(td.getTableName()))
                    .filter(td -> schemaName.equals(td.getSchemaName()))
                    .findFirst().orElse(null);
        } else if (tableName != null) {
            found = effectiveTableDef.stream()
                    .filter(td -> tableName.equals(td.getTableName()))
                    .findFirst().orElse(null);
        }

        if (found != null) {
            return found;
        }

        // Fall back to database using the provided connection
        return getTableDefFromDatabase(connection, tableDefId, idUuid, tableName, schemaName);
    }

    /**
     * Looks up a TableDef from the database using the provided connection.
     */
    private TableDef getTableDefFromDatabase(Connection connection,
            Integer tableDefId, String tableDefIdUuid, String tableName, String schemaName) {
        try {
            OperationResult<TableDef> result = OperationResult.notFound("No lookup criteria provided");

            if (tableDefId != null) {
                result = tableDefRepo.selectTableDefById(connection, tableDefId);
            }
            if (tableDefIdUuid != null) {
                result = tableDefRepo.selectTableDefByUuid(connection, tableDefIdUuid);
            }
            if (tableName != null && schemaName != null) {
                result = tableDefRepo.selectTableDefByTableNameAndName(connection, tableName, schemaName);
            }
            if (tableName != null && schemaName == null) {
                result = tableDefRepo.selectTableDefByTableName(connection, tableName);
            }

            if (result instanceof OperationResult.Success<TableDef> s) {
                TableDef tableDef = s.value();
                OperationResult<List<IndexInfo>> indexResult = tableDefRepo.selectAllIndexOfTable(connection, tableDef.getTableName());
                tableDef.setIndexInfo(new ArrayList<>(indexResult.isSuccess()
                        ? ((OperationResult.Success<List<IndexInfo>>) indexResult).value()
                        : List.of()));
                return tableDef;
            }

            log.debug("Did not find table def for: {}, {}, {}", tableDefId, tableDefIdUuid, tableName);
            return null;
        } catch (Exception exc) {
            log.error("Unexpected error in method getTableDefFromDatabase", exc);
            return null;
        }
    }

    // ---- Convenience resolution methods ----

    OperationResult<TableDef> resolveTableDefByIdUuid(Connection connection, String idUuid) {
        if (idUuid == null || idUuid.trim().isEmpty()) {
            return OperationResult.error("ID UUID cannot be null or empty");
        }
        try {
            TableDef found = resolveTableDef(connection, null, idUuid, null, null);
            if (found == null) {
                return OperationResult.notFound("TableDef not found for idUuid: " + idUuid);
            }
            return OperationResult.success(found);
        } catch (Exception exc) {
            log.error("Unexpected error in resolveTableDefByIdUuid", exc);
            return OperationResult.error("Failed to retrieve table definition", exc);
        }
    }

    OperationResult<TableDef> resolveTableDefByTableNameAndName(Connection connection, String tableName, String schemaName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return OperationResult.error("Table name cannot be null or empty");
        }
        try {
            TableDef found = resolveTableDef(connection, null, null, tableName, schemaName);
            if (found == null) {
                return OperationResult.notFound("TableDef not found for table: " + tableName);
            }
            return OperationResult.success(found);
        } catch (Exception exc) {
            log.error("Unexpected error in resolveTableDefByTableNameAndName", exc);
            return OperationResult.error("Failed to retrieve table definition", exc);
        }
    }

    OperationResult<TableDef> resolveTableDefById(Connection connection, Integer tableDefId) {
        if (tableDefId == null || tableDefId <= 0) {
            return OperationResult.error("Table definition ID must be positive");
        }
        try {
            TableDef found = resolveTableDef(connection, tableDefId, null, null, null);
            if (found == null) {
                return OperationResult.notFound("TableDef not found for id: " + tableDefId);
            }
            return OperationResult.success(found);
        } catch (Exception exc) {
            log.error("Unexpected error in resolveTableDefById", exc);
            return OperationResult.error("Failed to retrieve table definition", exc);
        }
    }

    // ---- Schema insert ----

    OperationResult<String> insertSchema(Connection connection, String tableName, String schemaName, String schemaData) {
        try {
            UUID uuid = UUID.randomUUID();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String schemaHash = bytesToHex(digest.digest(schemaData.getBytes(StandardCharsets.UTF_8)));

            OperationResult<Integer> result =
                    tableDefRepo.insertSchema(connection, uuid.toString(), tableName, schemaName, schemaData, schemaHash);

            if (result instanceof OperationResult.Success<Integer> s && s.value() > 0) {
                return OperationResult.success(uuid.toString());
            } else if (result instanceof OperationResult.Error<Integer> e) {
                return OperationResult.error(e.message(), e.cause());
            } else {
                return OperationResult.error("Failed to insert schema - no rows affected");
            }
        } catch (Exception exc) {
            log.error("Unexpected error in insertSchema", exc);
            return OperationResult.error("Failed to insert schema", exc);
        }
    }

    // ---- Validation delegation ----

    ValidationResult validateData(int tableDefId, String schemaHash, String schemaData, String jsonData) {
        return validationUtil.validateData(tableDefId, schemaHash, schemaData, jsonData);
    }

    // ---- Utility ----

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
