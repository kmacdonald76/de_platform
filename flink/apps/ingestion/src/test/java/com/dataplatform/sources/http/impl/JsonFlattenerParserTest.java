package com.dataplatform.sources.http.impl;

import com.dataplatform.model.FlatteningInstructions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.types.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonFlattenerParserTest {

    private JsonFlattenerParser parser;
    private Map<String, String> schema;
    private FlatteningInstructions instructions;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        schema = new LinkedHashMap<>();
        schema.put("id", "integer");
        schema.put("name", "string");
        schema.put("value", "double");

        instructions = mock(FlatteningInstructions.class);
        when(instructions.getTarget()).thenReturn("$"); // Default to root for simplicity
        when(instructions.getSelectFields()).thenReturn(Collections.emptyList()); // No specific select fields
        when(instructions.getIncludeParentFields()).thenReturn("none"); // No parent fields by default

        objectMapper = new ObjectMapper();
    }

    @Test
    void parse_singleJsonObject_returnsCorrectRow() throws Exception {
        // Arrange
        String json = "{\"id\": 1, \"name\": \"test\", \"value\": 10.5}";
        byte[] record = json.getBytes();

        parser = new JsonFlattenerParser(schema, instructions);

        // Act
        List<Row> result = parser.parse(record);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        Row row = result.get(0);
        assertEquals(3, row.getArity());
        assertEquals(1, row.getField(0)); // id
        assertEquals("test", row.getField(1)); // name
        assertEquals(10.5, row.getField(2)); // value
    }

    @Test
    void parse_jsonWithMissingFields_returnsNullForMissingFields() throws Exception {
        // Arrange
        String json = "{\"id\": 2, \"name\": \"another\"}";
        byte[] record = json.getBytes();

        parser = new JsonFlattenerParser(schema, instructions);

        // Act
        List<Row> result = parser.parse(record);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        Row row = result.get(0);
        assertEquals(2, row.getField(0)); // id
        assertEquals("another", row.getField(1)); // name
        assertNull(row.getField(2)); // value should be null
    }

    @Test
    void parse_jsonWithExtraFields_ignoresExtraFields() throws Exception {
        // Arrange
        String json = "{\"id\": 3, \"name\": \"extra\", \"value\": 20.0, \"extra_field\": \"ignored\"}";
        byte[] record = json.getBytes();

        parser = new JsonFlattenerParser(schema, instructions);

        // Act
        List<Row> result = parser.parse(record);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        Row row = result.get(0);
        assertEquals(3, row.getArity());
        assertEquals(3, row.getField(0)); // id
        assertEquals("extra", row.getField(1)); // name
        assertEquals(20.0, row.getField(2)); // value
    }

    @Test
    void parse_jsonWithNestedObjectAndSelectFields_extractsCorrectly() throws Exception {
        // Arrange
        schema = new HashMap<>();
        schema.put("nested_id", "integer");
        schema.put("nested_name", "string");

        when(instructions.getTarget()).thenReturn("$.data");
        FlatteningInstructions.SelectField selectField1 = new FlatteningInstructions.SelectField("nested_id");
        selectField1.setPath("$.id");
        FlatteningInstructions.SelectField selectField2 = new FlatteningInstructions.SelectField("nested_name");
        selectField2.setPath("$.name");
        when(instructions.getSelectFields()).thenReturn(List.of(selectField1, selectField2));

        String json = "{\"outer_field\": \"outer\", \"data\": {\"id\": 100, \"name\": \"nested_test\", \"extra\": \"field\"}}";
        byte[] record = json.getBytes();

        parser = new JsonFlattenerParser(schema, instructions);

        // Act
        List<Row> result = parser.parse(record);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        Row row = result.get(0);
        assertEquals(100, row.getField(0)); // nested_id
        assertEquals("nested_test", row.getField(1)); // nested_name
    }

    @Test
    void parse_jsonWithArrayTarget_returnsMultipleRows() throws Exception {
        // Arrange
        schema = new HashMap<>();
        schema.put("item_id", "integer");
        schema.put("item_name", "string");

        when(instructions.getTarget()).thenReturn("$.items[*]");
        FlatteningInstructions.SelectField selectField1 = new FlatteningInstructions.SelectField("item_id");
        selectField1.setPath("$.id");
        FlatteningInstructions.SelectField selectField2 = new FlatteningInstructions.SelectField("item_name");
        selectField2.setPath("$.name");
        when(instructions.getSelectFields()).thenReturn(List.of(selectField1, selectField2));

        String json = "{\"list_name\": \"my_list\", \"items\": [{\"id\": 1, \"name\": \"apple\"}, {\"id\": 2, \"name\": \"banana\"}]}";
        byte[] record = json.getBytes();

        parser = new JsonFlattenerParser(schema, instructions);

        // Act
        List<Row> result = parser.parse(record);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        Row row1 = result.get(0);
        assertEquals(1, row1.getField(0));
        assertEquals("apple", row1.getField(1));

        Row row2 = result.get(1);
        assertEquals(2, row2.getField(0));
        assertEquals("banana", row2.getField(1));
    }

    @Test
    void parse_emptyTargetAndIncludeParentFieldsAll_returnsRowWithParentFields() throws Exception {
        // Arrange
        schema = new LinkedHashMap<>();
        schema.put("outer_field", "string");
        schema.put("another_field", "string");

        when(instructions.getTarget()).thenReturn("$.non_existent_array[*]"); // Target is empty
        when(instructions.getIncludeParentFields()).thenReturn("all");

        String json = "{\"outer_field\": \"parent_val1\", \"another_field\": \"parent_val2\"}";
        byte[] record = json.getBytes();

        parser = new JsonFlattenerParser(schema, instructions);

        // Act
        List<Row> result = parser.parse(record);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        Row row = result.get(0);
        assertEquals("parent_val1", row.getField(0)); // outer_field
        assertEquals("parent_val2", row.getField(1)); // another_field
    }

    @Test
    void parse_emptyTargetAndIncludeParentFieldsSpecific_returnsRowWithSpecificParentFields() throws Exception {
        // Arrange
        schema = new LinkedHashMap<>();
        schema.put("specific_parent_field", "string");
        schema.put("another_field", "string");

        when(instructions.getTarget()).thenReturn("$.non_existent_array[*]"); // Target is empty
        when(instructions.getIncludeParentFields()).thenReturn(List.of("$.specific_parent_field"));

        String json = "{\"specific_parent_field\": \"parent_val_specific\", \"another_field\": \"parent_val_ignored\"}";
        byte[] record = json.getBytes();

        parser = new JsonFlattenerParser(schema, instructions);

        // Act
        List<Row> result = parser.parse(record);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        Row row = result.get(0);
        assertEquals("parent_val_specific", row.getField(0)); // specific_parent_field
        assertNull(row.getField(1)); // another_field should be null as it's not in includeParentFields
    }

    @Test
    void parse_arrayHandlingIgnore_returnsNullForArrayField() throws Exception {
        // Arrange
        schema = new HashMap<>();
        schema.put("data_field", "string");
        schema.put("array_field", "string");

        when(instructions.getTarget()).thenReturn("$");
        when(instructions.getArrayHandling()).thenReturn("ignore");

        String json = "{\"data_field\": \"some_data\", \"array_field\": [1, 2, 3]}";
        byte[] record = json.getBytes();

        parser = new JsonFlattenerParser(schema, instructions);

        // Act
        List<Row> result = parser.parse(record);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        Row row = result.get(0);
        assertEquals("some_data", row.getField(1));
        assertNull(row.getField(0)); // array_field should be null
    }

    @Test
    void parse_objectHandlingIgnore_returnsNullForObjectField() throws Exception {
        // Arrange
        schema = new HashMap<>();
        schema.put("data_field", "string");
        schema.put("object_field", "string");

        when(instructions.getTarget()).thenReturn("$");
        when(instructions.getObjectHandling()).thenReturn("ignore");

        String json = "{\"data_field\": \"some_data\", \"object_field\": {\"key\": \"value\"}}";
        byte[] record = json.getBytes();

        parser = new JsonFlattenerParser(schema, instructions);

        // Act
        List<Row> result = parser.parse(record);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        Row row = result.get(0);
        
        Object dataField = row.getField(1);
        Object objectField = row.getField(0);
        
        assertEquals("some_data", dataField);
        assertNull(objectField); // object_field should be null
    }

    @Test
    void parse_arrayHandlingJson_returnsJsonStringForArrayField() throws Exception {
        // Arrange
        schema = new HashMap<>();
        schema.put("data_field", "string");
        schema.put("array_field", "string");

        when(instructions.getTarget()).thenReturn("$");
        when(instructions.getArrayHandling()).thenReturn("json");

        String json = "{\"data_field\": \"some_data\", \"array_field\": [1, 2, 3]}";
        byte[] record = json.getBytes();

        parser = new JsonFlattenerParser(schema, instructions);

        // Act
        List<Row> result = parser.parse(record);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        Row row = result.get(0);
        assertEquals("some_data", row.getField(1));
        assertEquals("[1,2,3]", row.getField(0)); // array_field should be JSON string
    }

    @Test
    void parse_objectHandlingJson_returnsJsonStringForObjectField() throws Exception {
        // Arrange
        schema = new HashMap<>();
        schema.put("data_field", "string");
        schema.put("object_field", "string");

        when(instructions.getTarget()).thenReturn("$");
        when(instructions.getObjectHandling()).thenReturn("json");

        String json = "{\"data_field\": \"some_data\", \"object_field\": {\"key\": \"value\"}}";
        byte[] record = json.getBytes();

        parser = new JsonFlattenerParser(schema, instructions);

        // Act
        List<Row> result = parser.parse(record);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        Row row = result.get(0);
        assertEquals("some_data", row.getField(1));
        assertEquals("{\"key\":\"value\"}", row.getField(0)); // object_field should be JSON string
    }
}