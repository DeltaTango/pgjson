package io.github.deltatango.pgjson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.deltatango.pgjson.exceptions.RequestException;
import io.github.deltatango.pgjson.model.DatabaseEntry;
import io.github.deltatango.pgjson.model.IndexInfo;
import io.github.deltatango.pgjson.model.TableDef;
import io.github.deltatango.pgjson.model.enums.LogicalOperator;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import io.github.deltatango.pgjson.model.operations.PagedResult;
import io.github.deltatango.pgjson.model.repo.DatabaseEntryRepo;
import io.github.deltatango.pgjson.constants.ApplicationConstants;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal service responsible for search/select operations.
 *
 * <p>This class is package-private and not part of the public API.
 * All public access goes through {@link PostgreSqlJsonClient}.</p>
 */
class SearchService {

    private final DatabaseEntryRepo databaseEntryRepo;
    private final SchemaService schemaService;
    private final Gson gson;

    SearchService(DatabaseEntryRepo databaseEntryRepo, SchemaService schemaService, Gson gson) {
        this.databaseEntryRepo = databaseEntryRepo;
        this.schemaService = schemaService;
        this.gson = gson;
    }

    OperationResult<List<DatabaseEntry>> selectData(Connection connection, String tableName, String schemaName, String searchJson)
            throws RequestException {
        JsonObject jsonObject = parseSearchJson(searchJson);
        SearchParameters searchParams = extractSearchParameters(jsonObject);
        JsonElement searchTerm = extractSearchTerm(jsonObject);

        // Resolve TableDef on the same connection used for the search query
        OperationResult<TableDef> tableDefResult = schemaService.resolveTableDefByTableNameAndName(connection, tableName, schemaName);
        List<IndexInfo> indexes = tableDefResult.isSuccess()
                ? ((OperationResult.Success<TableDef>) tableDefResult).value().getIndexInfo()
                : new ArrayList<>();

        return databaseEntryRepo.searchData(
                connection, tableName, searchParams.order, searchParams.limit, searchParams.offset,
                searchTerm, new ArrayList<>(indexes), searchParams.logicalOperator);
    }

    OperationResult<PagedResult<DatabaseEntry>> selectDataWithCount(Connection connection, String tableName, String schemaName, String searchJson)
            throws RequestException {
        JsonObject jsonObject = parseSearchJson(searchJson);
        SearchParameters searchParams = extractSearchParameters(jsonObject);
        JsonElement searchTerm = extractSearchTerm(jsonObject);

        OperationResult<TableDef> tableDefResult = schemaService.resolveTableDefByTableNameAndName(connection, tableName, schemaName);
        List<IndexInfo> indexes = tableDefResult.isSuccess()
                ? ((OperationResult.Success<TableDef>) tableDefResult).value().getIndexInfo()
                : new ArrayList<>();

        return databaseEntryRepo.searchDataWithCount(
                connection, tableName, searchParams.order, searchParams.limit, searchParams.offset,
                searchTerm, new ArrayList<>(indexes), searchParams.logicalOperator);
    }

    private JsonObject parseSearchJson(String searchJson) throws RequestException {
        if (searchJson == null || searchJson.trim().isEmpty()) {
            throw new RequestException(ApplicationConstants.ERROR_SEARCH_JSON_NULL);
        }

        JsonObject jsonObject = gson.fromJson(searchJson, JsonObject.class);
        if (jsonObject == null || jsonObject.isJsonNull()) {
            throw new RequestException(ApplicationConstants.ERROR_REQUEST_EMPTY);
        }
        return jsonObject;
    }

    private SearchParameters extractSearchParameters(JsonObject jsonObject) throws RequestException {
        String order = extractOrderType(jsonObject);
        LogicalOperator logicalOperator = extractLogicalOperator(jsonObject);
        Integer limit = extractLimit(jsonObject);
        Integer offset = extractOffset(jsonObject);

        return new SearchParameters(order, logicalOperator, limit, offset);
    }

    private String extractOrderType(JsonObject jsonObject) {
        JsonElement orderTypeElement = jsonObject.get(ApplicationConstants.JSON_FIELD_ORDER_TYPE);
            if (orderTypeElement != null && !orderTypeElement.isJsonNull()) {
                String orderType = orderTypeElement.getAsString();
            if (ApplicationConstants.ORDER_ASC.equalsIgnoreCase(orderType)
                || ApplicationConstants.ORDER_DESC.equalsIgnoreCase(orderType)) {
                return orderType;
                }
        }
        return ApplicationConstants.DEFAULT_ORDER;
            }

    private LogicalOperator extractLogicalOperator(JsonObject jsonObject) {
        JsonElement logicalOperatorElement = jsonObject.get(ApplicationConstants.JSON_FIELD_LOGICAL_OPERATOR);
            if (logicalOperatorElement != null && !logicalOperatorElement.isJsonNull()) {
                String logicalOperator = logicalOperatorElement.getAsString();
            if (ApplicationConstants.LOGICAL_OPERATOR_AND.equalsIgnoreCase(logicalOperator)) {
                return LogicalOperator.and;
            } else if (ApplicationConstants.LOGICAL_OPERATOR_OR.equalsIgnoreCase(logicalOperator)) {
                return LogicalOperator.or;
            }
        }
        return LogicalOperator.and;
    }

    private Integer extractLimit(JsonObject jsonObject) throws RequestException {
        JsonElement limitElement = jsonObject.get(ApplicationConstants.JSON_FIELD_LIMIT);
        if (limitElement == null || limitElement.isJsonNull()) {
            throw new RequestException(ApplicationConstants.ERROR_LIMIT_MISSING);
        }

        Integer limit = limitElement.getAsInt();
        if (limit < ApplicationConstants.MIN_LIMIT) {
            throw new RequestException(ApplicationConstants.ERROR_LIMIT_INVALID);
        }
        return limit;
    }

    private Integer extractOffset(JsonObject jsonObject) throws RequestException {
        JsonElement offsetElement = jsonObject.get(ApplicationConstants.JSON_FIELD_OFFSET);
        if (offsetElement == null || offsetElement.isJsonNull()) {
            throw new RequestException(ApplicationConstants.ERROR_OFFSET_MISSING);
        }

        Integer offset = offsetElement.getAsInt();
        if (offset < ApplicationConstants.MIN_OFFSET) {
            throw new RequestException(ApplicationConstants.ERROR_OFFSET_INVALID);
        }
        return offset;
    }

    private JsonElement extractSearchTerm(JsonObject jsonObject) throws RequestException {
        JsonElement searchTermElement = jsonObject.get(ApplicationConstants.JSON_FIELD_SEARCH_TERM);
        if (searchTermElement == null || searchTermElement.isJsonNull()) {
            return null;
        }

        if (!(searchTermElement instanceof JsonObject)) {
            throw new RequestException(ApplicationConstants.ERROR_SEARCH_TERM_INVALID);
        }

        JsonObject searchTerm = (JsonObject) searchTermElement;
        if (searchTerm.isJsonNull()) {
            throw new RequestException(ApplicationConstants.ERROR_FAULTY_REQUEST);
        }

        return searchTerm.keySet().isEmpty() ? null : searchTerm;
    }

    static class SearchParameters {
        final String order;
        final LogicalOperator logicalOperator;
        final Integer limit;
        final Integer offset;

        SearchParameters(String order, LogicalOperator logicalOperator, Integer limit, Integer offset) {
            this.order = order;
            this.logicalOperator = logicalOperator;
            this.limit = limit;
            this.offset = offset;
        }
    }
}
