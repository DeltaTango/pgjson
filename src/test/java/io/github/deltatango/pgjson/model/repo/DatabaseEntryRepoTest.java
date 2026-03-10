package io.github.deltatango.pgjson.model.repo;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import io.github.deltatango.pgjson.model.operations.OperationResult;

import java.util.ArrayList;

import io.github.deltatango.pgjson.model.enums.LogicalOperator;
import org.junit.jupiter.api.Test;

public class DatabaseEntryRepoTest {

  DatabaseEntryRepo repo = new DatabaseEntryRepo();

  @Test
  public void createTermTest_WithFallback() throws PostgreJsonException {
    JsonObject object = new JsonObject();
    JsonElement element = new JsonPrimitive("John");
    object.add("attribute1", element);

    // Should NOT throw exception anymore - uses fallback
    OperationResult<SearchTerm> opResult = repo.createTerm(object, new ArrayList<>(), 10, LogicalOperator.and, "test_table");

    assertInstanceOf(OperationResult.Success.class, opResult);
    SearchTerm result = opResult.getOrThrow();
    assertNotNull(result);
    // Search term now uses parameterized placeholder
    assertTrue(result.getSearchTerm().contains("json_data->>'attribute1'=?"));
    // Parameters should contain the actual value
    assertEquals(1, result.getParameters().size());
    assertEquals("John", result.getParameters().get(0));
  }

  @Test
  public void createTermTest_FtsInference() throws PostgreJsonException {
    JsonObject object = new JsonObject();
    object.addProperty("description", "search for multiple words");

    OperationResult<SearchTerm> opResult = repo.createTerm(object, new ArrayList<>(), 10, LogicalOperator.and, "test_table");

    assertInstanceOf(OperationResult.Success.class, opResult);
    SearchTerm result = opResult.getOrThrow();
    assertNotNull(result);
    assertTrue(result.getSearchTerm().contains("to_tsvector"));
    // FTS term should use parameterized placeholder
    assertTrue(result.getSearchTerm().contains("to_tsquery('simple', ?)"));
    assertEquals(1, result.getParameters().size());
    assertEquals("search for multiple words", result.getParameters().get(0));
  }

  @Test
  public void createTermTest_ObjectInference() throws PostgreJsonException {
    JsonObject object = new JsonObject();
    JsonObject nested = new JsonObject();
    nested.addProperty("city", "New York");
    object.add("address", nested);

    OperationResult<SearchTerm> opResult = repo.createTerm(object, new ArrayList<>(), 10, LogicalOperator.and, "test_table");

    assertInstanceOf(OperationResult.Success.class, opResult);
    SearchTerm result = opResult.getOrThrow();
    assertNotNull(result);
    assertTrue(result.getSearchTerm().contains("json_data->'address' @> ?::jsonb"));
    assertEquals(1, result.getParameters().size());
  }

  @Test
  public void createTermTest_ArrayInference() throws PostgreJsonException {
    JsonObject object = new JsonObject();
    JsonArray array = new JsonArray();
    array.add("item1");
    array.add("item2");
    object.add("tags", array);

    OperationResult<SearchTerm> opResult = repo.createTerm(object, new ArrayList<>(), 10, LogicalOperator.and, "test_table");

    assertInstanceOf(OperationResult.Success.class, opResult);
    SearchTerm result = opResult.getOrThrow();
    assertNotNull(result);
    assertTrue(result.getSearchTerm().contains("json_data->'tags' @>"));
    // Array query should have parameterized placeholders
    assertTrue(result.getSearchTerm().contains("?::jsonb"));
    assertEquals(2, result.getParameters().size());
  }
}
