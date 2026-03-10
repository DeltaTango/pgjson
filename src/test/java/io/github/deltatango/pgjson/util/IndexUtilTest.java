package io.github.deltatango.pgjson.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for IndexUtil.
 *
 * <p>This test class covers all index detection scenarios including
 * simple properties, nested objects, arrays, and edge cases.</p>
 */
class IndexUtilTest {

    private IndexUtil indexUtil;

    @BeforeEach
    void setUp() {
        indexUtil = new IndexUtil();
    }

    @Test
    void testGetIndexes_EmptySchema() {
        // Test empty schema
        String schemaJson = "{}";
        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertTrue(indexes.isEmpty());
    }

    @Test
    void testGetIndexes_NullSchema() {
        // Test null schema
        assertThrows(NullPointerException.class,
                () -> indexUtil.getIndexes(null));
    }

    @Test
    void testGetIndexes_InvalidJson() {
        // Test invalid JSON
        String invalidJson = "{ invalid json }";
        assertThrows(Exception.class,
                () -> indexUtil.getIndexes(invalidJson));
    }

    @Test
    void testGetIndexes_SimpleProperty() {
        // Test simple property with index hint
        String schemaJson = """
            {
                "properties": {
                    "name": {
                        "type": "string",
                        "x-pgjson-index": "exact"
                    }
                }
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertEquals(1, indexes.size());
        assertEquals("exact", indexes.get("name"));
    }

    @Test
    void testGetIndexes_MultipleProperties() {
        // Test multiple properties with different index types
        String schemaJson = """
            {
                "properties": {
                    "id": {
                        "type": "string",
                        "x-pgjson-index": "exact"
                    },
                    "title": {
                        "type": "string",
                        "x-pgjson-index": "fts"
                    },
                    "tags": {
                        "type": "array",
                        "x-pgjson-index": "object"
                    }
                }
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertEquals(3, indexes.size());
        assertEquals("exact", indexes.get("id"));
        assertEquals("fts", indexes.get("title"));
        assertEquals("object", indexes.get("tags"));
    }

    @Test
    void testGetIndexes_NestedObject() {
        // Test nested object with index hints
        String schemaJson = """
            {
                "properties": {
                    "user": {
                        "type": "object",
                        "properties": {
                            "name": {
                                "type": "string",
                                "x-pgjson-index": "exact"
                            },
                            "email": {
                                "type": "string",
                                "x-pgjson-index": "fts"
                            }
                        }
                    }
                }
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertEquals(2, indexes.size());
        assertEquals("exact", indexes.get("user.name"));
        assertEquals("fts", indexes.get("user.email"));
    }

    @Test
    void testGetIndexes_DeeplyNestedObject() {
        // Test deeply nested object
        String schemaJson = """
            {
                "properties": {
                    "company": {
                        "type": "object",
                        "properties": {
                            "address": {
                                "type": "object",
                                "properties": {
                                    "street": {
                                        "type": "string",
                                        "x-pgjson-index": "exact"
                                    },
                                    "city": {
                                        "type": "string",
                                        "x-pgjson-index": "fts"
                                    }
                                }
                            }
                        }
                    }
                }
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertEquals(2, indexes.size());
        assertEquals("exact", indexes.get("company.address.street"));
        assertEquals("fts", indexes.get("company.address.city"));
    }

    @Test
    void testGetIndexes_ArrayWithItems() {
        // Test array with items that have properties
        String schemaJson = """
            {
                "properties": {
                    "products": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "name": {
                                    "type": "string",
                                    "x-pgjson-index": "fts"
                                },
                                "price": {
                                    "type": "number",
                                    "x-pgjson-index": "exact"
                                }
                            }
                        }
                    }
                }
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertEquals(2, indexes.size());
        assertEquals("fts", indexes.get("products[].name"));
        assertEquals("exact", indexes.get("products[].price"));
    }

    @Test
    void testGetIndexes_ComplexNestedArray() {
        // Test complex nested array structure
        String schemaJson = """
            {
                "properties": {
                    "orders": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "items": {
                                    "type": "array",
                                    "items": {
                                        "type": "object",
                                        "properties": {
                                            "product": {
                                                "type": "string",
                                                "x-pgjson-index": "fts"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertEquals(1, indexes.size());
        assertEquals("fts", indexes.get("orders[].items[].product"));
    }

    @Test
    void testGetIndexes_NoIndexHints() {
        // Test schema without index hints
        String schemaJson = """
            {
                "properties": {
                    "name": {
                        "type": "string"
                    },
                    "age": {
                        "type": "number"
                    }
                }
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertTrue(indexes.isEmpty());
    }

    @Test
    void testGetIndexes_MixedIndexHints() {
        // Test schema with some properties having index hints and others not
        String schemaJson = """
            {
                "properties": {
                    "id": {
                        "type": "string",
                        "x-pgjson-index": "exact"
                    },
                    "name": {
                        "type": "string"
                    },
                    "description": {
                        "type": "string",
                        "x-pgjson-index": "fts"
                    }
                }
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertEquals(2, indexes.size());
        assertEquals("exact", indexes.get("id"));
        assertEquals("fts", indexes.get("description"));
    }

    @Test
    void testGetIndexes_AllIndexTypes() {
        // Test all supported index types
        String schemaJson = """
            {
                "properties": {
                    "exactField": {
                        "type": "string",
                        "x-pgjson-index": "exact"
                    },
                    "ftsField": {
                        "type": "string",
                        "x-pgjson-index": "fts"
                    },
                    "objectField": {
                        "type": "object",
                        "x-pgjson-index": "object"
                    },
                    "nestedObjectField": {
                        "type": "object",
                        "x-pgjson-index": "nestedobject"
                    }
                }
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertEquals(4, indexes.size());
        assertEquals("exact", indexes.get("exactField"));
        assertEquals("fts", indexes.get("ftsField"));
        assertEquals("object", indexes.get("objectField"));
        assertEquals("nestedobject", indexes.get("nestedObjectField"));
    }

    @Test
    void testGetIndexes_NonObjectProperties() {
        // Test schema with non-object properties (should be skipped)
        String schemaJson = """
            {
                "properties": {
                    "stringProp": "string",
                    "numberProp": 123,
                    "booleanProp": true,
                    "nullProp": null,
                    "arrayProp": ["item1", "item2"],
                    "objectProp": {
                        "type": "string",
                        "x-pgjson-index": "exact"
                    }
                }
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertEquals(1, indexes.size());
        assertEquals("exact", indexes.get("objectProp"));
    }

    @Test
    void testGetIndexes_EmptyProperties() {
        // Test schema with empty properties object
        String schemaJson = """
            {
                "properties": {}
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertTrue(indexes.isEmpty());
    }

    @Test
    void testGetIndexes_NoProperties() {
        // Test schema without properties
        String schemaJson = """
            {
                "type": "object",
                "title": "Test Schema"
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertTrue(indexes.isEmpty());
    }

    @Test
    void testGetIndexes_ArrayWithoutItems() {
        // Test array without items property
        String schemaJson = """
            {
                "properties": {
                    "tags": {
                        "type": "array"
                    }
                }
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertTrue(indexes.isEmpty());
    }

    @Test
    void testGetIndexes_ItemsNotObject() {
        // Test array with items that are not objects
        String schemaJson = """
            {
                "properties": {
                    "tags": {
                        "type": "array",
                        "items": {
                            "type": "string"
                        }
                    }
                }
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertTrue(indexes.isEmpty());
    }

    @Test
    void testGetIndexes_ComplexRealWorldSchema() {
        // Test complex real-world schema
        String schemaJson = """
            {
                "type": "object",
                "properties": {
                    "id": {
                        "type": "string",
                        "x-pgjson-index": "exact"
                    },
                    "title": {
                        "type": "string",
                        "x-pgjson-index": "fts"
                    },
                    "author": {
                        "type": "object",
                        "properties": {
                            "name": {
                                "type": "string",
                                "x-pgjson-index": "exact"
                            },
                            "email": {
                                "type": "string",
                                "x-pgjson-index": "fts"
                            }
                        }
                    },
                    "tags": {
                        "type": "array",
                        "items": {
                            "type": "string",
                            "x-pgjson-index": "object"
                        }
                    },
                    "comments": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "text": {
                                    "type": "string",
                                    "x-pgjson-index": "fts"
                                },
                                "author": {
                                    "type": "string",
                                    "x-pgjson-index": "exact"
                                }
                            }
                        }
                    }
                }
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertEquals(7, indexes.size());
        assertEquals("exact", indexes.get("id"));
        assertEquals("fts", indexes.get("title"));
        assertEquals("exact", indexes.get("author.name"));
        assertEquals("fts", indexes.get("author.email"));
        assertEquals("object", indexes.get("tags[]"));
        assertEquals("fts", indexes.get("comments[].text"));
        assertEquals("exact", indexes.get("comments[].author"));
    }

    @Test
    void testGetIndexes_IndexHintAsNonString() {
        // Test index hint that is not a string (should be ignored)
        String schemaJson = """
            {
                "properties": {
                    "validField": {
                        "type": "string",
                        "x-pgjson-index": "exact"
                    },
                    "invalidField": {
                        "type": "string",
                        "x-pgjson-index": 123
                    }
                }
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertEquals(1, indexes.size());
        assertEquals("exact", indexes.get("validField"));
    }

    @Test
    void testGetIndexes_EmptyIndexHint() {
        // Test empty index hint (should be ignored)
        String schemaJson = """
            {
                "properties": {
                    "validField": {
                        "type": "string",
                        "x-pgjson-index": "exact"
                    },
                    "emptyField": {
                        "type": "string",
                        "x-pgjson-index": ""
                    }
                }
            }
            """;

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertEquals(1, indexes.size());
        assertEquals("exact", indexes.get("validField"));
    }

    @Test
    void testGetIndexes_JsonParserIntegration() {
        // Test integration with JsonParser
        String schemaJson = """
            {
                "properties": {
                    "testField": {
                        "type": "string",
                        "x-pgjson-index": "exact"
                    }
                }
            }
            """;

        // Parse manually to verify JsonParser integration
        JsonObject schema = JsonParser.parseString(schemaJson).getAsJsonObject();
        assertTrue(schema.has("properties"));

        Map<String, String> indexes = indexUtil.getIndexes(schemaJson);

        assertNotNull(indexes);
        assertEquals(1, indexes.size());
        assertEquals("exact", indexes.get("testField"));
    }
}
