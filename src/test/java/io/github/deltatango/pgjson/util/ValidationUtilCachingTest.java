package io.github.deltatango.pgjson.util;

import io.github.deltatango.pgjson.model.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@DisplayName("ValidationUtil Caching Integration Tests")
public class ValidationUtilCachingTest {
    
    private ValidationUtil validationUtil;
    private FileUtil fileUtil;
    
    @BeforeEach
    void setUp() {
        validationUtil = new ValidationUtil();
        fileUtil = new FileUtil();
    }
    
    @Test
    @DisplayName("Should validate with cached schema")
    void testValidationWithCaching() throws Exception {
        String data = fileUtil.readStringFromFile("json/sample_data.json");
        String schema = fileUtil.readStringFromFile("json/test-schema.json");
        Integer tabledefId = 1;
        String schemaHash = "test-hash-123";
        
        ValidationResult result = validationUtil.validateData(tabledefId, schemaHash, schema, data);
        
        assertTrue(result.getValidationStatus());
    }
    
    @Test
    @DisplayName("Should detect validation errors with cached schema")
    void testValidationErrorsWithCaching() throws Exception {
        String data = fileUtil.readStringFromFile("json/sample_data_not_valid.json");
        String schema = fileUtil.readStringFromFile("json/test-schema.json");
        Integer tabledefId = 1;
        String schemaHash = "test-hash-123";
        
        ValidationResult result = validationUtil.validateData(tabledefId, schemaHash, schema, data);
        
        assertFalse(result.getValidationStatus());
    }
    
    @Test
    @DisplayName("Should handle multiple validations with same schema")
    void testMultipleValidationsWithCaching() throws Exception {
        String data1 = fileUtil.readStringFromFile("json/sample_data.json");
        String data2 = fileUtil.readStringFromFile("json/test_schema2_data.json");
        String schema = fileUtil.readStringFromFile("json/test-schema.json");
        Integer tabledefId = 1;
        String schemaHash = "test-hash-123";
        
        ValidationResult result1 = validationUtil.validateData(tabledefId, schemaHash, schema, data1);
        ValidationResult result2 = validationUtil.validateData(tabledefId, schemaHash, schema, data2);
        
        assertNotNull(result1);
        assertNotNull(result2);
    }
    
    @Test
    @DisplayName("Should maintain backward compatibility")
    void testBackwardCompatibility() throws Exception {
        String data = fileUtil.readStringFromFile("json/sample_data.json");
        String schema = fileUtil.readStringFromFile("json/test-schema.json");
        
        ValidationResult result = validationUtil.validateData(schema, data);
        
        assertTrue(result.getValidationStatus());
    }
}
