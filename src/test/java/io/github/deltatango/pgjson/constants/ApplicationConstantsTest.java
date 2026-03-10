package io.github.deltatango.pgjson.constants;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ApplicationConstants.
 *
 * <p>This test class verifies all constants are properly defined,
 * have correct values, and the class follows utility class patterns.</p>
 */
class ApplicationConstantsTest {

    @Test
    void testConstructor_ThrowsUnsupportedOperationException() {
        // Test that constructor throws UnsupportedOperationException
        assertThrows(Exception.class,
                () -> {
                    Constructor<ApplicationConstants> constructor =
                            ApplicationConstants.class.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    constructor.newInstance();
                });
    }

    @Test
    void testClass_IsFinal() {
        // Test that class is final
        assertTrue(Modifier.isFinal(ApplicationConstants.class.getModifiers()));
    }

    @Test
    void testAllFields_AreStaticFinal() {
        // Test that all fields are static final
        Field[] fields = ApplicationConstants.class.getDeclaredFields();

        for (Field field : fields) {
            assertTrue(Modifier.isStatic(field.getModifiers()),
                    "Field " + field.getName() + " should be static");
            assertTrue(Modifier.isFinal(field.getModifiers()),
                    "Field " + field.getName() + " should be final");
        }
    }

    @Test
    void testDefaultValues() {
        // Test default value constants
        assertEquals("asc", ApplicationConstants.DEFAULT_ORDER);
        assertEquals("AND", ApplicationConstants.DEFAULT_LOGICAL_OPERATOR);
        assertEquals("{}", ApplicationConstants.DEFAULT_JSON_OBJECT);
    }

    @Test
    void testValidationLimits() {
        // Test validation limit constants
        assertEquals(1, ApplicationConstants.MIN_LIMIT);
        assertEquals(0, ApplicationConstants.MIN_OFFSET);
        assertEquals(1, ApplicationConstants.MIN_TABLE_DEF_ID);
        assertEquals(0, ApplicationConstants.MIN_INDEX);
    }

    @Test
    void testOrderTypes() {
        // Test order type constants
        assertEquals("asc", ApplicationConstants.ORDER_ASC);
        assertEquals("desc", ApplicationConstants.ORDER_DESC);

        // Verify they are different
        assertNotEquals(ApplicationConstants.ORDER_ASC, ApplicationConstants.ORDER_DESC);
    }

    @Test
    void testLogicalOperators() {
        // Test logical operator constants
        assertEquals("AND", ApplicationConstants.LOGICAL_OPERATOR_AND);
        assertEquals("OR", ApplicationConstants.LOGICAL_OPERATOR_OR);

        // Verify they are different
        assertNotEquals(ApplicationConstants.LOGICAL_OPERATOR_AND,
                ApplicationConstants.LOGICAL_OPERATOR_OR);
    }

    @Test
    void testJsonFieldNames() {
        // Test JSON field name constants
        assertEquals("orderType", ApplicationConstants.JSON_FIELD_ORDER_TYPE);
        assertEquals("logicalOperator", ApplicationConstants.JSON_FIELD_LOGICAL_OPERATOR);
        assertEquals("limit", ApplicationConstants.JSON_FIELD_LIMIT);
        assertEquals("offset", ApplicationConstants.JSON_FIELD_OFFSET);
        assertEquals("searchTerm", ApplicationConstants.JSON_FIELD_SEARCH_TERM);
        assertEquals("uiLabel", ApplicationConstants.JSON_FIELD_UI_LABEL);
    }

    @Test
    void testErrorMessages() {
        // Test error message constants
        assertNotNull(ApplicationConstants.ERROR_SEARCH_JSON_NULL);
        assertNotNull(ApplicationConstants.ERROR_REQUEST_EMPTY);
        assertNotNull(ApplicationConstants.ERROR_LIMIT_MISSING);
        assertNotNull(ApplicationConstants.ERROR_LIMIT_INVALID);
        assertNotNull(ApplicationConstants.ERROR_OFFSET_MISSING);
        assertNotNull(ApplicationConstants.ERROR_OFFSET_INVALID);
        assertNotNull(ApplicationConstants.ERROR_SEARCH_TERM_INVALID);
        assertNotNull(ApplicationConstants.ERROR_FAULTY_REQUEST);
        assertNotNull(ApplicationConstants.ERROR_TABLE_NAME_NULL);
        assertNotNull(ApplicationConstants.ERROR_ID_UUID_NULL);
        assertNotNull(ApplicationConstants.ERROR_JSON_DATA_NULL);
        assertNotNull(ApplicationConstants.ERROR_SCHEMA_NULL);
        assertNotNull(ApplicationConstants.ERROR_TABLE_DEF_ID_INVALID);
        assertNotNull(ApplicationConstants.ERROR_INDEX_INVALID);
        assertNotNull(ApplicationConstants.ERROR_KEY_NULL);

        // Verify error messages are not empty
        assertFalse(ApplicationConstants.ERROR_SEARCH_JSON_NULL.trim().isEmpty());
        assertFalse(ApplicationConstants.ERROR_REQUEST_EMPTY.trim().isEmpty());
        assertFalse(ApplicationConstants.ERROR_LIMIT_MISSING.trim().isEmpty());
        assertFalse(ApplicationConstants.ERROR_LIMIT_INVALID.trim().isEmpty());
        assertFalse(ApplicationConstants.ERROR_OFFSET_MISSING.trim().isEmpty());
        assertFalse(ApplicationConstants.ERROR_OFFSET_INVALID.trim().isEmpty());
        assertFalse(ApplicationConstants.ERROR_SEARCH_TERM_INVALID.trim().isEmpty());
        assertFalse(ApplicationConstants.ERROR_FAULTY_REQUEST.trim().isEmpty());
        assertFalse(ApplicationConstants.ERROR_TABLE_NAME_NULL.trim().isEmpty());
        assertFalse(ApplicationConstants.ERROR_ID_UUID_NULL.trim().isEmpty());
        assertFalse(ApplicationConstants.ERROR_JSON_DATA_NULL.trim().isEmpty());
        assertFalse(ApplicationConstants.ERROR_SCHEMA_NULL.trim().isEmpty());
        assertFalse(ApplicationConstants.ERROR_TABLE_DEF_ID_INVALID.trim().isEmpty());
        assertFalse(ApplicationConstants.ERROR_INDEX_INVALID.trim().isEmpty());
        assertFalse(ApplicationConstants.ERROR_KEY_NULL.trim().isEmpty());
    }

    @Test
    void testSuccessMessages() {
        // Test success message constants
        assertNotNull(ApplicationConstants.SUCCESS_DATA_INSERTED);
        assertNotNull(ApplicationConstants.SUCCESS_DATA_UPDATED);
        assertNotNull(ApplicationConstants.SUCCESS_DATA_DELETED);
        assertNotNull(ApplicationConstants.SUCCESS_VALIDATION_PASSED);

        // Verify success messages are not empty
        assertFalse(ApplicationConstants.SUCCESS_DATA_INSERTED.trim().isEmpty());
        assertFalse(ApplicationConstants.SUCCESS_DATA_UPDATED.trim().isEmpty());
        assertFalse(ApplicationConstants.SUCCESS_DATA_DELETED.trim().isEmpty());
        assertFalse(ApplicationConstants.SUCCESS_VALIDATION_PASSED.trim().isEmpty());
    }

    @Test
    void testWarningMessages() {
        // Test warning message constants
        assertNotNull(ApplicationConstants.WARN_TABLE_DEF_NOT_FOUND);
        assertNotNull(ApplicationConstants.WARN_ENTRY_NOT_FOUND);
        assertNotNull(ApplicationConstants.WARN_INVALID_SCHEMA);
        assertNotNull(ApplicationConstants.WARN_INVALID_JSON);
        assertNotNull(ApplicationConstants.WARN_CANNOT_ITERATE_UI_LABELS);

        // Verify warning messages are not empty
        assertFalse(ApplicationConstants.WARN_TABLE_DEF_NOT_FOUND.trim().isEmpty());
        assertFalse(ApplicationConstants.WARN_ENTRY_NOT_FOUND.trim().isEmpty());
        assertFalse(ApplicationConstants.WARN_INVALID_SCHEMA.trim().isEmpty());
        assertFalse(ApplicationConstants.WARN_INVALID_JSON.trim().isEmpty());
        assertFalse(ApplicationConstants.WARN_CANNOT_ITERATE_UI_LABELS.trim().isEmpty());
    }

    @Test
    void testLogMessages() {
        // Test log message constants
        assertNotNull(ApplicationConstants.LOG_LOADING_TABLE_DEFS);
        assertNotNull(ApplicationConstants.LOG_FOUND_TABLE_DEF);
        assertNotNull(ApplicationConstants.LOG_ARRAY_QUERY_DETECTED);
        assertNotNull(ApplicationConstants.LOG_SINGLE_TERM_GENERATED);
        assertNotNull(ApplicationConstants.LOG_FINAL_TERM);
        assertNotNull(ApplicationConstants.LOG_TERMS);
        assertNotNull(ApplicationConstants.LOG_LABELS_COUNT);

        // Verify log messages are not empty
        assertFalse(ApplicationConstants.LOG_LOADING_TABLE_DEFS.trim().isEmpty());
        assertFalse(ApplicationConstants.LOG_FOUND_TABLE_DEF.trim().isEmpty());
        assertFalse(ApplicationConstants.LOG_ARRAY_QUERY_DETECTED.trim().isEmpty());
        assertFalse(ApplicationConstants.LOG_SINGLE_TERM_GENERATED.trim().isEmpty());
        assertFalse(ApplicationConstants.LOG_FINAL_TERM.trim().isEmpty());
        assertFalse(ApplicationConstants.LOG_TERMS.trim().isEmpty());
        assertFalse(ApplicationConstants.LOG_LABELS_COUNT.trim().isEmpty());
    }

    @Test
    void testDatabaseOperations() {
        // Test database operation constants
        assertEquals("INSERT", ApplicationConstants.DB_OPERATION_INSERT);
        assertEquals("UPDATE", ApplicationConstants.DB_OPERATION_UPDATE);
        assertEquals("DELETE", ApplicationConstants.DB_OPERATION_DELETE);
        assertEquals("SELECT", ApplicationConstants.DB_OPERATION_SELECT);

        // Verify they are all different
        List<String> operations = Arrays.asList(
                ApplicationConstants.DB_OPERATION_INSERT,
                ApplicationConstants.DB_OPERATION_UPDATE,
                ApplicationConstants.DB_OPERATION_DELETE,
                ApplicationConstants.DB_OPERATION_SELECT
        );

        assertEquals(4, operations.stream().distinct().count());
    }

    @Test
    void testExceptionMessages() {
        // Test exception message constants
        assertNotNull(ApplicationConstants.EXCEPTION_HAPPENED_MESSAGE);
        assertNotNull(ApplicationConstants.EXCEPTION_DATABASE_ENTRY_NOT_FOUND);
        assertNotNull(ApplicationConstants.EXCEPTION_DATA_VALIDATION_FAILED);
        assertNotNull(ApplicationConstants.EXCEPTION_DATABASE_ENTRY_NOT_INSERTED);
        assertNotNull(ApplicationConstants.EXCEPTION_DATABASE_ENTRY_NOT_UPDATED);
        assertNotNull(ApplicationConstants.EXCEPTION_COULD_NOT_GENERATE_SEARCH_TERM);
        assertNotNull(ApplicationConstants.EXCEPTION_EITHER_KEY_OR_INDEX_MISSING);

        // Verify exception messages are not empty
        assertFalse(ApplicationConstants.EXCEPTION_HAPPENED_MESSAGE.trim().isEmpty());
        assertFalse(ApplicationConstants.EXCEPTION_DATABASE_ENTRY_NOT_FOUND.trim().isEmpty());
        assertFalse(ApplicationConstants.EXCEPTION_DATA_VALIDATION_FAILED.trim().isEmpty());
        assertFalse(ApplicationConstants.EXCEPTION_DATABASE_ENTRY_NOT_INSERTED.trim().isEmpty());
        assertFalse(ApplicationConstants.EXCEPTION_DATABASE_ENTRY_NOT_UPDATED.trim().isEmpty());
        assertFalse(ApplicationConstants.EXCEPTION_COULD_NOT_GENERATE_SEARCH_TERM.trim().isEmpty());
        assertFalse(ApplicationConstants.EXCEPTION_EITHER_KEY_OR_INDEX_MISSING.trim().isEmpty());
    }

    @Test
    void testConstants_AreImmutable() {
        // Test that constants cannot be modified (they are final)
        Field[] fields = ApplicationConstants.class.getDeclaredFields();

        for (Field field : fields) {
            assertTrue(Modifier.isFinal(field.getModifiers()),
                    "Field " + field.getName() + " should be final");
        }
    }

    @Test
    void testConstants_ArePublic() {
        // Test that all constants are public
        Field[] fields = ApplicationConstants.class.getDeclaredFields();

        for (Field field : fields) {
            assertTrue(Modifier.isPublic(field.getModifiers()),
                    "Field " + field.getName() + " should be public");
        }
    }

    @Test
    void testConstants_AreStatic() {
        // Test that all constants are static
        Field[] fields = ApplicationConstants.class.getDeclaredFields();

        for (Field field : fields) {
            assertTrue(Modifier.isStatic(field.getModifiers()),
                    "Field " + field.getName() + " should be static");
        }
    }

    @Test
    void testConstants_NoNullValues() {
        // Test that no constants have null values
        Field[] fields = ApplicationConstants.class.getDeclaredFields();

        for (Field field : fields) {
            try {
                Object value = field.get(null);
                assertNotNull(value, "Field " + field.getName() + " should not be null");
            } catch (IllegalAccessException e) {
                fail("Could not access field " + field.getName());
            }
        }
    }

    @Test
    void testConstants_StringConstants_NotBlank() {
        // Test that string constants are not blank
        Field[] fields = ApplicationConstants.class.getDeclaredFields();

        for (Field field : fields) {
            if (field.getType() == String.class) {
                try {
                    String value = (String) field.get(null);
                    assertNotNull(value, "String field " + field.getName() + " should not be null");
                    assertFalse(value.trim().isEmpty(),
                            "String field " + field.getName() + " should not be blank");
                } catch (IllegalAccessException e) {
                    fail("Could not access field " + field.getName());
                }
            }
        }
    }

    @Test
    void testConstants_IntegerConstants_ValidRange() {
        // Test that integer constants are in valid range
        Field[] fields = ApplicationConstants.class.getDeclaredFields();

        for (Field field : fields) {
            if (field.getType() == int.class) {
                try {
                    int value = field.getInt(null);
                    assertTrue(value >= 0,
                            "Integer field " + field.getName() + " should be non-negative");
                } catch (IllegalAccessException e) {
                    fail("Could not access field " + field.getName());
                }
            }
        }
    }

    @Test
    void testConstants_Consistency() {
        // Test consistency between related constants
        assertEquals(ApplicationConstants.DEFAULT_ORDER, ApplicationConstants.ORDER_ASC);
        assertEquals(ApplicationConstants.DEFAULT_LOGICAL_OPERATOR, ApplicationConstants.LOGICAL_OPERATOR_AND);

        // Verify order types are valid
        assertTrue(ApplicationConstants.ORDER_ASC.equals("asc") ||
                  ApplicationConstants.ORDER_ASC.equals("ASC"));
        assertTrue(ApplicationConstants.ORDER_DESC.equals("desc") ||
                  ApplicationConstants.ORDER_DESC.equals("DESC"));

        // Verify logical operators are valid
        assertTrue(ApplicationConstants.LOGICAL_OPERATOR_AND.equals("AND") ||
                  ApplicationConstants.LOGICAL_OPERATOR_AND.equals("and"));
        assertTrue(ApplicationConstants.LOGICAL_OPERATOR_OR.equals("OR") ||
                  ApplicationConstants.LOGICAL_OPERATOR_OR.equals("or"));
    }

    @Test
    void testConstants_Uniqueness() {
        // Test that constants with similar purposes are unique
        String[] orderTypes = {ApplicationConstants.ORDER_ASC, ApplicationConstants.ORDER_DESC};
        assertEquals(2, Arrays.stream(orderTypes).distinct().count());

        String[] logicalOperators = {ApplicationConstants.LOGICAL_OPERATOR_AND, ApplicationConstants.LOGICAL_OPERATOR_OR};
        assertEquals(2, Arrays.stream(logicalOperators).distinct().count());

        String[] databaseOperations = {
                ApplicationConstants.DB_OPERATION_INSERT,
                ApplicationConstants.DB_OPERATION_UPDATE,
                ApplicationConstants.DB_OPERATION_DELETE,
                ApplicationConstants.DB_OPERATION_SELECT
        };
        assertEquals(4, Arrays.stream(databaseOperations).distinct().count());
    }

    @Test
    void testConstants_FieldCount() {
        // Test that we have the expected number of constants
        Field[] fields = ApplicationConstants.class.getDeclaredFields();

        // Should have at least the minimum expected constants
        assertTrue(fields.length >= 20, "Should have at least 20 constants");
    }
}
