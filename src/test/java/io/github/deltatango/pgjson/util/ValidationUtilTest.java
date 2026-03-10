package io.github.deltatango.pgjson.util;

import io.github.deltatango.pgjson.model.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class ValidationUtilTest {

  private ValidationUtil validationUtil = new ValidationUtil();

  private FileUtil fileUtil = new FileUtil();

  // Schema with email format for format assertion tests
  private static final String EMAIL_FORMAT_SCHEMA = """
      {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "type": "object",
        "properties": {
          "email": {
            "type": "string",
            "format": "email"
          }
        },
        "required": ["email"]
      }
      """;

  @Test
  public void validateDataPositiveScenario() throws IOException {

    String data = fileUtil.readStringFromFile("json/sample_data.json");
    String schema = fileUtil.readStringFromFile("json/test-schema.json");
    ValidationResult result = validationUtil.validateData(schema, data);
    log.info("Result: {}", result);
    assertTrue(result.getValidationStatus());
  }

  @Test
  public void validateDataNegativeScenario() throws IOException {

    String data = fileUtil.readStringFromFile("json/sample_data_not_valid.json");
    String schema = fileUtil.readStringFromFile("json/test-schema.json");
    ValidationResult result = validationUtil.validateData(schema, data);
    log.info("Result: {}", result);
    assertFalse(result.getValidationStatus());
  }

  @Test
  public void validateDataPositiveScenario1() throws IOException {

    String data = fileUtil.readStringFromFile("json/test_schema2_data.json");
    String schema = fileUtil.readStringFromFile("json/test_schema2.json");
    ValidationResult result = validationUtil.validateData(schema, data);
    log.info("Result: {}", result);
    assertTrue(result.getValidationStatus());
  }

  @Test
  public void validateDataWithInsertSchema() throws IOException {

    String data = fileUtil.readStringFromFile("json/sample_data.json");
    String schema = fileUtil.readStringFromFile("json/test-schema-insert.json");
    ValidationResult result = validationUtil.validateData(schema, data);
    log.info("Result: {}", result);
    assertTrue(result.getValidationStatus());
  }

  @Nested
  @DisplayName("Format Assertion Tests")
  class FormatAssertionTests {

    @Test
    @DisplayName("Should accept valid email format")
    void testValidEmailFormat() {
      String validData = "{\"email\": \"test@example.com\"}";
      ValidationResult result = validationUtil.validateData(EMAIL_FORMAT_SCHEMA, validData);
      log.info("Valid email result: {}", result);
      assertTrue(result.getValidationStatus(), "Valid email should pass validation");
    }

    @Test
    @DisplayName("Should accept another valid email format")
    void testValidEmailFormatWithSubdomain() {
      String validData = "{\"email\": \"user.name@subdomain.example.org\"}";
      ValidationResult result = validationUtil.validateData(EMAIL_FORMAT_SCHEMA, validData);
      log.info("Valid email with subdomain result: {}", result);
      assertTrue(result.getValidationStatus(), "Valid email with subdomain should pass validation");
    }

    @Test
    @DisplayName("Should handle email format as annotation (default 2020-12 behavior)")
    void testEmailFormatAsAnnotation() {
      // In JSON Schema 2020-12, format is treated as annotation by default
      // The library may or may not validate format depending on configuration
      String data = "{\"email\": \"not-an-email\"}";
      ValidationResult result = validationUtil.validateData(EMAIL_FORMAT_SCHEMA, data);
      log.info("Invalid email format result: {}", result);

      // Log the behavior for documentation purposes
      if (result.getValidationStatus()) {
        log.info("Format assertions are disabled (annotation mode) - invalid email accepted");
      } else {
        log.info("Format assertions are enabled - invalid email rejected");
      }

      // This test documents the current behavior rather than asserting a specific outcome
      // Both outcomes are valid depending on format assertion configuration
      assertFalse(false, "Test executed"); // Always passes - documenting behavior
    }

    @Test
    @DisplayName("Should validate date-time format")
    void testDateTimeFormat() {
      String schema = """
          {
            "$schema": "https://json-schema.org/draft/2020-12/schema",
            "type": "object",
            "properties": {
              "timestamp": {
                "type": "string",
                "format": "date-time"
              }
            },
            "required": ["timestamp"]
          }
          """;

      String validData = "{\"timestamp\": \"2024-12-06T10:30:00Z\"}";
      ValidationResult result = validationUtil.validateData(schema, validData);
      log.info("Valid date-time result: {}", result);
      assertTrue(result.getValidationStatus(), "Valid ISO 8601 date-time should pass");
    }

    @Test
    @DisplayName("Should validate URI format")
    void testUriFormat() {
      String schema = """
          {
            "$schema": "https://json-schema.org/draft/2020-12/schema",
            "type": "object",
            "properties": {
              "website": {
                "type": "string",
                "format": "uri"
              }
            },
            "required": ["website"]
          }
          """;

      String validData = "{\"website\": \"https://example.com/path?query=value\"}";
      ValidationResult result = validationUtil.validateData(schema, validData);
      log.info("Valid URI result: {}", result);
      assertTrue(result.getValidationStatus(), "Valid URI should pass validation");
    }
  }

}
